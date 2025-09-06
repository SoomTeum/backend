package com.comma.soomteum.domain.theme.repository;

import com.comma.soomteum.domain.theme.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
}