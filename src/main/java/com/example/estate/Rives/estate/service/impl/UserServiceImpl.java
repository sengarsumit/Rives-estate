package com.example.estate.Rives.estate.service.impl;

import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;



    @Override
    public User saveUser(User user) {

        return userRepository.save(user);
    }

    @Override
    public User getUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        return user;
    }

    @Override
    public void updateUser(User user) {
        if (user.getUsername() != null && userRepository.existsByUsername(user.getUsername())) {
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found to update");
        }
    }

    @Override
    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    @Override
    public boolean isUsernameExist(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean isEmailExist(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
