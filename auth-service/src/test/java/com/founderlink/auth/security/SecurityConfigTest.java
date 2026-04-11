package com.founderlink.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;
import com.founderlink.auth.config.JwtProperties;

@SpringBootTest(classes = SecurityConfigTest.TestApp.class)
class SecurityConfigTest {

    @Autowired
    private SecurityFilterChain filterChain;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private PasswordEncoder encoder;

    @Test
    void contextLoadsBeansSuccessfully() {
        assertThat(filterChain).isNotNull();
        assertThat(authManager).isNotNull();
        assertThat(encoder).isNotNull();
    }

    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @Import({SecurityConfig.class, CustomUserDetailsService.class, JwtService.class, JwtProperties.class, JwtAuthFilter.class})
    static class TestApp {
        @org.springframework.context.annotation.Bean
        public com.founderlink.auth.repository.UserRepository userRepository() {
            return org.mockito.Mockito.mock(com.founderlink.auth.repository.UserRepository.class);
        }
        @org.springframework.context.annotation.Bean
        public java.time.Clock clock() {
            return java.time.Clock.systemUTC();
        }
    }
}
