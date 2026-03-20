package com.ssginc.showpingrefactoring.domain.stream.service;

import com.ssginc.showpingrefactoring.domain.stream.dto.request.RegisterLiveRequestDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.GetLiveProductInfoResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.GetLiveRegisterInfoResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StartLiveResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

public interface LiveService {

    GetLiveProductInfoResponseDto getStreamProductInfo(Long streamNo);

    StreamResponseDto getOnair();

    Page<StreamResponseDto> getAllActiveByPage(Pageable pageable);

    Page<StreamResponseDto> getAllStandbyByPage(Pageable pageable);

    Long registerLive(String memberId, RegisterLiveRequestDto request);

    StartLiveResponseDto startLive(Long streamNo);

    Boolean stopLive(Long streamNo);

    GetLiveRegisterInfoResponseDto getLiveRegisterInfo(String memberId);

    void incrementCount(String streamNo);

    void decrementCount(String streamNo);

    void saveSnapshot(Long streamNo, int viewerCount);

    Long getViewCount(Long streamNo);

}
