package com.comma.soomteum.domain.userPlace.entity;

import com.comma.soomteum.domain.BaseEntity;
import com.comma.soomteum.domain.place.entity.Place;
import com.comma.soomteum.domain.user.entity.User;
import com.comma.soomteum.domain.userPlace.enums.UserActionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_attraction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userContentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserActionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;
}
