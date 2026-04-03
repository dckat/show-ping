package com.ssginc.showpingrefactoring.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    //[요청 파라미터 에러 코드]
    INVALID_METHOD_ARGUMENT(HttpStatus.BAD_REQUEST, "CO001", "유효하지 않은 요청 파라미터입니다."),

    //[리소스 관련 에러 코드]
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "CO002", "스토리지에 해당 파일이 존재하지 않습니다."),

    //[회원 관련 에러 코드 추가]
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ME001", "로그인이 필요한 요청입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ME002", "회원을 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "ME003", "비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "ME004", "RefreshToken이 유효하지 않습니다."),

    DUPLICATED_MEMBER_ID(HttpStatus.CONFLICT, "ME005", "이미 사용 중인 회원 ID입니다."),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "ME006", "이미 사용 중인 이메일입니다."),
    DUPLICATED_PHONE(HttpStatus.CONFLICT, "ME007", "이미 사용 중인 전화번호입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "ME008", "관리자 권한이 필요합니다."),
    TOTP_NOT_REGISTERED(HttpStatus.BAD_REQUEST, "ME009", "TOTP 등록이 완료되지 않은 사용자입니다."),
    INVALID_TOTP_CODE(HttpStatus.UNAUTHORIZED, "ME010", "잘못된 OTP 인증 코드입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "ME011", "이메일 인증이 완료되지 않았습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ME012", "데이터베이스 오류가 발생했습니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "ME013", "접근이 거부되었습니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "ME014", "Access Token이 만료되었습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "ME015", "Access Token이 유효하지 않습니다."),
    PROFILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ME016", "프로파일이 정상적으로 삭제되지 않았습니다."),

    // [라이브.VOD 관련 에러 코드]
    // [공통]
    STREAM_NOT_FOUND(HttpStatus.NOT_FOUND, "ST001", "요청한 영상 정보가 없습니다."),
    UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ST002", "업로드 중 문제가 발생하였습니다"),

    // [VOD]
    VOD_LIST_EMPTY(HttpStatus.NOT_FOUND, "VO001", "VOD 목록이 비어있습니다."),
    HLS_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "VO002", "HLS 변환에 실패하였습니다."),

    //[시청]
    WATCH_LIST_EMPTY(HttpStatus.NOT_FOUND, "WA001", "시청 내역이 존재하지 않습니다.")

    //(추후 추가 가능: 이미 존재하는 이메일/아이디 등)
    ;

    // HTTP status code
    private final HttpStatus status;

    // Custom response code
    private final String code;

    // Custome response message
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
