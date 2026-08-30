package com.example.hockeyserver.service;

import com.example.hockeyserver.dto.RegisterRequest;
import com.example.hockeyserver.dto.RegisterResponse;
import com.example.hockeyserver.entity.User;
import com.example.hockeyserver.exception.EmailAlreadyExistsException;
import com.example.hockeyserver.exception.UsernameAlreadyExistsException;
import com.example.hockeyserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import com.example.hockeyserver.dto.LoginRequest;
import com.example.hockeyserver.dto.LoginResponse;
import com.example.hockeyserver.entity.Role;
import com.example.hockeyserver.exception.InvalidCredentialsException;
import com.example.hockeyserver.security.JwtService;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void registerShouldSaveNewUser() {
        RegisterRequest request = new RegisterRequest(
                "Daniel",
                "Daniel@Example.com",
                "secret-password"
        );

        when(userRepository.existsByUsername("Daniel"))
                .thenReturn(false);

        when(userRepository.existsByEmail("daniel@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("secret-password"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        RegisterResponse response =
                userService.register(request);

        assertEquals("Daniel", response.getUsername());
        assertEquals(
                "daniel@example.com",
                response.getEmail()
        );

        verify(passwordEncoder)
                .encode("secret-password");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(Role.USER, savedUser.getRole());
    }

    @Test
    void registerShouldRejectExistingUsername() {
        RegisterRequest request = new RegisterRequest(
                "Daniel",
                "daniel@example.com",
                "secret-password"
        );

        when(userRepository.existsByUsername("Daniel")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> userService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerShouldRejectExistingEmail() {
        RegisterRequest request = new RegisterRequest(
                "Daniel",
                "daniel@example.com",
                "secret-password"
        );

        when(userRepository.existsByUsername("Daniel")).thenReturn(false);
        when(userRepository.existsByEmail("daniel@example.com")).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginShouldReturnUserForValidCredentials() {
        LoginRequest request = new LoginRequest(
                "Daniel",
                "secret-password"
        );

        User user = new User(
                "Daniel",
                "daniel@example.com",
                "encoded-password"
        );

        user.setRole(Role.USER);

        when(userRepository.findByUsername("Daniel"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "secret-password",
                "encoded-password"
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("signed-jwt-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(900L);

        LoginResponse response =
                userService.login(request);

        assertEquals("Daniel", response.getUsername());
        assertEquals(
                "daniel@example.com",
                response.getEmail()
        );
        assertEquals(Role.USER, response.getRole());
        assertEquals(
                "signed-jwt-token",
                response.getAccessToken()
        );
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());

        verify(userRepository)
                .findByUsername("Daniel");

        verify(passwordEncoder)
                .matches(
                        "secret-password",
                        "encoded-password"
                );

        verify(jwtService).generateToken(user);
    }

    @Test
    void loginShouldRejectUnknownUsername() {
        LoginRequest request = new LoginRequest(
                "UnknownPlayer",
                "secret-password"
        );

        when(userRepository.findByUsername("UnknownPlayer"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );

        verify(userRepository)
                .findByUsername("UnknownPlayer");

        verify(passwordEncoder, never())
                .matches(any(), any());
    }

    @Test
    void loginShouldRejectInvalidPassword() {
        LoginRequest request = new LoginRequest(
                "Daniel",
                "wrong-password"
        );

        User user = new User(
                "Daniel",
                "daniel@example.com",
                "encoded-password"
        );

        user.setRole(Role.USER);

        when(userRepository.findByUsername("Daniel"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );

        verify(passwordEncoder)
                .matches(
                        "wrong-password",
                        "encoded-password"
                );
    }
}
