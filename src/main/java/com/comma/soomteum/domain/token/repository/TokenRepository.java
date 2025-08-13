package com.comma.soomteum.domain.token.repository;

import com.comma.soomteum.domain.token.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface TokenRepository extends JpaRepository<Token, Long>  {
    Optional<Token> findByRefreshToken(String refreshToken);
}
