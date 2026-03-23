package com.ssginc.showpingrefactoring.domain.stream.dto.object;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class StreamInfoDto {

    private Long streamNo;
    private String streamTitle;
    private Long streamDuration;
    private Map<Long, Double> viewerMap;

}
