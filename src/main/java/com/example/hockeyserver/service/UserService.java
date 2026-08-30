package com.example.hockeyserver.service;

import com.example.hockeyserver.dto.RegisterRequest;
import com.example.hockeyserver.dto.RegisterResponse;
import com.example.hockeyserver.entity.User;
import com.example.hockeyserver.exception.EmailAlreadyExistsException;
import com.example.hockeyserver.exception.UsernameAlreadyExistsException;
import com.example.hockeyserver.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.hockeyserver.entity.Role;
import com.example.hockeyserver.dto.LoginResponse;
import com.example.hockeyserver.dto.LoginRequest;
import com.example.hockeyserver.exception.InvalidCredentialsException;
import com.example.hockeyserver.security.JwtService;

@Service
public class UserService {

    /*
    * adatok normalizálása
      egyediség ellenőrzése
      jelszó hash-elése
      User létrehozása
      mentés
      biztonságos válasz készítése
    * */

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public RegisterResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        String passwordHash =
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                username,
                email,
                passwordHash
        );

        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail()
        );
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateToken(user);

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                accessToken,
                jwtService.getExpirationSeconds()
        );
    }
}
