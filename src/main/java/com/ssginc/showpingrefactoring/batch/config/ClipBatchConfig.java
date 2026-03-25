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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class ClipBatchConfig {

    private final JobFailureListener jobFailureListener;

    private final ClipDataReader clipDataReader;
    private final ClipProcessor clipProcessor;
    private final ClipWriter clipWriter;

    @Bean(name = "createClipJob")
    public Job createClipJob(JobRepository jobRepository, Step createClipStep) {
        return new JobBuilder("createClipJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(createClipStep)
                .listener(jobFailureListener)
                .build();
    }

    @Bean
    public Step createClipStep(JobRepository jobRepository,
                               @Qualifier("CustomRetryTemplate") RetryTemplate retryTemplate) {

        return new StepBuilder("createClipStep", jobRepository)
                .<LiveStreamLogInfo, List<ClipSegment>>chunk(1)
                .reader(clipDataReader)
                .processor(clipProcessor)
                .writer(clipWriter)
                .build();
    }

}
