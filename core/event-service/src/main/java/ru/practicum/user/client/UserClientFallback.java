package ru.practicum.user.client;

import org.springframework.stereotype.Component;
import ru.practicum.user.dto.UserDto;

@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserDto getUser(Long userId) {
        return null;
    }

    @Override
    public Boolean existsById(Long userId) {
        return false;
    }
}