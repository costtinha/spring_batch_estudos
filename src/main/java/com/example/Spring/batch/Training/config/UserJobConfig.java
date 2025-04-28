package com.example.Spring.batch.Training.config;

import com.example.Spring.batch.Training.dtos.UserCSV;
import com.example.Spring.batch.Training.entity.User;
// import com.example.Spring.batch.Training.persistance.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.*;
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;
// import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
// import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class UserJobConfig {
    private static final Logger log = LoggerFactory.getLogger(UserJobConfig.class);
    //private final UserRepository repository;

  //  public UserJobConfig(UserRepository repository) {
       // this.repository = repository;
   // }

   @Bean
   public TaskExecutor taskExecutor(){
       ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
       executor.setCorePoolSize(4);
       executor.setMaxPoolSize(4);
       executor.setQueueCapacity(50);
       executor.setThreadNamePrefix("Batch-");
       executor.setWaitForTasksToCompleteOnShutdown(true);
       executor.initialize();
       return executor;
   }





    @Bean
    public SynchronizedItemStreamReader<UserCSV> reader(){
        FlatFileItemReader<UserCSV> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("user_10k.csv"));
        reader.setLinesToSkip(1);
        reader.setLineMapper( new DefaultLineMapper<>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {
                {
                    setNames("firstName", "lastName", "age", "email");
                }});
            setFieldSetMapper( new BeanWrapperFieldSetMapper<>() {{
                setTargetType(UserCSV.class);
            }});
            }});
        log.info("Arquivo de leitura inicializado e pronto para o user_10k.csv");
        SynchronizedItemStreamReader<UserCSV> finalReader = new SynchronizedItemStreamReader<>();
        finalReader.setDelegate(reader);
        return finalReader;
    }

    @Bean
    public ItemProcessor<UserCSV, User> processor(){
        return  userCVS -> {
            String fullname = userCVS.getFirstName() + " " + userCVS.getLastName();
            log.info("Processing UserCSV: firstName={}, lastName={}, age={}, email={}",
                    userCVS.getFirstName(), userCVS.getLastName(), userCVS.getAge(), userCVS.getEmail());
            return new User(fullname, userCVS.getAge(), userCVS.getEmail());

        };
    }

    @Bean
    public ItemWriter<User> writer(DataSource dataSource){
        JdbcBatchItemWriter<User> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO user_table (name, age, email) VALUES (:name, :age, :email)");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        log.info("Initialized writer for user_table");
        return writer;

    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      SynchronizedItemStreamReader<UserCSV> reader,
                      ItemProcessor<UserCSV,User> processor,
                      ItemWriter<User> writer,
                      TaskExecutor taskExecutor){
        return new StepBuilder("step1",jobRepository)
                .<UserCSV,User>chunk(1000,transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(taskExecutor)
                .build();
    }


    @Bean
    public Step createIndexStep(JobRepository jobRepository,
                                PlatformTransactionManager platformTransactionManager,
                                DataSource dataSource){
        return new  StepBuilder("createIndexStep",jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try (Connection connection = dataSource.getConnection();
                         Statement statement = connection.createStatement()) {
                            statement.execute("CREATE INDEX idx_user_email ON user_table(email)");

                    }
                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
                .build();
    }

    @Bean
    public Step dropIndexStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              DataSource dataSource){
        return new StepBuilder("dropIndexStep",jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try(Connection conn = dataSource.getConnection();
                    Statement statement = conn.createStatement()){
                        statement.execute("DROP INDEX IF EXISTS idx_user_email");

                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }



    @Bean
    public Job importUserCSVJob(JobRepository jobRepository, Step step1,
                                Step dropIndexStep, Step createIndexStep){
        return new JobBuilder("importUserJob",jobRepository)
                .start(dropIndexStep)
                .next(step1)
                .next(createIndexStep)
                .build();
    }

    // Daqui para frente beans sobre processo de exportação

    @Bean
    public JdbcPagingItemReader<User> exportReader(DataSource dataSource){
       JdbcPagingItemReader<User> reader = new JdbcPagingItemReader<>();
       reader.setDataSource(dataSource);
       reader.setRowMapper(new BeanPropertyRowMapper<>(User.class));
       reader.setFetchSize(1000);
       reader.setPageSize(1000);

        PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
        queryProvider.setSelectClause("user_id, name, age, email");
        queryProvider.setFromClause("user_table");
        queryProvider.setWhereClause("user_id >= :minId AND userId <= :maxId");

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("user_id",Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);

        reader.setQueryProvider(queryProvider);

        return reader;

    }


    @Bean
    public FlatFileItemWriter<User> exportWriter() throws IOException {
        FlatFileItemWriter<User> exportWriter = new FlatFileItemWriter<>();
        exportWriter.setResource(new FileSystemResource("output/users_export_${partitionName}.csv"));
        exportWriter.setHeaderCallback(writer -> writer.write("userId,name,age,email"));
        exportWriter.setLineAggregator(new DelimitedLineAggregator<>(){{
            setDelimiter(",");
            setFieldExtractor(new BeanWrapperFieldExtractor<>(){{
                setNames(new String[]{"userId","name","age","email"});
            }});
        }});
        return exportWriter;

    }

    @Bean
    public Job exportUserCSVJob(JobRepository jobRepository, Step masterStep, Step combineFilesStep){
       return new JobBuilder("exportUserCSVJob",jobRepository)
               .start(masterStep)
               .next(combineFilesStep)
               .build();
    }

    @Bean
    public Partitioner userPartitioner(DataSource dataSource){
       return gridSize -> {
           Map<String, ExecutionContext> partitions = new HashMap<>();
           long minId = getMinUserId(dataSource);
           long maxId = getMaxUserId(dataSource);
           long targetSize = (maxId - minId) / gridSize + 1;

           for (int i = 0; i < gridSize; i++){
               ExecutionContext context = new ExecutionContext();
               long partitionMinId = minId + i * targetSize;
               long partitionMaxId = Math.min(partitionMinId + targetSize - 1,maxId);
               context.putLong("minId", partitionMinId);
               context.putLong("maxId", partitionMaxId);
               context.putString("partitionName","partition" +i);
               partitions.put("partition" + i, context);
               log.info("Created partition {}: minId ={}, maxId={}",i,partitionMinId,partitionMaxId);
           }
           return partitions;
       };
    }

    @Bean
    public Step slaveStep(JobRepository jobRepository,
                          PlatformTransactionManager transactionManager,
                          FlatFileItemWriter<User> exportWriter,
                          JdbcPagingItemReader<User> exportReader,
                          TaskExecutor taskExecutor){
       return new StepBuilder("slaveStep",jobRepository)
               .<User,User>chunk(1000,transactionManager)
               .reader(exportReader)
               .writer(exportWriter)
               .taskExecutor(taskExecutor)
               .listener(new ParameterSettingListener(exportReader))
               .build();
    }

    @Bean
    public Step masterStep(JobRepository jobRepository,
                           Step slaveStep,
                           PlatformTransactionManager transactionManager,
                           Partitioner userPartitioner,
                           TaskExecutor taskExecutor){
       return new StepBuilder("masterStep", jobRepository)
               .partitioner("slaveStep", userPartitioner)
               .step(slaveStep)
               .taskExecutor(taskExecutor)
               .gridSize(4)
               .build();
    }


    @Bean
    public Step combineFilesStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager){
       return new StepBuilder("combineFilesStep",jobRepository)
               .tasklet((contribution, chunkContext) ->{
                   File outputDir = new File("output");
                   File finalFile = new File("output/users_export.csv");
                   try (BufferedWriter writer = Files.newBufferedWriter(finalFile.toPath())) {
                       writer.write("userId,name,age,email\n");
                       for (File tempFile : outputDir.listFiles((dir, name) -> name.startsWith("users_export_partition"))){
                           Files.lines(tempFile.toPath())
                                   .skip(1)
                                   .forEach(line -> {
                                       try {
                                           writer.write(line + "\n");
                                       } catch (IOException e){
                                           throw new RuntimeException("Failed to write to the final CSV ",e);
                                       }
                                   });
                           tempFile.delete();
                       }
                   }
                   return RepeatStatus.FINISHED;
               }, transactionManager)
               .build();
    }







    private long getMaxUserId(DataSource dataSource){
       try(Connection connection = dataSource.getConnection();
       Statement statement = connection.createStatement();
       ResultSet resultSet = statement.executeQuery("SELECT MAX(user_id) FROM user_table")) {
           if (resultSet.next()){
               return resultSet.getLong(1);
           }
       } catch (SQLException e){
           throw new RuntimeException("Failed to get max userId ",e );
       }
       return 0;
    }


    private long getMinUserId(DataSource dataSource){
       try(Connection connection = dataSource.getConnection();
       Statement statement = connection.createStatement();
           ResultSet resultSet = statement.executeQuery("SELECT MIN(user_id) FROM user_table")) {
           if (resultSet.next()) {
               return resultSet.getLong(1);
           }
       } catch (SQLException e){
           throw new RuntimeException("Failed to get min userId",e);
       }
       return 0;
    }
}
