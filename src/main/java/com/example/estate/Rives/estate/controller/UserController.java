package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.UpdateUserDTO;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/")
public class UserController {

    @Autowired
    private UserService userService;

    @PreAuthorize("hasAnyRole('USER','DEALER','ADMIN')")
    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username){
        if(userService.isUsernameExist(username)) {
            User user=userService.getUserByUsername(username);
            userService.deleteUser(user);
            return ResponseEntity.ok().body("user has been deleted");
        }
        return ResponseEntity.badRequest().body("User does not exist");
    }
    @PreAuthorize("hasAnyRole('USER','DEALER','ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }



}
