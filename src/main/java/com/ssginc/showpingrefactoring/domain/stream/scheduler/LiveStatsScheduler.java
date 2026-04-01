package com.ssginc.showpingrefactoring.domain.stream.scheduler;

import com.google.gson.JsonObject;
import com.ssginc.showpingrefactoring.common.handler.LiveHandler;
import com.ssginc.showpingrefactoring.common.util.UserSession;
import com.ssginc.showpingrefactoring.domain.stream.service.LiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveStatsScheduler {

    private final TaskScheduler taskScheduler;

    private final LiveService liveService;

    private final LiveHandler liveHandler;

    private final Map<Long, Long> streamStartTimes = new ConcurrentHashMap<>();

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void startViewCountScheduler(Long streamNo) {
        if (scheduledTasks.containsKey(streamNo)) {
            log.warn("이미 스케줄러가 동작 중인 방송입니다. streamNo: {}", streamNo);
            return;
        }

        streamStartTimes.put(streamNo, System.currentTimeMillis() / 1000);

        // 3초마다 해당 streamNo에 대해서만 실행
        ScheduledFuture<?> task = taskScheduler.scheduleAtFixedRate(() -> {
            broadcastViewCountByStream(streamNo);
        }, 3000);

        scheduledTasks.put(streamNo, task);
        log.info("라이브 스케줄러 시작 [streamNo: {}]", streamNo);
    }

    public void stopViewCountScheduler(Long streamNo) {
        ScheduledFuture<?> task = scheduledTasks.get(streamNo);
        if (task != null) {
            task.cancel(false);
            scheduledTasks.remove(streamNo);
            log.info("라이브 스케줄러 중단 [streamNo: {}]", streamNo);
        }
    }

    private void broadcastViewCountByStream(Long streamNo) {
        UserSession currentPresenter = liveHandler.getPresenterUserSession();
        ConcurrentHashMap<String, UserSession> currentViewers = liveHandler.getViewers();

        Long startTime = streamStartTimes.get(streamNo);

        // 해당 streamNo를 보고 있는 유저 필터링 (Long 비교)
        long count = currentViewers.values().stream()
                .filter(user -> {
                    Object attr = user.getSession().getAttributes().get("streamNo");
                    return attr != null && streamNo.equals(Long.valueOf(attr.toString()));
                })
                .count();

        // DB 저장 (Long 타입 streamNo 전달)
        liveService.saveSnapshot(streamNo, (int) count, startTime);

        // 메시지 구성
        JsonObject countMessage = new JsonObject();
        countMessage.addProperty("id", "viewerCountUpdate");
        countMessage.addProperty("count", count);

        // 메시지 전송
        if (isSameStream(currentPresenter, streamNo)) {
            sendMessage(currentPresenter, countMessage, streamNo);
        }

        currentViewers.values().stream()
                .filter(user -> isSameStream(user, streamNo))
                .forEach(user -> sendMessage(user, countMessage, streamNo));

    }

    private boolean isSameStream(UserSession user, Long streamNo) {
        if (user == null || user.getSession() == null) return false;
        Object attr = user.getSession().getAttributes().get("streamNo");
        return attr != null && streamNo.equals(Long.valueOf(attr.toString()));
    }

    private void sendMessage(UserSession user, JsonObject message, Long streamNo) {
        try {
            synchronized (user.getSession()) {
                if (user.getSession().isOpen()) {
                    user.sendMessage(message);
                }
            }
        } catch (IOException e) {
            log.error("메시지 전송 실패 [streamNo: {}, sessionId: {}]: {}",
                    streamNo, user.getSession().getId(), e.getMessage());
        }
    }

}
