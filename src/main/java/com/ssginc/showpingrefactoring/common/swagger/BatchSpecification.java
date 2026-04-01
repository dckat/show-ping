package com.ssginc.showpingrefactoring.common.swagger;


import com.ssginc.showpingrefactoring.domain.stream.dto.request.VodTitleRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import static com.ssginc.showpingrefactoring.common.swagger.ApiResponseDescriptions.SERVER_ERROR;

@Tag(name = "batch", description = "Batch 작업 수행")
@ApiResponse(responseCode = "500", description = SERVER_ERROR)
public interface BatchSpecification {

    @Operation(
            summary = "HLS 생성 배치",
            description = "영상 저장 후 HLS 생성을 위한 배치작업 진행"
    )
    @ApiResponse(
            responseCode = "202",
            description = "HLS 생성을 위한 batch 작업 생성 완료"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "영상 제목 정보 DTO",
            required    = true,
            content     = @Content(
                    mediaType = "application/json",
                    schema    = @Schema(
                            type        = "object",
                            description = "HLS 생성을 위한 영상 제목",
                            example     = "{\"fileTitle\":\"stream_01_노트북_특가.mp4\"}"
                    )
            )
    )
    ResponseEntity<String> createHLS(@RequestBody VodTitleRequestDto vodTitleRequestDto) throws Exception;

    @Operation(
            summary = "자막 생성 배치",
            description = "영상 저장 후 자막생성을 위한 배치작업 진행"
    )
    @ApiResponse(
            responseCode = "202",
            description = "자막 생성을 위한 batch 작업 생성 완료"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "자막 생성을 위한 영상 제목 정보 DTO",
            required    = true,
            content     = @Content(
                    mediaType = "application/json",
                    schema    = @Schema(
                            type        = "object",
                            description = "자막 생성을 위한 영상 제목",
                            example     = "{\"fileTitle\":\"stream_01_노트북_특가.mp4\"}"
                    )
            )
    )
    ResponseEntity<String> createSubtitle(@RequestBody VodTitleRequestDto vodTitleRequestDto) throws Exception;

    @Operation(
            summary = "영상 클립 생성 배치",
            description = "영상 클립 생성을 위한 배치작업 진행"
    )
    @ApiResponse(
            responseCode = "202",
            description = "영상 클립 생성을 위한 batch 작업 생성 완료"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "클립 생성을 위한 제목 정보 DTO",
            required    = true,
            content     = @Content(
                    mediaType = "application/json",
                    schema    = @Schema(
                            type        = "object",
                            description = "클립 생성을 위한 영상 제목",
                            example     = "{\"fileTitle\":\"stream_01_노트북_특가.mp4\"}"
                    )
            )
    )
    ResponseEntity<String> createClip(
            @PathVariable Long streamNo,
            @RequestBody VodTitleRequestDto vodTitleRequestDto) throws Exception;
}
