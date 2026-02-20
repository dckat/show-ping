package com.ssginc.showpingrefactoring.domain.stream.swagger;

import com.ssginc.showpingrefactoring.domain.stream.dto.request.VodListRequestDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.request.VodListScrollRequestDto;
import com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

import static com.ssginc.showpingrefactoring.common.swagger.ApiResponseDescriptions.SERVER_ERROR;

@Tag(name = "vod", description = "VOD 관련 API")
@ApiResponse(responseCode = "500", description = SERVER_ERROR)
public interface VodApiSpecification {

    @Operation(
            summary = "VOD 목록 조회",
            description = "등록된 모든 VOD 정보를 조회"
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(implementation = StreamResponseDto.class)
                    )
            )
    )
    ResponseEntity<?> listVod(@ParameterObject @Valid VodListRequestDto vodListRequestDto);

    @Operation(
            summary = "VOD 목록 조회",
            description = "등록된 모든 VOD 정보를 조회"
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(implementation = StreamResponseDto.class)
                    )
            )
    )
    public ResponseEntity<?> listVodScroll(@Valid @ModelAttribute VodListScrollRequestDto vodListScrollRequestDto);

    @Operation(
            summary     = "VOD 업로드",
            description = "제목(title)을 전달받아 VOD를 업로드합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "업로드 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            type        = "String",
                            description = "업로드 처리 결과",
                            example     = "title"
                    )
            )
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "업로드할 VOD 제목을 담은 JSON",
            required    = true,
            content     = @Content(
                    mediaType = "application/json",
                    schema    = @Schema(
                            type        = "object",
                            description = "요청 파일 예시",
                            example     = "{\"title\":\"노트북_특가\"}"
                    )
            )
    )
    @PostMapping("/upload")
    ResponseEntity<?> uploadVod(@RequestBody Map<String, String> requestData);

    @Operation(
            summary     = "자막 조회",
            description = "요청한 파일 제목에 해당하는 자막 정보를 JSON 형태로 반환"
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "object", description = "자막 정보 JSON")
            )
    )
    @Parameter(
            name        = "title",
            in          = ParameterIn.PATH,
            description = "조회할 자막 파일의 제목 (확장자 제외)",
            required    = true,
            example     = "노트북_특가"
    )
    ResponseEntity<?> getSubtitle(@PathVariable String title);
}
