package com.example.dating.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.backend.support.PostgisIntegrationTest;

@SpringBootTest
@Transactional
class DatingMiniAppApplicationTests extends PostgisIntegrationTest {
    @Test
    void contextLoads() {}
}
