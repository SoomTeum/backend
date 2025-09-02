package com.comma.soomteum.domain.userPlace.entity;

import com.comma.soomteum.domain.BaseEntity;
import com.comma.soomteum.domain.place.entity.Place;
import com.comma.soomteum.domain.user.entity.User;
import com.comma.soomteum.domain.userPlace.enums.UserActionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "user_place",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_place_user_place_type",
                        columnNames = {"user_id", "place_id", "type"}
                )
        },
        indexes = {
                @Index(name = "idx_user_place_place_type", columnList = "place_id,type"), // like 카운트/랭킹에 유용
                @Index(name = "idx_user_place_user_type",  columnList = "user_id,type")   // 내 저장/좋아요 목록 조회
        }
)
public class UserPlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_place_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserActionType type;

    @Builder
    public UserPlace(User user, Place place, UserActionType type) {
        this.user = user;
        this.place = place;
        this.type = type;
    }
}
