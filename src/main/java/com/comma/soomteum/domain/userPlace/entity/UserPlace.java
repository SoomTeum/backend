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
                        name = "user_place_unique",
                        columnNames = {"user_id", "place_id"}
                )
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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserActionType type;

    @Builder
    public UserPlace(User user, Place place, UserActionType type) {
        this.user = user;
        this.place = place;
        this.type = type;
    }
}