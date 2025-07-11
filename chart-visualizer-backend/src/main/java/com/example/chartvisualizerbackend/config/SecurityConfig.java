// src/main/java/com/example/chartvisualizerbackend/config/SecurityConfig.java
package com.example.chartvisualizerbackend.config;

// import com.example.chartvisualizerbackend.filter.JwtAuthFilter; // <-- BU IMPORT'U KALDIRDIK
import com.example.chartvisualizerbackend.filter.JwtAuthFilter;
import com.example.chartvisualizerbackend.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // @Autowired private JwtAuthFilter authFilter; // <-- BU SATIRI KALDIRDIK

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new CustomUserDetailsService(passwordEncoder);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // filterChain metoduna JwtAuthFilter'ı parametre olarak enjekte ettik.
    // Spring, bu bean'i kendi otomatik olarak oluşturacaktır.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter authFilter) throws Exception {
        http
                .csrf().disable()
                // CORS ayarını ekle
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
                    corsConfiguration.setAllowedOriginPatterns(List.of("http://localhost:3000")); // FRONTEND URL'İNİZ!
                    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setAllowCredentials(true); // Kimlik bilgileri (JWT) gönderilmesine izin ver
                    return corsConfiguration;
                }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService(passwordEncoder()));
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}