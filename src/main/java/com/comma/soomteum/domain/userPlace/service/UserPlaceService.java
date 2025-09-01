package com.comma.soomteum.domain.userPlace.service;

import com.comma.soomteum.domain.place.service.PlaceService;
import com.comma.soomteum.domain.user.repository.UserRepository;
import com.comma.soomteum.domain.userPlace.dto.UserPlaceResponseDto;
import com.comma.soomteum.domain.userPlace.entity.UserPlace;
import com.comma.soomteum.domain.userPlace.enums.UserActionType;
import com.comma.soomteum.domain.userPlace.repository.UserPlaceRepository;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPlaceService {

    private final UserPlaceRepository userPlaceRepository;
    private final UserRepository userRepository;
    private final PlaceService placeService;

    @Transactional
    public UserPlaceResponseDto setAction(Long userId, Long placeId, UserActionType type, boolean enable) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        var place = placeService.findPlaceById(placeId);

        var existing = userPlaceRepository.findByUser_UserIdAndPlace_PlaceIdAndType(userId, placeId, type);

        boolean changed = false;

        if (enable) {
            if (existing.isEmpty()) {
                try {
                    userPlaceRepository.save(UserPlace.builder()
                            .user(user)
                            .place(place)
                            .type(type)
                            .build());
                    if (type == UserActionType.LIKE) place.increaseLikeCount();
                    changed = true;
                } catch (org.springframework.dao.DataIntegrityViolationException ignore) {
                }
            }
        } else {
            if (existing.isPresent()) {
                userPlaceRepository.delete(existing.get());
                if (type == UserActionType.LIKE) place.decreaseLikeCount();
                changed = true;
            }
        }

        return UserPlaceResponseDto.builder()
                .placeId(placeId)
                .type(type)
                .enabled(enable)   // 현재 상태
                .changed(changed)  // 실제 변경 여부
                .message(type + " " + (enable ? "ON" : "OFF"))
                .build();
    }

    // 좋아요용 카운트
    @Transactional(readOnly = true)
    public long getActionCount(Long placeId, UserActionType type) {
        if (type == UserActionType.LIKE) {
            return placeService.findPlaceById(placeId).getLikeCount();
        }
        return userPlaceRepository.countByPlace_PlaceIdAndType(placeId, type);
    }

    @Transactional public UserPlaceResponseDto likePlace(Long userId, Long placeId) {
        return setAction(userId, placeId, UserActionType.LIKE, true);
    }
    @Transactional public UserPlaceResponseDto unlikePlace(Long userId, Long placeId) {
        return setAction(userId, placeId, UserActionType.LIKE, false);
    }
    @Transactional public UserPlaceResponseDto savePlace(Long userId, Long placeId) {
        return setAction(userId, placeId, UserActionType.SAVE, true);
    }
    @Transactional public UserPlaceResponseDto unsavePlace(Long userId, Long placeId) {
        return setAction(userId, placeId, UserActionType.SAVE, false);
    }
}
