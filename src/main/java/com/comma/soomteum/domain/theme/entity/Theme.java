package com.comma.soomteum.domain.theme.entity;

import com.comma.soomteum.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "theme")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Theme extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long themeId;

    @Column(length = 10, nullable = false)
    private String cat1;

    @Column(length = 10, nullable = false)
    private String cat2;

    @Column(length = 100, nullable = false)
    private String name;
}
