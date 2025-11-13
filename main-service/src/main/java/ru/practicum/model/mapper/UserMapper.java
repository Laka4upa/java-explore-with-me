package ru.practicum.model.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.model.dto.NewUserRequest;
import ru.practicum.model.dto.UserDto;
import ru.practicum.model.dto.UserShortDto;
import ru.practicum.model.entity.User;

@Component
public class UserMapper {

    public User toEntity(NewUserRequest userRequest) {
        return User.builder()
                .name(userRequest.getName())
                .email(userRequest.getEmail())
                .build();
    }

    public UserDto toDto(User user) {
        if (user == null) return null;

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public UserShortDto toShortDto(User user) {
        if (user == null) return null;

        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}