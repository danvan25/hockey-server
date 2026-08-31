package com.example.hockeyserver.controller;

import com.example.hockeyserver.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.example.hockeyserver.security.JwtAuthenticationEntryPoint;
import com.example.hockeyserver.security.JwtAuthenticationFilter;
import com.example.hockeyserver.security.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class
})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void healthShouldReturnServerStatus() throws Exception {
        mockMvc.perform(
                        get("/api/health")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("UP"))
                .andExpect(jsonPath("$.service")
                        .value("Hockey Server"));
    }
}
