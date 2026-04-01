package com.ssginc.showpingrefactoring.common.controller;

import com.ssginc.showpingrefactoring.common.swagger.BatchSpecification;
import com.ssginc.showpingrefactoring.domain.stream.dto.request.VodTitleRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author dckat
 * 배치 작업을 처리하는 컨트롤러 클래스
 * <p>
 */
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController implements BatchSpecification {

    private final JobLauncher jobLauncher;
    private final Job createHlsJob;
    private final Job createSubtitleJob;
    private final Job createClipJob;

    /**
     * HLS 저장 작업을 실행하는 컨트롤러 메서드
     * @param vodTitleRequestDto 파일 요청 DTO (파일 제목 포함)
     * @return 작업 실행 ID를 포함한 ResponseEntity
     */
    @Override
    @PostMapping("/hls/create")
    public ResponseEntity<String> createHLS(@RequestBody VodTitleRequestDto vodTitleRequestDto) throws Exception {
        String title = vodTitleRequestDto.getFileTitle();
        JobParameters params = new JobParametersBuilder()
                .addString("title", title)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution exec = jobLauncher.run(createHlsJob, params);
        return ResponseEntity.accepted()
                .body("saveHlsJob 실행 ID=" + exec.getId());
    }

    /**
     * 자막 생성 작업을 실행하는 컨트롤러 메서드
     * @param vodTitleRequestDto 파일 요청 DTO (파일 제목 포함)
     * @return 작업 실행 ID를 포함한 ResponseEntity
     */
    @Override
    @PostMapping("/subtitle/create")
    public ResponseEntity<String> createSubtitle(@RequestBody VodTitleRequestDto vodTitleRequestDto) throws Exception {
        String title = vodTitleRequestDto.getFileTitle();
        JobParameters params = new JobParametersBuilder()
                .addString("title", title)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution exec = jobLauncher.run(createSubtitleJob, params);
        return ResponseEntity.accepted()
                .body("createSubtitleJob 실행 ID=" + exec.getId());
    }

    @Override
    @PostMapping("/clip/create/{streamNo}")
    public ResponseEntity<String> createClip(
            @PathVariable Long streamNo,
            @RequestBody VodTitleRequestDto vodTitleRequestDto) throws Exception {
        String title = vodTitleRequestDto.getFileTitle();

        JobParameters params = new JobParametersBuilder()
                .addLong("streamNo", streamNo)
                .addString("streamTitle", title)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution exec = jobLauncher.run(createClipJob, params);
        return ResponseEntity.accepted()
                .body("createClipJob 실행 ID=" + exec.getId());

    }

}
