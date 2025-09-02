package com.comma.soomteum.config;

import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Decoder feignDecoder() {
        // Feign의 기본 응답 디코더로 JacksonDecoder를 사용하도록 설정.
        return new JacksonDecoder();
    }
}