package com.comma.soomteum.domain.userPlace.controller;

import com.comma.soomteum.domain.auth.annotation.LoginUser;
import com.comma.soomteum.domain.user.entity.User;
import com.comma.soomteum.domain.userPlace.dto.UserPlaceResponseDto;
import com.comma.soomteum.domain.userPlace.service.UserPlaceService;
import com.comma.soomteum.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Place", description = "사용자 장소 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class UserPlaceController {

    private final UserPlaceService userPlaceService;

    @Operation(summary = "장소 좋아요", description = "사용자가 특정 장소에 '좋아요'를 표시합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 장소를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 좋아요를 누른 장소")
    })
    @PostMapping("/{placeId}/likes")
    public ResponseEntity<ApiResponse<UserPlaceResponseDto>> likePlace(
            @Parameter(hidden = true) @LoginUser User user,
            @PathVariable Long placeId) {
        UserPlaceResponseDto responseDto = userPlaceService.likePlace(user.getUserId(), placeId);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }

    @Operation(summary = "장소 좋아요 해제", description = "사용자가 특정 장소에 표시한 '좋아요'를 해제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 장소를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "좋아요를 누르지 않은 장소")
    })
    @DeleteMapping("/{placeId}/likes")
    public ResponseEntity<ApiResponse<UserPlaceResponseDto>> unlikePlace(
            @Parameter(hidden = true) @LoginUser User user,
            @PathVariable Long placeId) {
        UserPlaceResponseDto responseDto = userPlaceService.unlikePlace(user.getUserId(), placeId);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }

    @Operation(summary = "장소 좋아요 개수 조회", description = "특정 장소의 '좋아요' 총 개수를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 개수 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
    })
    @GetMapping("/{placeId}/likes/count")
    public ResponseEntity<ApiResponse<Long>> getPlaceLikeCount(
            @PathVariable Long placeId) {
        long likeCount = userPlaceService.getPlaceLikeCount(placeId);
        return ResponseEntity.ok(ApiResponse.ok(likeCount));
    }
}
