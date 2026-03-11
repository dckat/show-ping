package com.ssginc.showpingrefactoring.batch.job;

import com.ssginc.showpingrefactoring.domain.stream.dto.object.VodRecommendDto;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class VodRecommendProcessor implements ItemProcessor<VodRecommendDto, VodRecommendDto> {

    @Override
    public VodRecommendDto process(VodRecommendDto item) throws Exception {
        // 테스트용: 강제로 에러 발생 시켜 슬랙 알림 확인
//        if (true)
//            throw new RuntimeException("슬랙 알림 테스트용 에러입니다!");

        return item;
    }
}