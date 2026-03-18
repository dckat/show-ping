package com.ssginc.showpingrefactoring.domain.stream.scheduler;

import com.google.gson.JsonObject;
import com.ssginc.showpingrefactoring.common.handler.LiveHandler;
import com.ssginc.showpingrefactoring.common.util.UserSession;
import com.ssginc.showpingrefactoring.domain.stream.service.LiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveStatsScheduler {

    private final LiveService liveService;
    private final LiveHandler liveHandler;

    @Scheduled(fixedRate = 5000)
    public void broadCastLiveStats() {
        ConcurrentHashMap<String, UserSession> currentViewers = liveHandler.getViewers();

        if (currentViewers.isEmpty()) return;

        // streamNo별로 인원수 집계
        Map<String, Long> statsByStream = currentViewers.values().stream()
                .map(userSession -> (String) userSession.getSession().getAttributes().get("streamNo"))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // 각 streamNo별로 처리
        statsByStream.forEach((streamNo, count) -> {
            liveService.saveSnapshot(streamNo, count.intValue());

            // 3. 해당 streamNo를 보고 있는 시청자들에게만 메시지 전송
            JsonObject countMessage = new JsonObject();
            countMessage.addProperty("id", "viewerCountUpdate");
            countMessage.addProperty("count", count);

            currentViewers.values().stream()
                    .filter(user -> streamNo.equals(user.getSession().getAttributes().get("streamNo")))
                    .forEach(user -> {
                        try {
                            synchronized (user.getSession()) {
                                if (user.getSession().isOpen()) {
                                    user.sendMessage(countMessage);
                                }
                            }
                        } catch (IOException e) {
                            log.error("인원수 전송 에러: {}", e.getMessage());
                        }
                    });
        });

    }

}
