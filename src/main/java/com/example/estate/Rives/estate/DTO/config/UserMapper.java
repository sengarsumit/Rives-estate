package com.example.estate.Rives.estate.DTO.config;

import com.example.estate.Rives.estate.DTO.UserLoginDTO;
import com.example.estate.Rives.estate.DTO.UserRegisterDTO;
import com.example.estate.Rives.estate.DTO.UserResponseDTO;
import com.example.estate.Rives.estate.model.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    @Autowired
    private ModelMapper modelMapper;

    public User dtoToUser(UserRegisterDTO userRegisterDTO) {
        User user=modelMapper.map(userRegisterDTO,User.class);
        return user;
    }

    public UserResponseDTO userToDto(User user)
    {
        UserResponseDTO userResponseDTO=modelMapper.map(user,UserResponseDTO.class);
        return userResponseDTO;
    }


}
