package com.ssginc.showpingrefactoring.domain.stream.service.implement;

import com.ssginc.showpingrefactoring.common.dto.SliceResponseDto;
import com.ssginc.showpingrefactoring.common.exception.CustomException;
import com.ssginc.showpingrefactoring.common.exception.ErrorCode;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.VodListCursor;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.repository.VodRowProjection;
import com.ssginc.showpingrefactoring.domain.watch.dto.object.WatchHistoryCursor;
import com.ssginc.showpingrefactoring.domain.watch.dto.response.WatchResponseDto;
import com.ssginc.showpingrefactoring.domain.watch.repository.WatchRowProjection;
import com.ssginc.showpingrefactoring.infrastructure.NCP.storage.StorageLoader;
import com.ssginc.showpingrefactoring.domain.stream.repository.VodRepository;
import com.ssginc.showpingrefactoring.domain.stream.service.VodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VodServiceImpl implements VodService {

    @Value("${download.path}")
    private String VIDEO_PATH;

    private final StorageLoader storageLoader;

    private final VodRepository vodRepository;

    @Override
    public String uploadVideo(String title) {
        String filePath = VIDEO_PATH + title;
        File file = new File(filePath);
        String fileName = file.getName();
        return storageLoader.uploadMp4File(file, fileName);
    }

    @Override
    public Page<StreamResponseDto> findVods(Long categoryNo, String sort, Pageable pageable) {
        Page<StreamResponseDto> vodPage;
        boolean isMostView = "mostViewed".equals(sort);

        // 조회수 기반 정렬이 포함된 경우
        if (isMostView) {
            if (categoryNo > 0) {
                vodPage = vodRepository.findByCategoryIdOrderByViewsDesc(categoryNo, pageable);
            } else {
                vodPage = vodRepository.findAllOrderByViewsDesc(pageable);
            }
        }
        else {
            if (categoryNo > 0) {
                vodPage = vodRepository.findByCategory(categoryNo, pageable);
            }
            else {
                vodPage = vodRepository.findAllVod(pageable);
            }
        }

        if (!vodPage.hasContent()) {
            throw new CustomException(ErrorCode.VOD_LIST_EMPTY);
        }

        return vodPage;
    }

    @Override
    public SliceResponseDto<StreamResponseDto, VodListCursor> findVodsScroll(
            Long categoryNo,
            VodListCursor cursor,
            int pageSize) {

        List<VodRowProjection> rows = vodRepository.getVodScroll(
                categoryNo,
                (cursor == null ? null : cursor.streamNo()),
                pageSize+1
        );

        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            rows = rows.subList(0, pageSize);
        }

        VodListCursor nextStreamNoCursor = null;

        if (hasMore && !rows.isEmpty()) {
            var last = rows.get(rows.size() - 1);
            nextStreamNoCursor = new VodListCursor(last.getStreamNo());
        }

        List<StreamResponseDto> content = rows.stream()
                .map(r -> new StreamResponseDto(
                        r.getStreamNo(),
                        r.getStreamTitle(),
                        r.getStreamDescription(),
                        r.getStreamStatus(),
                        r.getCategoryNo(),
                        r.getCategoryName(),
                        r.getProductName(),
                        r.getProductPrice(),
                        r.getProductSale(),
                        r.getProductImg(),
                        r.getStreamStartTime(),
                        r.getStreamEndTime()
                ))
                .toList();

        return SliceResponseDto.of(content, hasMore, nextStreamNoCursor);
    }

    /**
     * 영상번호로 VOD 정보를 가져오는 메서드
     * @param streamNo 영상 번호
     * @return 쿼리를 통해 가져온 영상정보 DTO
     */
    @Override
    public StreamResponseDto getVodByNo(Long streamNo) {
        StreamResponseDto vodDto = vodRepository.findVodByNo(streamNo);

        if (vodDto == null) {
        throw new CustomException(ErrorCode.STREAM_NOT_FOUND);
        }

        return vodDto;
    }
}
