package com.ssginc.showpingrefactoring.batch.listener;

import com.ssginc.showpingrefactoring.batch.service.SlackAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobFailureListener implements JobExecutionListener {

    private final SlackAlertService slackAlertService; // 슬랙 서비스 주입

    @Override
    public void afterJob(JobExecution jobExecution) {
        // 배치가 FAILED 상태로 종료된 경우에만 실행
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            String jobName = jobExecution.getJobInstance().getJobName();

            String failedStep = jobExecution.getStepExecutions().stream()
                    .filter(s -> s.getStatus() == BatchStatus.FAILED)
                    .map(StepExecution::getStepName)
                    .findFirst()
                    .orElse("Unknown Step");

            // 2. 예외 메시지에서 핵심 내용만 추출 (첫 줄만 가져오기)
            String fullError = jobExecution.getExitStatus().getExitDescription();
            String summaryError = fullError.split("\n")[0]; // 보통 첫 줄에 에러 타입과 메시지가 나옵니다.

            // 슬랙 서비스 호출
            slackAlertService.sendSlackAlert(jobName, failedStep);
        }
    }
}
