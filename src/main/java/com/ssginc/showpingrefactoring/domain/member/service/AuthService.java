package com.ssginc.showpingrefactoring.domain.member.service;

import com.ssginc.showpingrefactoring.domain.member.dto.object.MemberCacheProfileDto;
import com.ssginc.showpingrefactoring.domain.member.dto.request.LoginRequestDto;
import com.ssginc.showpingrefactoring.domain.member.dto.response.LoginResponseDto;
import com.ssginc.showpingrefactoring.domain.member.dto.request.ReissueRequestDto;
import com.ssginc.showpingrefactoring.domain.member.entity.Member;

import java.util.Map;

public interface AuthService {
    Map<String, String> login(LoginRequestDto request);

    void deleteAllSessions(String memberId);

    String[] reissue(String refreshToken);

    MemberCacheProfileDto getMemberCacheProfile(Member member);
}
