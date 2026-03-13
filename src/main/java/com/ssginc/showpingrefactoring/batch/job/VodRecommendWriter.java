package com.ssginc.showpingrefactoring.batch.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.VodRecommendDto;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class VodRecommendWriter implements ItemWriter<VodRecommendDto> {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void write(Chunk<? extends VodRecommendDto> chunk) throws JsonProcessingException {
        for (VodRecommendDto item : chunk) {
            String key = String.format("recommend:age:%d:gender:%s", item.getAgeGroup(), item.getGender());

            redisTemplate.opsForZSet().add(key, item, (double) item.getViewCount());

            redisTemplate.expire(key, 25, TimeUnit.HOURS);
        }
    }

}
