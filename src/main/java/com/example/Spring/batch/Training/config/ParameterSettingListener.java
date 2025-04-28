package com.example.Spring.batch.Training.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.database.JdbcPagingItemReader;

import java.util.HashMap;
import java.util.Map;

public class ParameterSettingListener extends StepExecutionListenerSupport {
    private final Logger logger = LoggerFactory.getLogger(ParameterSettingListener.class);
    private final JdbcPagingItemReader<?> reader;

    public ParameterSettingListener(JdbcPagingItemReader<?> reader) {
        this.reader = reader;
    }

    @Override
    public void beforeStep(StepExecution stepExecution){
        Map<String,Object> params = new HashMap<>();
        params.put("minId", stepExecution.getExecutionContext().getLong("minId"));
        params.put("maxId", stepExecution.getExecutionContext().getLong("maxId"));
        reader.setParameterValues(params);
        logger.info("Setting parameters for partition: minId={}, maxId={}",params.get("minId"), params.get("maxId"));
    }
    @Override
    public ExitStatus afterStep(StepExecution stepExecution){
        return stepExecution.getExitStatus();
    }
}
