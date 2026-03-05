package com.ssginc.showpingrefactoring.domain.stream.service.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssginc.showpingrefactoring.common.dto.SliceResponseDto;
import com.ssginc.showpingrefactoring.common.exception.CustomException;
import com.ssginc.showpingrefactoring.common.exception.ErrorCode;
import com.ssginc.showpingrefactoring.domain.member.dto.object.MemberCacheProfileDto;
import com.ssginc.showpingrefactoring.domain.member.entity.Member;
import com.ssginc.showpingrefactoring.domain.member.repository.MemberRepository;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.VodListCursor;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.VodRecommendDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.repository.VodRowProjection;
import com.ssginc.showpingrefactoring.infrastructure.NCP.storage.StorageLoader;
import com.ssginc.showpingrefactoring.domain.stream.repository.VodRepository;
import com.ssginc.showpingrefactoring.domain.stream.service.VodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VodServiceImpl implements VodService {

    @Value("${download.path}")
    private String VIDEO_PATH;

    private final StorageLoader storageLoader;

    private final VodRepository vodRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    private final MemberRepository memberRepository;

    private final ObjectMapper objectMapper;

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

    @Override
    public List<VodRecommendDto> getRecommendInfo(Long userId) {
        String profileKey = "user:profile:" + userId;

        // Redis 내 사용자 프로필(연령대, 성별) 확인
        MemberCacheProfileDto profile = (MemberCacheProfileDto) redisTemplate.opsForValue().get(profileKey);

        // 캐시에 없으면 DB 조회 후 Redis에 저장
        if (profile == null) {
            Member member = memberRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 연령대 계산 (한국 나이 기준 예시)
            long age = LocalDate.now().getYear() - member.getMemberBirthdate().getYear() + 1;
            Long ageGroup = (age / 10) * 10;

            profile = new MemberCacheProfileDto(ageGroup, member.getMemberGender());

            // 24시간 동안 캐싱
            redisTemplate.opsForValue().set(profileKey, profile, Duration.ofHours(24));
        }

        // 해당 연령/성별 조합의 추천 VOD 리스트를 Redis에서 조회
        // Redis 키 ex: "recommend:age:20:gender:MALE"
        String key = String.format("recommend:age:%d:gender:%s", profile.getAgeGroup(), profile.getGender());
        Set<Object> results = redisTemplate.opsForZSet().reverseRange(key, 0, 3);

        List<VodRecommendDto> recommendList = results.stream()
                .map(obj -> {
                    // 이미 객체 타입이라면 캐스팅,
                    // 만약 LinkedHashMap으로 리턴된다면 objectMapper로 컨버팅
                    if (obj instanceof VodRecommendDto) {
                        return (VodRecommendDto) obj;
                    } else {
                        // GenericJackson2JsonRedisSerializer 등을 쓸 때 가끔 발생
                        return objectMapper.convertValue(obj, VodRecommendDto.class);
                    }
                })
                .collect(Collectors.toList());

        return recommendList;
    }

}
