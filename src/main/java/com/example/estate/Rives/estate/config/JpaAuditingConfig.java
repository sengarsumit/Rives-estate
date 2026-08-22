package com.example.estate.Rives.estate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Deliberately its own @Configuration class rather than on
// @SpringBootApplication: @WebMvcTest slices locate the main application
// class as their base config, so @EnableJpaAuditing there gets processed
// even though those slices never bootstrap JPA - failing with
// "JPA metamodel must not be empty". A separate config class isn't swept
// into that restricted component scan.
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
