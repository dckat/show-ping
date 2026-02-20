package com.ssginc.showpingrefactoring.domain.stream.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VodListScrollRequestDto {

    @NotNull(message = "카테고리 번호는 필수입니다.")
    @Min(value = 0, message = "카테고리 번호는 0 이상이어야 합니다.")
    private Long categoryNo = 0L;

    private Long cursorStreamNo;

    @NotNull(message = "페이지 크기는 필수입니다.")
    private int pageSize;

}
