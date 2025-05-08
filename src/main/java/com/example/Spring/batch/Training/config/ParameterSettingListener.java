package com.example.Spring.batch.Training.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;

import java.util.HashMap;
import java.util.Map;

public class ParameterSettingListener extends StepExecutionListenerSupport {
    private final Logger logger = LoggerFactory.getLogger(ParameterSettingListener.class);
    private final JdbcPagingItemReader<?> reader;
    private final FlatFileItemWriter<?> writer;

    public ParameterSettingListener(JdbcPagingItemReader<?> reader, FlatFileItemWriter<?> writer) {
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public void beforeStep(StepExecution stepExecution){
        String partitionName = stepExecution.getExecutionContext().getString("partitionName");
        logger.info("Setting writer resource > output/users_export_{}.csv",partitionName);
        if(writer != null){
            writer.setResource(new FileSystemResource("output/users_export_"+ partitionName + ".csv"));
        }
        logger.info("Reader parameters: minId={}, maxId={}",stepExecution.getExecutionContext().getInt("minId")
        ,stepExecution.getExecutionContext().getInt("maxId"));
    }
    @Override
    public ExitStatus afterStep(StepExecution stepExecution){
        return stepExecution.getExitStatus();
    }
}
