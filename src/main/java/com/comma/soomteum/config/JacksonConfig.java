package com.comma.soomteum.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Root wrapping을 사용하지 않도록 설정 (기본값이 false이지만 명시적으로 설정)
        mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, false);
        mapper.findAndRegisterModules();
        return mapper;
    }
}