package com.brandbuilder.reviewapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Add Java 8 Time module for LocalDateTime, LocalDate, etc.
        mapper.registerModule(new JavaTimeModule());

        // Add Hibernate module to handle lazy loading
        Hibernate5JakartaModule hibernateModule = new Hibernate5JakartaModule();
        hibernateModule.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, false);
        hibernateModule.configure(Hibernate5JakartaModule.Feature.USE_TRANSIENT_ANNOTATION, false);

        mapper.registerModule(hibernateModule);

        return mapper;
    }
}