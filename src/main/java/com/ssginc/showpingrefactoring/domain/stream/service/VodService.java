package com.ssginc.showpingrefactoring.domain.stream.service;

import com.ssginc.showpingrefactoring.common.dto.SliceResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.VodListCursor;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.VodRecommendDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VodService {

    String uploadVideo(String title);

    Page<StreamResponseDto> findVods(@Min(value = 0) Long categoryNo, String sort, Pageable pageable);

    StreamResponseDto getVodByNo(Long streamNo);

    SliceResponseDto<StreamResponseDto, VodListCursor> findVodsScroll(
            Long categoryNo,
            VodListCursor cursor,
            int pageSize);

    List<VodRecommendDto> getRecommendInfo(Long memberNo, String memberId);
}
