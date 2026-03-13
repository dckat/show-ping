package com.ssginc.showpingrefactoring.batch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SlackAlertService {

    @Value("${slack.webhook.url}")
    private String webhookUrl;

    public void sendSlackAlert(String jobName, String stepName) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. 전체 메시지 구성
        Map<String, Object> payload = new HashMap<>();
        payload.put("text", "⚠️ *배치 작업 중 예외가 발생했습니다.*");

        // 2. 시각적 강조를 위한 Attachment 추가
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", "#FF0000"); // 빨간색 바
        attachment.put("ts", System.currentTimeMillis() / 1000); // 발생 시간 타임스탬프

        // 3. 상세 내용을 필드 형태로 구성 (가독성 최적화)
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("실패 작업 (Job)", jobName, true));
        fields.add(createField("실패 단계 (Step)", stepName, true));
        fields.add(createField("발생 시각", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), true));
        attachment.put("fields", fields);
        payload.put("attachments", List.of(attachment));
        try {
            restTemplate.postForEntity(webhookUrl, payload, String.class);
        } catch (Exception e) {
            log.error("슬랙 메시지 전송 중 오류 발생: {}", e.getMessage());
        }
    }

    private Map<String, Object> createField(String title, String value, boolean isShort) {
        return Map.of("title", title, "value", value, "short", isShort);
    }

}
