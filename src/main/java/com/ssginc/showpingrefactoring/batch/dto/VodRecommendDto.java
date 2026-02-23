package com.ssginc.showpingrefactoring.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VodRecommendDto {
    private Integer ageGroup;
    private String gender;
    private Long streamNo;
    private Long viewCount;
}
