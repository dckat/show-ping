package com.ssginc.showpingrefactoring.batch.job.reader;

import com.ssginc.showpingrefactoring.domain.stream.dto.object.LiveStreamLogInfo;
import com.ssginc.showpingrefactoring.domain.stream.service.HlsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
@StepScope
@Slf4j
public class ClipDataReader implements ItemReader<LiveStreamLogInfo> {

    @Value("#{jobParameters['streamNo']}")
    private Long streamNo;

    @Value("#{jobParameters['streamTitle']}")
    private String streamTitle;

    private final RedisTemplate<String, String> redisTemplate;

    private boolean isRead = false;

    private final HlsService hlsService;

    @Override
    public LiveStreamLogInfo read() throws Exception {
        // 이미 읽었다면 null을 반환하여 배치를 종료함
        if (isRead) {
            isRead = false;
            return null;
        }

        log.info("레디스 Data 읽기 시작: {}", streamNo);

        // 시청자 수 데이터 로드 (ZSET: score=timestamp, value=count)
        Map<Long, Double> viewerMap = fetchViewerData(streamNo);
        if (viewerMap.isEmpty()) {
            log.warn("No viewer data found in Redis for: {}", streamNo);
            return null;
        }

        // 원본 영상 실제 길이 획득 (구간 보정을 위함)
        Long duration = hlsService.getStreamDuration(streamTitle);

        isRead = true;

        return LiveStreamLogInfo.builder()
                .streamNo(streamNo)
                .streamTitle(streamTitle)
                .streamDuration(duration)
                .viewerMap(viewerMap)
                .build();
    }

    private Map<Long, Double> fetchViewerData(Long streamNo) {
        String key = "stats:" + streamNo;

        // 모든 범위를 가져옴
        Set<ZSetOperations.TypedTuple<String>> rawSnapshots =
                redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);

        // 데이터가 없으면 종료
        if (rawSnapshots == null || rawSnapshots.isEmpty()) {
            return null;
        }

        // 시간순 정렬을 위한 treeMap 사용
        Map<Long, Double> viewerDataMap = new TreeMap<>();

        for (ZSetOperations.TypedTuple<String> tuple : rawSnapshots) {
            String value = tuple.getValue();

            if (value != null) {
                String[] parts = value.split(":");
                long ts = Long.parseLong(parts[0]);
                double count = Double.parseDouble(parts[1]);
                viewerDataMap.put(ts, count);
            }
        }

        return viewerDataMap;
    }

}
