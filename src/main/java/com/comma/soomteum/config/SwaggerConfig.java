package com.comma.soomteum.config;

import com.comma.soomteum.global.response.ExceptionDto;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
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

    @Bean
    OpenApiCustomizer commonErrorResponses() {
        return openApi -> {
            var modelMap = io.swagger.v3.core.converter.ModelConverters.getInstance()
                    .read(ExceptionDto.class);
            if (openApi.getComponents() != null && modelMap != null) {
                modelMap.forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            }

            var errorContent = new Content().addMediaType(
                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    new io.swagger.v3.oas.models.media.MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ExceptionDto"))
            );

            if (openApi.getPaths() == null) return;
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(op -> {
                        op.getResponses()
                                .addApiResponse("400", new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("요청 파라미터 오류").content(errorContent))
                                .addApiResponse("502", new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("외부 관광 API 오류").content(errorContent))
                                .addApiResponse("500", new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("서버 오류").content(errorContent));
                    })
            );
        };
    }

    // KOR Service2 그룹
    @Bean
    GroupedOpenApi korGroup(OpenApiCustomizer commonErrorResponses) {
        return GroupedOpenApi.builder()
                .group("KOR Service2")
                .pathsToMatch("/api/kor/**")
                .addOpenApiCustomizer(commonErrorResponses)
                .build();
    }

    // Auth(카카오) 그룹
    @Bean
    GroupedOpenApi authGroup(OpenApiCustomizer commonErrorResponses) {
        return GroupedOpenApi.builder()
                .group("Auth")
                .pathsToMatch("/api/auth/**")     // AuthController 경로와 일치
                .addOpenApiCustomizer(commonErrorResponses)
                .build();
    }

     @Bean
     GroupedOpenApi allApis() {
         return GroupedOpenApi.builder()
                 .group("All APIs")
                 .pathsToMatch("/**")
                 .build();
     }
}
