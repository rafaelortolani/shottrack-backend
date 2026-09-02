package com.shottrack.backend.application.user.mapper;

import com.shottrack.backend.application.user.dto.UserResponse;
import com.shottrack.backend.application.user.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
