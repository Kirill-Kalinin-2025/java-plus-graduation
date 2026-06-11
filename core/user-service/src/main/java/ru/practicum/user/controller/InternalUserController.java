package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user.repository.UserRepository;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}/exists")
    public Boolean existsById(@PathVariable Long userId) {
        return userRepository.existsById(userId);
    }

    @GetMapping("/{userId}/name")
    public String getUserName(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getName())
                .orElse(null);
    }
}