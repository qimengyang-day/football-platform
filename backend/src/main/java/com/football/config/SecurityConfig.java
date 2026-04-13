package com.football.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()

                // 公开展示类接口：未登录也可访问（前端首屏/列表/详情）
                .antMatchers(HttpMethod.GET, "/api/match/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/player/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/club/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/news/list").permitAll()
                .antMatchers(HttpMethod.GET, "/api/fan/news/list").permitAll()
                .antMatchers(HttpMethod.GET, "/api/public/contact").permitAll()
                .antMatchers(HttpMethod.GET, "/api/data/player/radar/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/fan/follow/**").hasRole("FAN")
                .antMatchers(HttpMethod.GET, "/api/fan/comment/hot").permitAll()
                .antMatchers(HttpMethod.GET, "/api/fan/match/**").permitAll()

                // 写操作按角色控制
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/api/player/**").hasRole("PLAYER")
                .antMatchers(HttpMethod.PUT, "/api/player/**").hasRole("PLAYER")
                .antMatchers(HttpMethod.POST, "/api/fan/**").hasRole("FAN")
                .antMatchers(HttpMethod.PUT, "/api/fan/**").hasRole("FAN")
                .antMatchers(HttpMethod.DELETE, "/api/fan/**").hasRole("FAN")
                .antMatchers(HttpMethod.PUT, "/api/user/profile").authenticated()
                .antMatchers(HttpMethod.PUT, "/api/user/password").authenticated()
                .antMatchers(HttpMethod.POST, "/api/club/**").hasRole("CLUB")
                .antMatchers(HttpMethod.PUT, "/api/club/**").hasRole("CLUB")

                // 仅管理员可创建俱乐部（配合“俱乐部不能自助注册”）
                .antMatchers(HttpMethod.POST, "/api/club/team").hasRole("ADMIN")
                .anyRequest().authenticated();

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}