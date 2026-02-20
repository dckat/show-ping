package com.ssginc.showpingrefactoring.domain.stream.repository;

import com.ssginc.showpingrefactoring.domain.stream.entity.StreamStatus;

import java.time.LocalDateTime;

public interface VodRowProjection {

    Long getStreamNo();
    String getStreamTitle();
    String getStreamDescription();
    StreamStatus getStreamStatus();
    Long getCategoryNo();
    String getCategoryName();
    String getProductName();
    Long getProductPrice();
    Integer getProductSale();
    String getProductImg();
    LocalDateTime getStreamStartTime();
    LocalDateTime getStreamEndTime();

}
