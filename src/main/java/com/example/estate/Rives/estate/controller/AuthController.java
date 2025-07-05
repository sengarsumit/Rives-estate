package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    PasswordEncoder encoder;

    @PostMapping("/signin")
    public String authenticateUser(@RequestBody User user)
    {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return jwtUtil.generateToken(userDetails.getUsername());
    }

    @PostMapping("/signup")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return new ResponseEntity<>("Username is already in use", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            return new ResponseEntity<>("Email is already in use", HttpStatus.CONFLICT);
        }
        if(user.getRole()==Role.ADMIN){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("cannot register as ADMIN");
        }
        User newUser=new User();
        newUser.setUsername(user.getUsername());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(encoder.encode(user.getPassword()));
        newUser.setRole(user.getRole());

        userRepository.save(newUser);
        return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);

    }
}
