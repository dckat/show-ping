package com.ssginc.showpingrefactoring.batch.job;

import com.ssginc.showpingrefactoring.batch.dto.VodRecommendDto;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class VodRecommendWriter implements ItemWriter<VodRecommendDto> {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void write(Chunk<? extends VodRecommendDto> chunk) {
        for (VodRecommendDto item : chunk) {
            String key = String.format("recommend:age:%d:gender:%s", item.getAgeGroup(), item.getGender());

            // Redis 저장 (score = viewCount)
            redisTemplate.opsForZSet().add(key, String.valueOf(item.getStreamNo()), item.getViewCount());
            redisTemplate.expire(key, 25, TimeUnit.HOURS);
        }
    }

}
