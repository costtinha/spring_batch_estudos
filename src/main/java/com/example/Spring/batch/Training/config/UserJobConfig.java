package com.example.Spring.batch.Training.config;

import com.example.Spring.batch.Training.dtos.UserCSV;
import com.example.Spring.batch.Training.entity.User;
// import com.example.Spring.batch.Training.persistance.UserRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class UserJobConfig {
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
        SynchronizedItemStreamReader<UserCSV> finalReader = new SynchronizedItemStreamReader<>();
        finalReader.setDelegate(reader);
        return finalReader;
    }

    @Bean
    public ItemProcessor<UserCSV, User> processor(){
        return  userCVS -> {
            String fullname = userCVS.getFirstName() + " " + userCVS.getLastName();
            return new User(fullname, userCVS.getAge(), userCVS.getEmail());

        };
    }

    @Bean
    public ItemWriter<User> writer(DataSource dataSource){
        JdbcBatchItemWriter<User> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql("INSERT INTO user_table (userId, name, age, email) VALUES ( :userId, :name, :age, :email)");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
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
    public JdbcCursorItemReader<User> exportReader(DataSource dataSource){
       JdbcCursorItemReader<User> reader = new JdbcCursorItemReader<>();
       reader.setDataSource(dataSource);
       reader.setSql("SELECT userId, name, age, email FROM user_table");
       reader.setRowMapper(new BeanPropertyRowMapper<>(User.class));
       reader.setFetchSize(1000);
       return reader;

    }


    @Bean
    public FlatFileItemWriter<User> exportWriter() throws IOException {
       String directoryPath = "output";
       String filePath = directoryPath + "/users_export.csv";
        File directory = new File(directoryPath);
        if (!directory.exists()){
            directory.mkdirs();
        }
       FlatFileItemWriter<User> exportWriter = new FlatFileItemWriter<>();
        exportWriter.setResource(new FileSystemResource(filePath));
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
    public Step exportStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           FlatFileItemWriter<User> exportWriter,
                           JdbcCursorItemReader<User> exportReader){
       return  new StepBuilder("exportStep",jobRepository)
               .<User,User>chunk(1000,transactionManager)
               .reader(exportReader)
               .writer(exportWriter)
               .build();
    }

    @Bean
    public Job exportUserCSVJob(JobRepository jobRepository, Step exportStep){
       return new JobBuilder("exportUserCSVJob",jobRepository)
               .start(exportStep)
               .build();
    }
}
