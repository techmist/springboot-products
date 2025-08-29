package com.example.demo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
class SecurityConfig {
    
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(
                        AntPathRequestMatcher("/h2-console/**"),
                        AntPathRequestMatcher("/api/**"),
                        AntPathRequestMatcher("/products/**"),
                        AntPathRequestMatcher("/fetch")
                    )
            }
            .headers { headers ->
                headers.frameOptions { frameOptions ->
                    frameOptions.sameOrigin()
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/**",
                        "/webjars/**",
                        "/h2-console/**",
                        "/css/**",
                        "/js/**",
                        "/favicon.ico"
                    ).permitAll()
                    .anyRequest().permitAll()
            }
            
        return http.build()
    }
}
