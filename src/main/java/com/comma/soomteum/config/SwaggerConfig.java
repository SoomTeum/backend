package com.comma.soomteum.config;

import com.comma.soomteum.global.response.ExceptionDto; // ★ 에러 바디 DTO
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Info info = new Info()
                .title("숨틈 API")
                .description("개발 중인 백엔드 API 명세입니다.")
                .version("v1.0.0");

        String jwtSchemeName = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    // ⬇️ /api/kor/** 그룹에 공통 에러 응답(400/502/500) 자동 주입
    @Bean
    GroupedOpenApi korGroup(OpenApiCustomizer korCommonResponses) {
        return GroupedOpenApi.builder()
                .group("KOR Service2")
                .pathsToMatch("/api/kor/**")        // 전역 적용 원하면 "/**"
                .addOpenApiCustomizer(korCommonResponses)
                .build();
    }

    @Bean
    OpenApiCustomizer korCommonResponses() {
        return openApi -> {
            // 1) ExceptionDto 스키마 등록(안전하게 명시)
            var modelMap = io.swagger.v3.core.converter.ModelConverters.getInstance()
                    .read(ExceptionDto.class);
            if (openApi.getComponents() != null && modelMap != null) {
                modelMap.forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            }

            // 2) 공통 에러 본문 (application/json + ExceptionDto)
            var errorContent = new Content().addMediaType(
                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    new io.swagger.v3.oas.models.media.MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ExceptionDto"))
            );

            // 3) 모든 Operation에 400/502/500 응답 추가
            if (openApi.getPaths() == null) return;
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(op -> {
                        op.getResponses()
                                .addApiResponse("400", new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("요청 파라미터 오류")
                                        .content(errorContent))
                                .addApiResponse("502", new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("외부 관광 API 오류")
                                        .content(errorContent))
                                .addApiResponse("500", new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("서버 오류")
                                        .content(errorContent));
                    })
            );
        };
    }
}
