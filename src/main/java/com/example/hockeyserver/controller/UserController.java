package com.example.hockeyserver.controller;

import com.example.hockeyserver.dto.CurrentUserResponse;
import com.example.hockeyserver.security.AuthenticatedUser;
import com.example.hockeyserver.entity.User;
import com.example.hockeyserver.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        User currentUser = userService.findById(user.id());
        return new CurrentUserResponse(currentUser);
    }
}
