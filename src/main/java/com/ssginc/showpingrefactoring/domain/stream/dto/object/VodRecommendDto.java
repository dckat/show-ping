package com.ssginc.showpingrefactoring.domain.stream.dto.object;

import com.ssginc.showpingrefactoring.domain.stream.entity.StreamStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VodRecommendDto {

    private Integer ageGroup;               // 연령대

    private String gender;                  // 성별

    private Long streamNo;                  // 영상 번호

    private Long viewCount;                 // 시청 횟수 (최근 1주일 이내 기준)

    private String streamTitle;             // 영상 제목

    private String productName;             // 상품 이름

    private Long productPrice;              // 상품 가격

    private int productSale;                // 상품 할인율

    private String productImg;              // 상품 이미지

}
