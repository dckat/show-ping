package com.ssginc.showpingrefactoring.domain.stream.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClipResponseDto {

    private Long clipNo;
    private String streamTitle;
    private String clipPath;

}
