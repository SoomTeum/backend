package com.comma.soomteum.domain.userPlace.controller;

import com.comma.soomteum.domain.auth.annotation.LoginUser;
import com.comma.soomteum.domain.user.entity.User;
import com.comma.soomteum.domain.userPlace.dto.PlaceActionRequestDto;
import com.comma.soomteum.domain.userPlace.dto.PlaceLikeStatusResponseDto;
import com.comma.soomteum.domain.userPlace.dto.UserPlaceResponseDto;
import com.comma.soomteum.domain.userPlace.enums.UserActionType;
import com.comma.soomteum.domain.userPlace.service.UserPlaceService;
import com.comma.soomteum.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Place", description = "사용자 장소 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceLikeController {

    private final UserPlaceService userPlaceService;

    @Operation(
            summary = "장소 좋아요", 
            description = "사용자가 특정 장소에 '좋아요'를 표시합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 장소를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 좋아요를 누른 장소")
    })
    @PostMapping("/like")
    public ResponseEntity<ApiResponse<UserPlaceResponseDto>> likePlace(
            @Parameter(hidden = true) @LoginUser User user,
            @Valid @RequestBody PlaceActionRequestDto request) {
        UserPlaceResponseDto responseDto = userPlaceService.setActionByContentId(
                user.getUserId(),
                request.getContentId(),
                request.getRegionName(),
                request.getThemeName(),
                request.getPlaceName(),
                request.getCnctrLevel(),
                UserActionType.LIKE,
                true);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }

    @Operation(
            summary = "장소 좋아요 해제", 
            description = "사용자가 특정 장소에 표시한 '좋아요'를 해제합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 장소를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "좋아요를 누르지 않은 장소")
    })
    @DeleteMapping("/like")
    public ResponseEntity<ApiResponse<UserPlaceResponseDto>> unlikePlace(
            @Parameter(hidden = true) @LoginUser User user,
            @Parameter(description = "공공데이터 API의 컨텐츠 ID", required = true, example = "128758")
            @RequestParam String contentId) {
        UserPlaceResponseDto responseDto = userPlaceService.removeLikeByContentId(
                user.getUserId(), contentId);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }

    @Operation(summary = "장소 좋아요 개수 조회", description = "특정 장소의 '좋아요' 총 개수를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 개수 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
    })
    @GetMapping("/like/count")
    public ResponseEntity<ApiResponse<Long>> getPlaceLikeCount(
            @RequestParam String contentId) {
        long likeCount = userPlaceService.getActionCountByContentId(contentId, UserActionType.LIKE);
        return ResponseEntity.ok(ApiResponse.ok(likeCount));
    }

    @Operation(
            summary = "장소 좋아요 상태 조회", 
            description = "사용자가 특정 장소에 좋아요를 눌렀는지 확인합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 상태 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/like/status")
    public ResponseEntity<ApiResponse<PlaceLikeStatusResponseDto>> getPlaceLikeStatus(
            @Parameter(description = "공공데이터 API의 컨텐츠 ID", required = true, example = "128758")
            @RequestParam String contentId,
            @Parameter(hidden = true) @LoginUser User user) {
        boolean isLiked = userPlaceService.isUserActionExists(user.getUserId(), contentId, UserActionType.LIKE);
        PlaceLikeStatusResponseDto responseDto = PlaceLikeStatusResponseDto.of(isLiked, contentId);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }
}
