package com.comma.soomteum.domain.userPlace.service;

import com.comma.soomteum.domain.place.repository.PlaceRepository;
import com.comma.soomteum.domain.place.service.PlaceService;
import com.comma.soomteum.domain.user.repository.UserRepository;
import com.comma.soomteum.domain.userPlace.dto.UserPlaceItemDto;
import com.comma.soomteum.domain.userPlace.dto.UserPlacePageResponseDto;
import com.comma.soomteum.domain.userPlace.dto.UserPlaceResponseDto;
import com.comma.soomteum.domain.userPlace.entity.UserPlace;
import com.comma.soomteum.domain.userPlace.enums.UserActionType;
import com.comma.soomteum.domain.userPlace.repository.UserPlaceRepository;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class UserPlaceService {

    private final UserPlaceRepository userPlaceRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
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
                    if (type == UserActionType.LIKE) {
                        System.out.println("Before increase: likeCount = " + place.getLikeCount());
                        place.increaseLikeCount();
                        System.out.println("After increase: likeCount = " + place.getLikeCount());
                        placeRepository.save(place);
                        placeRepository.flush(); // 강제로 DB에 반영
                        System.out.println("Saved and flushed place with likeCount = " + place.getLikeCount());
                    }
                    changed = true;
                } catch (org.springframework.dao.DataIntegrityViolationException ignore) {
                }
            }
        } else {
            if (existing.isPresent()) {
                userPlaceRepository.delete(existing.get());
                if (type == UserActionType.LIKE) {
                    place.decreaseLikeCount();
                    placeRepository.save(place);
                }
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

    @Transactional
    public UserPlaceResponseDto removeLikeByContentId(Long userId, String contentId) {
        // contentId로 Place 찾기
        var placeOpt = placeService.findByContentId(contentId);
        if (placeOpt.isEmpty()) {
            throw new CustomException(ErrorCode.PLACE_NOT_FOUND);
        }
        
        var place = placeOpt.get();
        
        // 해당 사용자의 좋아요 찾기
        var existing = userPlaceRepository.findByUser_UserIdAndPlace_PlaceIdAndType(
                userId, place.getPlaceId(), UserActionType.LIKE);
        
        boolean changed = false;
        if (existing.isPresent()) {
            userPlaceRepository.delete(existing.get());
            place.decreaseLikeCount();
            placeRepository.save(place);
            placeRepository.flush();
            changed = true;
        }
        
        return UserPlaceResponseDto.builder()
                .placeId(place.getPlaceId())
                .type(UserActionType.LIKE)
                .enabled(false)
                .changed(changed)
                .message("좋아요 " + (changed ? "해제됨" : "이미 해제됨"))
                .build();
    }

    @Transactional(readOnly = true)
    public long getActionCountByContentId(String contentId, UserActionType type) {
        var placeOpt = placeService.findByContentId(contentId);
        if (placeOpt.isEmpty()) {
            return 0L;
        }
        
        var place = placeOpt.get();
        if (type == UserActionType.LIKE) {
            return place.getLikeCount();
        }
        return userPlaceRepository.countByPlace_PlaceIdAndType(place.getPlaceId(), type);
    }

    @Transactional(readOnly = true)
    public UserPlacePageResponseDto getMyPlaces(Long userId, UserActionType type, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        var page = userPlaceRepository.findByUser_UserIdAndType(userId, type, pageable);

        var items = page.getContent().stream().map(up -> {
            var p = up.getPlace();
            return UserPlaceItemDto.builder()
                    .placeId(p.getPlaceId())
                    // .title(p.getName())
                    // .imageUrl(p.getThumbnailUrl())
                    // .address(p.getAddress())
                    .savedAt(up.getCreatedAt())
                    .build();
        }).toList();

        return UserPlacePageResponseDto.of(
                items,
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast(),
                page.hasNext(), page.hasPrevious()
        );
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

    @Transactional
    public UserPlaceResponseDto setActionByContentId(Long userId, String contentId, Long regionId, Long themeId, BigDecimal cnctrLevel, UserActionType type, boolean enable) {
        if (userId == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        var place = placeService.findOrCreatePlace(contentId, regionId, themeId, cnctrLevel);
        
        if (place.getPlaceId() == null) {
            throw new CustomException(ErrorCode.PLACE_NOT_FOUND);
        }
        
        var existing = userPlaceRepository.findByUser_UserIdAndPlace_PlaceIdAndType(userId, place.getPlaceId(), type);

        boolean changed = false;

        if (enable) {
            if (existing.isEmpty()) {
                try {
                    userPlaceRepository.save(UserPlace.builder()
                            .user(user)
                            .place(place)
                            .type(type)
                            .build());
                    if (type == UserActionType.LIKE) {
                        System.out.println("Before increase: likeCount = " + place.getLikeCount());
                        place.increaseLikeCount();
                        System.out.println("After increase: likeCount = " + place.getLikeCount());
                        placeRepository.save(place);
                        placeRepository.flush(); // 강제로 DB에 반영
                        System.out.println("Saved and flushed place with likeCount = " + place.getLikeCount());
                    }
                    changed = true;
                } catch (org.springframework.dao.DataIntegrityViolationException ignore) {
                }
            }
        } else {
            if (existing.isPresent()) {
                userPlaceRepository.delete(existing.get());
                if (type == UserActionType.LIKE) {
                    place.decreaseLikeCount();
                    placeRepository.save(place);
                }
                changed = true;
            }
        }

        return UserPlaceResponseDto.builder()
                .placeId(place.getPlaceId())
                .type(type)
                .enabled(enable)
                .changed(changed)
                .message(type + " " + (enable ? "ON" : "OFF"))
                .build();
    }
}
