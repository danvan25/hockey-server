package com.example.hockeyserver.controller;

import com.example.hockeyserver.dto.CurrentUserResponse;
import com.example.hockeyserver.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return new CurrentUserResponse(user);
    }
}
