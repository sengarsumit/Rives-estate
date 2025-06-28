package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.UpdateUserDTO;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody User user){
        if(userService.isUsernameExist(user.getUsername())) {
            return ResponseEntity.badRequest().body("username already exist");
        }
        else if(userService.isEmailExist(user.getEmail())) {
            return ResponseEntity.badRequest().body("email already exist");
        }
        user.setRole(Role.USER);
        User user1=userService.saveUser(user);
        return ResponseEntity.ok(user1);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username){
        if(userService.isUsernameExist(username)) {
            User user=userService.getUserByUsername(username);
            userService.deleteUser(user);
            return ResponseEntity.ok().body("user has been deleted");
        }
        return ResponseEntity.badRequest().body("User does not exist");
    }

    @PatchMapping("/{username}")
    public ResponseEntity<User> updateUser(
            @PathVariable String username,
            @RequestBody UpdateUserDTO updateDTO
    ) {
        User user = userService.getUserByUsername(username);

        if (updateDTO.getFirstName() != null) {
            user.setFirstName(updateDTO.getFirstName());
        }
        if(updateDTO.getLastName() != null) {
            user.setLastName(updateDTO.getLastName());
        }
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getPassword() != null) {
            user.setPassword(updateDTO.getPassword()); // consider encoding it
        }
        if (updateDTO.getRole() != null) {
            user.setRole(updateDTO.getRole());
        }

        userService.updateUser(user);
        return ResponseEntity.ok(user);
    }



}
