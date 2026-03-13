package com.ssginc.showpingrefactoring.domain.member.service.implement;

import com.ssginc.showpingrefactoring.domain.member.dto.object.MemberCacheProfileDto;
import com.ssginc.showpingrefactoring.domain.member.dto.request.LoginRequestDto;
import com.ssginc.showpingrefactoring.domain.member.entity.Member;
import com.ssginc.showpingrefactoring.common.exception.CustomException;
import com.ssginc.showpingrefactoring.common.exception.ErrorCode;
import com.ssginc.showpingrefactoring.common.jwt.JwtTokenProvider;
import com.ssginc.showpingrefactoring.domain.member.repository.MemberRepository;
import com.ssginc.showpingrefactoring.domain.member.service.AuthService;

import com.ssginc.showpingrefactoring.domain.member.service.RedisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenService redisTokenService;
    private final PasswordEncoder passwordEncoder;

    @Value("${profile.expiration.hours}")
    private Long memberProfileExpiration;

    @Qualifier("redisObjectTemplate")
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, String> login(LoginRequestDto request) {
        String memberId = request.getMemberId();
        String rawPassword = request.getPassword();

        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(rawPassword, member.getMemberPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 캐싱용 회원 프로파일 (Key: user:profile:123, Value: {ageGroup: "20", gender: "MALE"})
        String cacheKey = "user:profile:" + member.getMemberId();
        MemberCacheProfileDto profile = getMemberCacheProfile(member);

        // 만료시간: 1시간 (accessToken 유효기간에 맞춰서 설정)
        redisTemplate.opsForValue().set(cacheKey, profile, Duration.ofHours(memberProfileExpiration));

        String role = member.getMemberRole().name();

        //  memberNo 포함해서 AT 발급 (JwtTokenProvider가 mid 클레임으로 넣음)
        String accessToken = jwtTokenProvider.generateAccessToken(
                memberId,
                member.getMemberNo(),
                role
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);
        redisTokenService.saveRefreshToken(memberId, refreshToken);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("status", "LOGIN_SUCCESS");
        return tokens;
    }

    @Override
    public String[] reissue(String refreshTokenFromCookie) {
        String memberId;
        try {
            memberId = jwtTokenProvider.getUsernameAllowExpired(refreshTokenFromCookie);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String stored = redisTokenService.getRefreshToken(memberId);
        if (stored == null || !stored.equals(refreshTokenFromCookie)) {
            redisTokenService.deleteRefreshToken(refreshTokenFromCookie);
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String role = member.getMemberRole().name();

        //  재발급 시에도 memberNo 포함
        String newAT = jwtTokenProvider.generateAccessToken(
                memberId,
                member.getMemberNo(),
                role
        );
        String newRT = jwtTokenProvider.generateRefreshToken(memberId);

        boolean ok = redisTokenService.rotateRefreshToken(memberId, refreshTokenFromCookie, newRT);
        if (!ok) throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);

        // 캐싱용 프로파일 재발급 (Key: user:profile:123, Value: {ageGroup: "20", gender: "MALE"})
        String cacheKey = "user:profile:" + member.getMemberId();
        MemberCacheProfileDto profile = getMemberCacheProfile(member);

        // 만료시간: 1시간 (accessToken 유효기간에 맞춰서 설정)
        redisTemplate.opsForValue().set(cacheKey, profile, Duration.ofHours(memberProfileExpiration));

        return new String[]{newAT, newRT};
    }

    @Override
    public void deleteAllSessions(String memberId) {
        redisTokenService.deleteAllRefreshTokens(memberId);
        deleteUserProfile(memberId);
    }

    @Override
    public MemberCacheProfileDto getMemberCacheProfile(Member member) {
        // 연령대 계산 및 정보 추출
        long age = LocalDate.now().getYear() - member.getMemberBirthdate().getYear() + 1;
        Long ageGroup = (age / 10) * 10;
        String gender = member.getMemberGender();

        return new MemberCacheProfileDto(ageGroup, gender);
    }

    public void deleteUserProfile(String memberId) {
        String key = "user:profile:" + memberId;

        boolean ok = redisTemplate.delete(key);

        if (!ok)
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }
}
