package com.ssginc.showpingrefactoring.batch.config;

import com.ssginc.showpingrefactoring.batch.job.processor.ClipProcessor;
import com.ssginc.showpingrefactoring.batch.job.reader.ClipDataReader;
import com.ssginc.showpingrefactoring.batch.job.writer.ClipWriter;
import com.ssginc.showpingrefactoring.batch.listener.JobFailureListener;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.ClipSegment;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.LiveStreamLogInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class ClipBatchConfig {

    private final FixedBackOffPolicy customBackOffPolicy;
    private final JobFailureListener jobFailureListener;
    private final PlatformTransactionManager tx;

    private final JobRepository jobRepository;

    private final ClipDataReader clipDataReader;
    private final ClipProcessor clipProcessor;
    private final ClipWriter clipWriter;

    @Bean
    public Job createClipJob() {
        return new JobBuilder("createClipJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(createClipStep())
                .listener(jobFailureListener)
                .build();
    }

    @Bean
    public Step createClipStep() {
        return new StepBuilder("createClipStep", jobRepository)
                .<LiveStreamLogInfo, List<ClipSegment>>chunk(1, tx)
                .reader(clipDataReader)
                .processor(clipProcessor)
                .writer(clipWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .backOffPolicy(customBackOffPolicy)
                .build();
    }

}
