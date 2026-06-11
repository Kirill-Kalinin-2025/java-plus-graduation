package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user.dto.UserDto;
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

    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    UserDto dto = new UserDto();
                    dto.setId(user.getId());
                    dto.setName(user.getName());
                    dto.setEmail(user.getEmail());
                    return dto;
                })
                .orElse(null);
    }
}