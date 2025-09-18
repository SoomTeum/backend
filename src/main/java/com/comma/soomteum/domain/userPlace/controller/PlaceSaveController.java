package com.comma.soomteum.domain.userPlace.controller;

import com.comma.soomteum.domain.auth.annotation.LoginUser;
import com.comma.soomteum.domain.user.entity.User;
import com.comma.soomteum.domain.userPlace.dto.PlaceActionRequestDto;
import com.comma.soomteum.domain.userPlace.dto.PlaceSaveStatusResponseDto;
import com.comma.soomteum.domain.userPlace.dto.UserPlacePageResponseDto;
import com.comma.soomteum.domain.userPlace.dto.UserPlaceResponseDto;
import com.comma.soomteum.domain.userPlace.enums.UserActionType;
import com.comma.soomteum.domain.userPlace.service.UserPlaceService;
import com.comma.soomteum.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "My Saved Places", description = "내 여행지 저장(북마크) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my/places")
public class PlaceSaveController {

    private final UserPlaceService userPlaceService;

    @Operation(
            summary = "저장 설정(멱등)",
            description = "해당 장소를 내 저장 목록에 추가합니다. 이미 저장되어 있어도 성공을 반환합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공",
                    content = @Content(schema = @Schema(implementation = UserPlaceResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
    })
    @PutMapping("/save")
    public ResponseEntity<ApiResponse<UserPlaceResponseDto>> save(
            @Parameter(hidden = true) @LoginUser User user,
            @Valid @RequestBody PlaceActionRequestDto request) {


        var dto = userPlaceService.setActionByContentId(
                user.getUserId(),
                request.getContentId(), 
                request.getRegionName(), 
                request.getThemeName(), 
                request.getCnctrLevel(),
                UserActionType.SAVE, 
                true);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @Operation(
            summary = "저장 해제(멱등)",
            description = "해당 장소를 내 저장 목록에서 제거합니다. 이미 제거된 상태여도 성공을 반환합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 해제 성공",
                    content = @Content(schema = @Schema(implementation = UserPlaceResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "장소를 찾을 수 없음")
    })
    @DeleteMapping("/save")
    public ResponseEntity<ApiResponse<UserPlaceResponseDto>> unsave(
            @Parameter(hidden = true) @LoginUser User user,
            @Valid @RequestBody PlaceActionRequestDto request) {

        var dto = userPlaceService.removeSaveByContentId(
                user.getUserId(), request.getContentId());
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @Operation(
            summary = "내 저장 목록 조회",
            description = "현재 로그인 사용자의 저장 목록을 페이징으로 조회합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserPlacePageResponseDto.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<UserPlacePageResponseDto>> mySaved(
            @Parameter(hidden = true) @LoginUser User user,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(value = "size", defaultValue = "20") int size) {

        var result = userPlaceService.getMyPlaces(user.getUserId (), UserActionType.SAVE, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(
            summary = "장소 저장 상태 조회", 
            description = "사용자가 특정 장소를 저장했는지 확인합니다.",
            security = @SecurityRequirement(name = "JWT TOKEN")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 상태 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/save/status")
    public ResponseEntity<ApiResponse<PlaceSaveStatusResponseDto>> getPlaceSaveStatus(
            @Parameter(description = "공공데이터 API의 컨텐츠 ID", required = true, example = "128758")
            @RequestParam String contentId,
            @Parameter(hidden = true) @LoginUser User user) {
        boolean isSaved = userPlaceService.isUserActionExists(user.getUserId(), contentId, UserActionType.SAVE);
        PlaceSaveStatusResponseDto responseDto = PlaceSaveStatusResponseDto.of(isSaved, contentId);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }
}
