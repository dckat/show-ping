package com.ssginc.showpingrefactoring.domain.stream.controller;

import com.ssginc.showpingrefactoring.domain.product.dto.response.GetProductListResponseDto;
import com.ssginc.showpingrefactoring.domain.product.service.ProductService;
import com.ssginc.showpingrefactoring.domain.stream.dto.request.RegisterLiveRequestDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.request.LiveRequestDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.GetLiveRegisterInfoResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StartLiveResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.scheduler.LiveStatsScheduler;
import com.ssginc.showpingrefactoring.domain.stream.service.LiveService;
import com.ssginc.showpingrefactoring.domain.stream.swagger.LiveApiSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveApiController implements LiveApiSpecification {

    private final LiveService liveService;

    private final ProductService productService;

    private final LiveStatsScheduler liveStatsScheduler;

    /**
     * 라이브 방송을 반환해주는 컨트롤러 메서드
     * @return 전달할 응답객체 (json 형태로 전달)
     */
    @GetMapping("/onair")
    @Override
    public ResponseEntity<?> getOnair() {
        StreamResponseDto onair = liveService.getOnair();

        // Map으로 전달할 응답객체 저장
        Map<String, Object> result = new HashMap<>();
        result.put("onair", onair);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 라이브 중.예정 방송목록을 반환해주는 컨트롤러 메서드
     * @return 전달할 응답객체 (json 형태로 전달)
     */
    @GetMapping("/active")
    @Override
    public ResponseEntity<?> getActive(@RequestParam(defaultValue = "0", name = "pageNo") int pageNo) {
        Pageable pageable = PageRequest.of(pageNo, 4);
        Page<StreamResponseDto> pageInfo = liveService.getAllActiveByPage(pageable);

        // Map으로 전달할 응답객체 저장
        Map<String, Object> result = new HashMap<>();
        result.put("pageInfo", pageInfo);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 준비중인 방송목록을 반환해주는 컨트롤러 메서드
     * @return 전달할 응답객체 (json 형태로 전달)
     */
    @GetMapping("/standby")
    @Override
    public ResponseEntity<?> getStandby(@RequestParam(defaultValue = "0", name = "pageNo") int pageNo) {
        Pageable pageable = PageRequest.of(pageNo, 4);;
        Page<StreamResponseDto> pageInfo = liveService.getAllStandbyByPage(pageable);

        // Map으로 전달할 응답객체 저장
        Map<String, Object> result = new HashMap<>();
        result.put("pageInfo", pageInfo);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

//    /**
//     * 방송 등록 화면에서 상품 선택을 위해 상품 목록을 반환해주는 메서드
//     * @return 상품 목록이 포함된 응답 객체
//     */
//    @GetMapping("/product/list")
//    @Override
//    public ResponseEntity<List<ProductItemDto>> getProductList() {
//        List<ProductItemDto> productItemDtoList = productService.getProducts();
//
//        return ResponseEntity.status(HttpStatus.OK).body(productItemDtoList);
//    }

//    /**
//     * 방송 등록 화면에서 상품 선택을 위해 상품 목록을 반환해주는 메서드
//     * offset 방식의 Paging 적용
//     * @param page 조회할 페이지
//     * @param size 조회할 개수
//     * @return
//     */
//    @GetMapping("/product/list")
//    @Override
//    public ResponseEntity<Page<ProductItemDto>> getProductList(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//        Page<ProductItemDto> result = productService.getProducts(page, size);
//
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }

    @GetMapping("/product/list")
    @Override
    public ResponseEntity<GetProductListResponseDto> getProductList(
            @RequestParam(required = false) Long lastProductNo,
            @RequestParam(defaultValue = "20") int size) {
        GetProductListResponseDto result = productService.getProducts(lastProductNo, size);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /**
     * 방송 등록을 하는 메서드
     * @param request 방송 등록에 필요한 방송 정보
     * @return 생성 혹은 수정된 방송 데이터의 방송 번호가 포함된 응답 객체
     */
    @PostMapping("/register")
    @Override
    public ResponseEntity<Map<String, Long>> resgisterLive(@AuthenticationPrincipal UserDetails userDetails, @RequestBody RegisterLiveRequestDto request) {
        String memberId = null;

        if (userDetails != null) {
            memberId = userDetails.getUsername();
        }

        Long streamNo = liveService.registerLive(memberId, request);

        Map<String, Long> response = new HashMap<>();
        response.put("streamNo", streamNo);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 방송 시작을 하는 메서드
     * @param request streamNo가 담긴 요청 객체
     * @return 시작한 방송에 대한 정보
     */
    @PostMapping("/start")
    @Override
    public ResponseEntity<StartLiveResponseDto> startLive(@RequestBody LiveRequestDto request) {
        StartLiveResponseDto response = liveService.startLive(request.getStreamNo());

        liveStatsScheduler.startViewCountScheduler(request.getStreamNo());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 방송 종료를 하는 메서드
     * @param request streamNo가 담긴 요청 객체
     * @return 방송 종료 설정 적용 여부
     */
    @PostMapping("/stop")
    @Override
    public ResponseEntity<Map<String, Boolean>> stopLive(@RequestBody LiveRequestDto request) {
        Boolean result = liveService.stopLive(request.getStreamNo());

        liveStatsScheduler.stopViewCountScheduler(request.getStreamNo());

        Map<String, Boolean> response = new HashMap<>();
        response.put("result", result);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 방송 페이지 이동시 필요한 정보를 가져오는 메서드
     * @param userDetails
     * @return 방송 정보가 담긴 응답 객체
     */
    @GetMapping("/live-info")
    @Override
    public ResponseEntity<GetLiveRegisterInfoResponseDto> getLiveInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String memberId = null;
        if (userDetails != null) {
            memberId = userDetails.getUsername();
        }

        GetLiveRegisterInfoResponseDto response = liveService.getLiveRegisterInfo(memberId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/viewCount/{streamNo}")
    public ResponseEntity<?> getViewCount(@PathVariable Long streamNo) {
        Long viewCount = liveService.getViewCount(streamNo);

        return ResponseEntity.status(HttpStatus.OK).body(viewCount);
    }

}
