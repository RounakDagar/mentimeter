package com.example.mentimeter.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF protection for stateless APIs (like yours)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Define authorization rules
                .authorizeHttpRequests(auth -> auth
                        // 3. Allow anyone to access the registration and login endpoints
                        .requestMatchers("/auth/**").permitAll()

                        // 4. Require authentication for any other request
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}