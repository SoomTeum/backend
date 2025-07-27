package com.comma.soomteum.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {


    // ========================
    // 400 Bad Request
    // ========================
    BAD_REQUEST(400_000, HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_FILE_FORMAT(400_001, HttpStatus.BAD_REQUEST, "업로드된 파일 형식이 올바르지 않습니다."),
    INVALID_INPUT(400_002, HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    NULL_VALUE(400_003, HttpStatus.BAD_REQUEST, "Null 값이 들어왔습니다."),
    TEST_ERROR(400_004, HttpStatus.BAD_REQUEST, "테스트 에러입니다."),


    // ========================
    // 401 Unauthorized
    // ========================
    TOKEN_EXPIRED(401_000, HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID(401_001, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_NOT_FOUND(401_002, HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다."),
    TOKEN_UNSUPPORTED(401_003, HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰 형식입니다."),
    INVALID_CREDENTIALS(401_004, HttpStatus.UNAUTHORIZED, "인증 정보가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(401_005, HttpStatus.UNAUTHORIZED, "재발급 토큰이 유효하지 않습니다."),
    INVALID_ACCESS_TOKEN(401_006, HttpStatus.UNAUTHORIZED, "접근 토큰이 유효하지 않습니다."),
    INVALID_TOKEN(401_007, HttpStatus.UNAUTHORIZED, "토큰이 생성되지 않았습니다."),
    INVALID_LOGIN(401_008, HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    REFRESH_TOKEN_NOT_FOUND(401_011, HttpStatus.UNAUTHORIZED, "저장된 리프레시 토큰이 존재하지 않습니다."),
    REFRESH_TOKEN_MISMATCH(401_009, HttpStatus.UNAUTHORIZED, "저장된 리프레시 토큰과 일치하지 않습니다."),
    EXPIRED_REFRESH_TOKEN(401_010, HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),


    // ========================
    // 403 Forbidden
    // ========================
    FORBIDDEN(403_000, HttpStatus.FORBIDDEN, "접속 권한이 없습니다."),
    ACCESS_DENY(403_001, HttpStatus.FORBIDDEN, "접근이 거부되었습니다."),
    UNAUTHORIZED_POST_ACCESS(403_002, HttpStatus.FORBIDDEN, "해당 게시글에 접근할 권한이 없습니다."),


    // ========================
    // 404 Not Found
    // ========================
    NOT_FOUND_END_POINT(404_000, HttpStatus.NOT_FOUND, "요청한 대상이 존재하지 않습니다."),
    USER_NOT_FOUND(404_001, HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_NOT_FOUND_IN_COOKIE(404_002, HttpStatus.NOT_FOUND, "쿠키에서 사용자 정보를 찾을 수 없습니다."),
    POST_NOT_FOUND(404_003, HttpStatus.NOT_FOUND, "요청한 게시글을 찾을 수 없습니다."),
    POST_TYPE_NOT_FOUND(404_004, HttpStatus.NOT_FOUND, "게시글 타입을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(404_005, HttpStatus.NOT_FOUND, "요청한 댓글을 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(404_006, HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."),

    // ========================
    // 409 Conflict
    // ========================
    DUPLICATE_EMAIL(409_001, HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),


    // ========================
    // 500 Internal Server Error
    // ========================
    INTERNAL_SERVER_ERROR(500_000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.");

    // 기타 공통

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }


}