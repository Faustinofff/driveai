package com.driveai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI driveAiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DriveAI API")
                        .version("1.0.0")
                        .description("""
                                API del e-commerce de autos DriveAI.
                                Catálogo, carrito, checkout transaccional y asesor de compra
                                inteligente basado en LLM (Groq Cloud, llama-3.3-70b-versatile).
                                """)
                        .contact(new Contact().name("DriveAI Team").email("dev@driveai.local"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}