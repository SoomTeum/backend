package com.comma.soomteum.domain.user.entity;

import com.comma.soomteum.domain.BaseEntity;
import com.comma.soomteum.domain.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(length = 50, nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String providerId;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    public void reactivate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isWithdrawn() {
        return this.status == UserStatus.WITHDRAWN;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
}
