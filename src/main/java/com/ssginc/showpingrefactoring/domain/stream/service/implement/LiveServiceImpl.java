package com.ssginc.showpingrefactoring.domain.stream.service.implement;

import com.ssginc.showpingrefactoring.common.exception.CustomException;
import com.ssginc.showpingrefactoring.common.exception.ErrorCode;
import com.ssginc.showpingrefactoring.domain.member.entity.Member;
import com.ssginc.showpingrefactoring.domain.member.repository.MemberRepository;
import com.ssginc.showpingrefactoring.domain.product.repository.ProductRepository;
import com.ssginc.showpingrefactoring.domain.product.entity.Product;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.CreateLiveDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.GetLiveRegisterInfoDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.request.RegisterLiveRequestDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.GetLiveProductInfoResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.GetLiveRegisterInfoResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StartLiveResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.entity.Stream;
import com.ssginc.showpingrefactoring.domain.stream.entity.StreamStatus;
import com.ssginc.showpingrefactoring.domain.stream.repository.LiveRepository;
import com.ssginc.showpingrefactoring.domain.stream.service.LiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveServiceImpl implements LiveService {

    private final LiveRepository liveRepository;

    private final ProductRepository productRepository;

    private final MemberRepository memberRepository;

    private final StringRedisTemplate redisTemplate;

    private static final String COUNT_KEY_PREFIX = "live:count:";

    private static final String STATS_KEY_PREFIX = "stats:";

    /**
     * 시청하려는 방송의 상품을 정보를 가져오는 메서드
     * @param streamNo 시청하려는 방송 번호
     * @return 시청하려는 방송의 상품 정보
     */
    public GetLiveProductInfoResponseDto getStreamProductInfo(Long streamNo){
        Stream stream = liveRepository.findById(streamNo).orElseThrow(() ->
                new CustomException(ErrorCode.STREAM_NOT_FOUND));

        Product product = stream.getProduct();

        // 상품의 원래 가격
        Long productPrice = product.getProductPrice();
        // 상품에 적용된 할인율
        Integer productSale = product.getProductSale();
        // 상품의 할인된 가격
        Long productSalePrice = productPrice;
        if (productSale != 0) {
            productSalePrice = (long) (productPrice * (1 - (double) productSale / 100));
        }

        // 포맷팅 지정
        NumberFormat nf = NumberFormat.getInstance(Locale.KOREA);

        // 원래 가격 포맷팅 적용
        String formattedPrice = nf.format(productPrice) + "원";
        // 할인 가격 포맷팅 적용
        String formattedSalePrice = nf.format(productSalePrice) + "원";


        return GetLiveProductInfoResponseDto.builder()
                .productNo(product.getProductNo())
                .productImg(product.getProductImg())
                .productName(product.getProductName())
                .productPrice(formattedPrice)
                .productSalePrice(formattedSalePrice)
                .build();
    }

    /**
     * 방송중인 라이브 방송 하나를 반환하는 메서드
     * @return 라이브 방송정보 1개
     */
    @Override
    public StreamResponseDto getOnair() {
        List<StreamResponseDto> onairList = liveRepository.findOnair();
        return onairList.isEmpty() ? null : onairList.get(0);
    }

    /**
     * 방송 중, 방송 대기 중인 방송들을 반환하는 메서드
     * @param pageable Pageable 객체
     * @return 방송 중, 방송 대기 중인 방송 정보 페이지 리스트
     */
    @Override
    public Page<StreamResponseDto> getAllActiveByPage(Pageable pageable) {
        return liveRepository.findAllActiveByPage(pageable);
    }

    /**
     * 방송 대기 중인 방송들을 반환하는 메서드
     * @param pageable Pageable 객체
     * @return 방송 대기 중인 방송 정보 페이지 리스트
     */
    @Override
    public Page<StreamResponseDto> getAllStandbyByPage(Pageable pageable) {
        return liveRepository.findAllStandbyByPage(pageable);
    }

    /**
     * 방송 데이터를 생성하거나 수정하는 메서드
     * @param memberId
     * @param request
     * @return 생성 혹은 수정된 방송의 방송 번호
     */
    @Override
    public Long registerLive(String memberId, RegisterLiveRequestDto request) {
        // 생성 혹은 수정된 streamNo
        Long responseStreamNo;
        Product product = productRepository.findById(request.getProductNo()).orElseThrow(RuntimeException::new);

        // 할인율 전처리
        Integer productSale = request.getProductSale();
        if (productSale == null) {
            productSale = 0;
        }

        Long streamNo = request.getStreamNo();
        // 기존에 등록된 방송 정보가 있는 경우 방송 데이터를 수정
        if (streamNo != null) {
            Stream stream = liveRepository.findById(streamNo).orElseThrow(RuntimeException::new);

            stream.setStreamTitle(request.getStreamTitle());
            stream.setStreamDescription(request.getStreamDescription());
            // 기존에 선택된 상품의 할인율을 0으로 반영
            stream.getProduct().setProductSale(0);
            // 방송 정보를 새로 선택한 상품으로 변경
            stream.setProduct(product);
            // 새로 선택된 상품의 할인율 반영
            product.setProductSale(productSale);

            stream.setStreamEnrollTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")));

            responseStreamNo = liveRepository.save(stream).getStreamNo();
        } else {    // 기존에 등록된 방송 정보가 없는 경우 새로 방송 데이터를 생성
            Member member = memberRepository.findByMemberId(memberId).orElseThrow(RuntimeException::new);

            CreateLiveDto stream = CreateLiveDto.builder()
                    .member(member)
                    .product(product)
                    .streamTitle(request.getStreamTitle())
                    .streamDescription(request.getStreamDescription())
                    .streamStatus(StreamStatus.STANDBY)
                    .streamEnrollTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                    .build();

            // 할인율 적용
            product.setProductSale(productSale);

            responseStreamNo = liveRepository.save(stream.toEntity()).getStreamNo();
        }

        return responseStreamNo;
    }

    /**
     * 방송 시작을 하는 메서드
     * @param streamNo 시작하려는 방송 번호
     * @return 시작한 방송에 대한 정보
     */
    public StartLiveResponseDto startLive(Long streamNo) {
        Stream stream = liveRepository.findById(streamNo).orElseThrow(RuntimeException::new);

        // 방송 시작 시간 설정
        stream.setStreamStartTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        // 방송 상태 송출 중으로 변경
        stream.setStreamStatus(StreamStatus.ONAIR);

        stream = liveRepository.save(stream);

        Product product = stream.getProduct();
        // 천 단위 구분 포맷팅
        NumberFormat nf = NumberFormat.getInstance(Locale.KOREA);
        String formattedPrice = nf.format(product.getProductPrice()) + "원";

        return StartLiveResponseDto.builder()
                .streamTitle(stream.getStreamTitle())
                .streamDescription(stream.getStreamDescription())
                .productImg(product.getProductImg())
                .productNo(product.getProductNo())
                .productName(product.getProductName())
                .productPrice(formattedPrice)
                .productSale(product.getProductSale())
                .build();
    }

    /**
     * 방송 종료를 하는 메서드
     * @param streamNo 종료하려는 방송 번호
     * @return 방송 종료 설정 적용 여부
     */
    public Boolean stopLive(Long streamNo) {
        Stream stream = liveRepository.findById(streamNo).orElseThrow(RuntimeException::new);

        // 방송 종료 시간 설정
        stream.setStreamEndTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        // 방송 상태 송출 종료로 변경
        stream.setStreamStatus(StreamStatus.ENDED);
        // 해당 방송의 상품의 할인율 0으로 변경(할인 종료)
        stream.getProduct().setProductSale(0);

        stream = liveRepository.save(stream);

        if (stream.getStreamEndTime() != null && stream.getStreamStatus() == StreamStatus.ENDED && stream.getProduct().getProductSale() == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 로그인한 사용자가 등록하고 시작하지 않은 방송 정보를 가져오는 메서드
     * @param memberId
     * @return GetStreamRegisterInfoDto 로그인한 회원으로 등록된 방송 정보
     */
    @Override
    public GetLiveRegisterInfoResponseDto getLiveRegisterInfo(String memberId) {
        try {
            GetLiveRegisterInfoDto streamInfo = liveRepository.findStandbyLiveByMemberId(memberId);

            if (streamInfo == null) {
                throw new RuntimeException("해당 회원으로 등록된 방송 정보가 없습니다.");
            }

            NumberFormat nf = NumberFormat.getInstance(Locale.KOREA);
            String formattedPrice = nf.format(streamInfo.getProductPrice()) + "원";

            return GetLiveRegisterInfoResponseDto.builder()
                    .streamNo(streamInfo.getStreamNo())
                    .streamTitle(streamInfo.getStreamTitle())
                    .streamDescription(streamInfo.getStreamDescription())
                    .productNo(streamInfo.getProductNo())
                    .productName(streamInfo.getProductName())
                    .productPrice(formattedPrice)
                    .productSale(streamInfo.getProductSale())
                    .productImg(streamInfo.getProductImg())
                    .build();
        } catch (RuntimeException e) {
            log.error("Exception [Err_Msg]: {}", e.getMessage());
            log.error("Exception [Err_Where]: {}", e.getStackTrace()[0]);

            return null;
        }

    }

    @Override
    public void incrementCount(String streamNo) {
        String key = COUNT_KEY_PREFIX + streamNo;
        redisTemplate.opsForValue().increment(key);
    }

    @Override
    public void decrementCount(String streamNo) {
        String key = COUNT_KEY_PREFIX + streamNo;
        redisTemplate.opsForValue().decrement(key);
    }

}
