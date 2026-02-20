package com.ssginc.showpingrefactoring.domain.stream.controller;

import com.ssginc.showpingrefactoring.common.dto.PageResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.request.VodListRequestDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.service.SubtitleService;
import com.ssginc.showpingrefactoring.domain.stream.service.VodService;
import com.ssginc.showpingrefactoring.domain.stream.swagger.VodApiSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author dckat
 * VOD 관련 요청-응답을 수행하는 컨트롤러 클래스
 * <p>
 */
@RestController
@RequestMapping("/api/vod")
@RequiredArgsConstructor
public class VodApiController implements VodApiSpecification {

    private final VodService vodService;

    private final SubtitleService subtitleService;

    /**
     * Vod 목록을 페이징하여 반환해주는 컨트롤러 메서드
     * @param vodListRequestDto vod 목록을 보여주기 위한 기준 (페이지 번호. 카테고리 번호. 정렬 기준)
     * @return 전달할 응답객체 (json 형태로 전달)
     */
    @Override
    @GetMapping("/list")
    public ResponseEntity<?> listVod(@Valid @ModelAttribute VodListRequestDto vodListRequestDto) {
        Pageable pageable = PageRequest.of(vodListRequestDto.getPageNo(), 4);

        Page<StreamResponseDto> page = vodService.findVods(
                vodListRequestDto.getCategoryNo(),
                vodListRequestDto.getSort(),
                pageable);

        return ResponseEntity.ok(PageResponseDto.of(page));
    }

    /**
     * VOD 파일을 NCP Storage에 저장을 요청하는 컨트롤러 메서드
     * @param requestData 요청 데이터 정보
     * @return VOD의 저장결과 응답객체
     */
    @Override
    @PostMapping("/upload")
    public ResponseEntity<?> uploadVod(@RequestBody Map<String, String> requestData) {
        String title = requestData.get("title");
        return ResponseEntity.ok(vodService.uploadVideo(title));
    }

    /**
     * 파일 제목으로 자막 정보 파일을 가져오는 메서드
     * @param title 파일 제목
     * @return 자막 생성 여부 응답 객체
     */
    @Override
    @GetMapping("/subtitle/{title}.json")
    public ResponseEntity<?> getSubtitle(@PathVariable String title) {
        Resource subtitleJson = subtitleService.getSubtitle(title);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(subtitleJson);
    }

}
