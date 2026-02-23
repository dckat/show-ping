package com.ssginc.showpingrefactoring.batch.config;

import com.ssginc.showpingrefactoring.batch.dto.VodRecommendDto;
import com.ssginc.showpingrefactoring.batch.job.VodRecommendWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class VodRecommendBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager tx;

    private final JdbcPagingItemReader<VodRecommendDto> jdbcPagingReader;
    private final VodRecommendWriter recommendWriter;

    @Bean
    public Job vodRecommendJob() {
        return new JobBuilder("vodRecommendJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(vodRecommendStep())
                .build();
    }

    @Bean
    public Step vodRecommendStep() {
        return new StepBuilder("vodRecommendStep", jobRepository)
                .<VodRecommendDto, VodRecommendDto>chunk(500, tx)
                .reader(jdbcPagingReader) // 필드로 주입받은 빈을 사용
                .writer(recommendWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .backOffPolicy(backOffPolicy())
                .build();
    }

    @Bean
    public FixedBackOffPolicy backOffPolicy() {
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(2000L);
        return policy;
    }
}
