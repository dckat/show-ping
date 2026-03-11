package com.ssginc.showpingrefactoring.domain.member.dto.object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberCacheProfileDto {

    private Long ageGroup;
    private String gender;

}
