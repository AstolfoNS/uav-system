package com.tf.backend.core.config;


import com.tf.backend.core.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@EnableMethodSecurity
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                // 禁用CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // 启动CORS跨域认证
                .cors(Customizer.withDefaults())
                // 配置 session 策略为“无状态”
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 禁用表单登录
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用 httpBasic 认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // 配置请求权限规则
                .authorizeHttpRequests(auth -> auth
                        // 公共开放的接口：/actuator/**，/public/**
                        .requestMatchers("/actuator/**", "/public/**", "/swagger-ui/**", "/auth/**")
                        .permitAll()
                        // 其他所有请求通通走认证
                        .anyRequest()
                        .authenticated()
                )
                // 提前解析 token 并注入用户信息
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
