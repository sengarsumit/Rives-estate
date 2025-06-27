package com.example.estate.Rives.estate.service;

import com.example.estate.Rives.estate.model.User;

import java.util.List;

public interface UserService {

    User saveUser(User user);
    User getUserByUsername(String username);
    void updateUser(User user);
    void deleteUser(User user);
    boolean isUsernameExist(String username);
    boolean isEmailExist(String email);
    List<User> getAllUsers();

}
