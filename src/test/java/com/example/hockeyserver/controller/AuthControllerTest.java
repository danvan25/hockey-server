package com.example.hockeyserver.controller;

import com.example.hockeyserver.config.SecurityConfig;
import com.example.hockeyserver.dto.RegisterRequest;
import com.example.hockeyserver.dto.RegisterResponse;
import com.example.hockeyserver.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.example.hockeyserver.exception.EmailAlreadyExistsException;
import com.example.hockeyserver.exception.GlobalExceptionHandler;
import com.example.hockeyserver.exception.UsernameAlreadyExistsException;


@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class,
        GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void registerShouldReturnCreatedUser() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Daniel",
                "daniel@example.com",
                "secret-password"
        );

        RegisterResponse response = new RegisterResponse(
                1L,
                "Daniel",
                "daniel@example.com"
        );

        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username")
                        .value("Daniel"))
                .andExpect(jsonPath("$.email")
                        .value("daniel@example.com"));

        verify(userService)
                .register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldRejectInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Daniel",
                "not-an-email",
                "secret-password"
        );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.email")
                        .value("Email address is invalid"));

        verify(userService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldRejectShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Daniel",
                "daniel@example.com",
                "short"
        );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest());

        verify(userService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldRejectBlankUsername() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",
                "daniel@example.com",
                "secret-password"
        );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest());

        verify(userService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldReturnConflictForExistingUsername()
            throws Exception {

        RegisterRequest request = new RegisterRequest(
                "Daniel",
                "daniel@example.com",
                "secret-password"
        );

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(
                        new UsernameAlreadyExistsException("Daniel")
                );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Username is already in use: Daniel"
                        ))
                .andExpect(jsonPath("$.path")
                        .value("/api/auth/register"));
    }

    @Test
    void registerShouldReturnConflictForExistingEmail()
            throws Exception {

        RegisterRequest request = new RegisterRequest(
                "Daniel",
                "daniel@example.com",
                "secret-password"
        );

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(
                        new EmailAlreadyExistsException(
                                "daniel@example.com"
                        )
                );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Email address is already in use: "
                                        + "daniel@example.com"
                        ))
                .andExpect(jsonPath("$.path")
                        .value("/api/auth/register"));
    }


}