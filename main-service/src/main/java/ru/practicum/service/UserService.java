package ru.practicum.service;

import ru.practicum.model.dto.NewUserRequest;
import ru.practicum.model.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest userRequest);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long userId);
}