package com.ssginc.showpingrefactoring.domain.stream.service;

import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

import java.io.IOException;

public interface HlsService {

    Mono<?> getHLSV1(String title);

    Mono<?> getTsSegmentV1(String title, String segment);

    String createHLS(String title) throws IOException, InterruptedException;

    Mono<?> getHLSV2Flux(String title);

    Mono<?> getTsSegmentV2Flux(String title, String segment);

    Resource getHLSV2(String title);

    Resource getTsSegmentV2(String title, String segment);

    Long getStreamDuration(String streamTitle);

}
