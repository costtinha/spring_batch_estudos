package com.example.Spring.batch.Training.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/job")
public class JobLaucherController {
    private final Logger log = LoggerFactory.getLogger(JobLaucherController.class);
    private final JobLauncher jobLauncher;
    private final Job importUserCSVJob;
    private final Job exportUserCSVJob;

    @Autowired
    public JobLaucherController(JobLauncher jobLauncher, @Qualifier("importUserCSVJob") Job importUserCSVJob,
                                @Qualifier("exportUserCSVJob") Job exportUserCSVJob) {
        this.jobLauncher = jobLauncher;
        this.importUserCSVJob = importUserCSVJob;
        this.exportUserCSVJob = exportUserCSVJob;
    }

    @PostMapping("/import")
    public String runJob(){
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("startAt",System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(importUserCSVJob,params);
            return "Job iniciado com sucesso";
        } catch (Exception e){
            log.error("Erro ao iniciar importUserCSVJob", e);
            e.printStackTrace();
            return "Erro ao iniciar job" + e.getMessage();
        }
    }

    @PostMapping("/export")
    public String runExportJob(){
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("startsAt",System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(exportUserCSVJob,params);
            return "Job de exportação iniciado";
        } catch (Exception e){
            log.error("Erro ao iniciar exportUserCSVJob",e);
            e.printStackTrace();
            return "Erro ao iniciar job" + e.getMessage();
        }
    }

    @GetMapping("/ping")
    public String ping() {
        return "Aplicação está rodando!";
    }
}
