package com.ssginc.showpingrefactoring.batch;

import com.ssginc.showpingrefactoring.common.util.AesGcmCrypto;
import com.ssginc.showpingrefactoring.domain.stream.service.HlsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration.class,
        org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration.class
})
public class HlsJobTest {

    @MockBean(name = "aesGcmCrypto")
    private AesGcmCrypto aesGcmCrypto;

    @MockBean
    private HlsService hlsService;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    @Qualifier("createHlsJob")
    private Job createHlsJob;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(createHlsJob);
    }

    @Test
    void retry_then_success() throws Exception {
        when(hlsService.createHLS("test-title"))
                .thenThrow(new SocketTimeoutException("t1"))
                .thenThrow(new SocketTimeoutException("t2"))
                .thenReturn("hls-path-or-id");

        JobParameters params = new JobParametersBuilder()
                .addString("title", "test-title")
                .addLong("run.id", System.currentTimeMillis()) // 파라미터 중복 방지
                .toJobParameters();

        JobExecution exec = jobLauncherTestUtils.launchJob(params);

        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(hlsService, times(3)).createHLS("test-title");
    }

    @Test
    void retry_exhausted_then_fail() throws Exception {
        // 3번 모두 실패
        when(hlsService.createHLS("test-title"))
                .thenThrow(new SocketTimeoutException("t1"))
                .thenThrow(new SocketTimeoutException("t2"))
                .thenThrow(new SocketTimeoutException("t3"));

        JobParameters params = new JobParametersBuilder()
                .addString("title", "test-title")
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution exec = jobLauncherTestUtils.launchJob(params);

        assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);
        verify(hlsService, times(3)).createHLS("test-title");
    }
}
