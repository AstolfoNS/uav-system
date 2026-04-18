package com.tf.backend.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@SpringBootTest
public class PasswordTests {

    @Autowired
    private PasswordEncoder passwordEncoder;


    public void getPassword(String password) {
        log.info("password: {}, encoded password: {}", password, passwordEncoder.encode(password));
    }

    @Test
    public void test() {
        getPassword("123456");
    }

}
