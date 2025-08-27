package com.comma.soomteum.domain.userPlace.service;

import com.comma.soomteum.domain.place.entity.Place;
import com.comma.soomteum.domain.place.service.PlaceService;
import com.comma.soomteum.domain.user.entity.User;
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
    public UserPlaceResponseDto likePlace(Long userId, Long placeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Place place = placeService.findPlaceById(placeId);

        userPlaceRepository.findByUser_UserIdAndPlace_PlaceId(userId, placeId).ifPresent(userPlace -> {
            throw new CustomException(ErrorCode.ALREADY_LIKED_PLACE);
        });

        UserPlace userPlace = UserPlace.builder()
                .user(user)
                .place(place)
                .type(UserActionType.LIKE)
                .build();

        userPlaceRepository.save(userPlace);
        place.increaseLikeCount();

        return UserPlaceResponseDto.builder()
                .message("userId: " + userId + " PlaceId: " + placeId + " 좋아요가 성공했습니다.")
                .build();
    }

    @Transactional
    public UserPlaceResponseDto unlikePlace(Long userId, Long placeId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Place place = placeService.findPlaceById(placeId);

        UserPlace userPlace = userPlaceRepository.findByUser_UserIdAndPlace_PlaceId(userId, placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_LIKED_PLACE));

        userPlaceRepository.delete(userPlace);
        place.decreaseLikeCount();

        return UserPlaceResponseDto.builder()
                .message("userId: " + userId + " PlaceId: " + placeId + " 좋아요 해제가 성공했습니다.")
                .build();
    }

    @Transactional(readOnly = true)
    public long getPlaceLikeCount(Long placeId) {
        Place place = placeService.findPlaceById(placeId);
        return place.getLikeCount();
    }
}
