package com.ssginc.showpingrefactoring.batch.config;

import com.ssginc.showpingrefactoring.batch.listener.JobFailureListener;
import com.ssginc.showpingrefactoring.batch.util.CustomRetryTemplate;
import com.ssginc.showpingrefactoring.domain.stream.service.HlsService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author dckat
 * HLS 배치 작업 구성하는 클래스
 * <p>
 */
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class HlsBatchConfig {

    private final JobFailureListener jobFailureListener;

    /**
     * 지정된 JobRepository와 Step을 사용하여 HLS 저장 작업(Job)을 생성하는 메서드
     * @param jobRepository  JobRepository 객체
     * @param createHlsStep    HLS 저장 Step
     * @return 생성된 HLS 저장 Job
     */
    @Bean (name = "createHlsJob")
    public Job createHlsJob(JobRepository jobRepository, Step createHlsStep) {
        return new JobBuilder("createHlsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(createHlsStep)
                .listener(jobFailureListener)
                .build();
    }

    /**
     * 지정된 JobRepository, PlatformTransactionManager, 그리고 HlsService를 사용하여
     * HLS 저장 Step을 생성하는 메서드
     * @param jobRepository  JobRepository 객체
     * @param tx             PlatformTransactionManager 객체
     * @param hlsService     HlsService 객체
     * @return 생성된 HLS 저장 Step
     */
    @Bean
    public Step createHlsStep(JobRepository jobRepository,
                              PlatformTransactionManager tx,
                              HlsService hlsService,
                              @Qualifier("CustomRetryTemplate") RetryTemplate retryTemplate) {

        return new StepBuilder("createHlsStep", jobRepository)
                .tasklet((contrib, ctx) -> {
                    String title = ctx.getStepContext()
                            .getStepExecution()
                            .getJobParameters()
                            .getString("title");

                    retryTemplate.execute(retryContext -> {
                        hlsService.createHLS(title);
                        return null;
                    });

                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

}
