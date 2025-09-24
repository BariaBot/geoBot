package com.example.dating.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dating.backend.support.PostgisIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTest extends PostgisIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void healthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
               .andExpect(status().isOk());
    }
}
