package ru.practicum.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/users/{userId}/exists")
    Boolean existsById(@PathVariable Long userId);

    @GetMapping("/internal/users/{userId}/name")
    String getUserName(@PathVariable Long userId);
}