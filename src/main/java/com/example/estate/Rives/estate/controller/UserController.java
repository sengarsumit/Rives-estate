package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.UpdateUserDTO;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @PreAuthorize("hasAnyRole('USER','DEALER','ADMIN')")
    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username, @AuthenticationPrincipal User loggedInUser) {
        if (!isSelfOrAdmin(loggedInUser, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own account");
        }
        if (!userService.isUsernameExist(username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User does not exist");
        }
        User user = userService.getUserByUsername(username);
        userService.deleteUser(user);
        return ResponseEntity.ok().body("user has been deleted");
    }

    @PreAuthorize("hasAnyRole('USER','DEALER','ADMIN')")
    @PatchMapping("/{username}")
    public ResponseEntity<?> updateUser(
            @PathVariable String username,
            @RequestBody UpdateUserDTO updateDTO,
            @AuthenticationPrincipal User loggedInUser
    ) {
        if (!isSelfOrAdmin(loggedInUser, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only update your own account");
        }

        User user = userService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User does not exist");
        }

        if (updateDTO.getFirstName() != null) {
            user.setFirstName(updateDTO.getFirstName());
        }
        if (updateDTO.getLastName() != null) {
            user.setLastName(updateDTO.getLastName());
        }
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(updateDTO.getPassword()));
        }
        if (updateDTO.getRole() != null) {
            // Role is a privilege decision, never trust it from the target user's own
            // request — only an ADMIN may change another account's role.
            if (loggedInUser.getRole() == Role.ADMIN) {
                user.setRole(updateDTO.getRole());
            }
        }

        userService.updateUser(user);
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    private boolean isSelfOrAdmin(User loggedInUser, String username) {
        return loggedInUser.getUsername().equals(username) || loggedInUser.getRole() == Role.ADMIN;
    }
}
