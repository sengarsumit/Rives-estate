package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    public ResponseEntity<?> authenticateUser(@RequestBody User user, HttpServletResponse httpServletResponse){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User dbUser = userRepository.findByUsername(userDetails.getUsername());

        if (dbUser == null) {
            throw new UsernameNotFoundException("User not found");
        }
        String accessToken= jwtUtil.generateAccessToken(dbUser.getUsername(), dbUser.getRole().toString());
        String refreshToken= jwtUtil.generateRefreshToken(dbUser.getUsername(), dbUser.getRole().toString());

        ResponseCookie accessCookie=ResponseCookie.from("accessToken",accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(15*60)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken",refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(7*24*60*60)
                .build();

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok("Login successful");
    }
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response)
    {
        String refreshToken=null;

        if(request.getCookies()!=null)
        {
            for(Cookie cookie:request.getCookies())
            {
                if("refreshToken".equals(cookie.getName()))
                {
                    refreshToken=cookie.getValue();
                }
            }
        }
        if(refreshToken!=null && jwtUtil.validateJwtToken(refreshToken))
        {
            String username=jwtUtil.getUsernameFromToken(refreshToken);
            User user=userRepository.findByUsername(username);

            if(user==null)
            {
                throw new UsernameNotFoundException("User not found");
            }
            String newAccessToken= jwtUtil.generateAccessToken(user.getUsername(), user.getRole().toString());

            ResponseCookie newAccessCookie=ResponseCookie.from("accessToken",newAccessToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(15*60)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, newAccessCookie.toString());
            return  ResponseEntity.ok("access token refreshed successfully");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid  refresh token");
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response)
    {
        ResponseCookie deleteAccess=ResponseCookie.from("accessToken","")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie deleteRefresh=ResponseCookie.from("refreshToken","")
                .httpOnly(true)
                .secure(true)
                .sameSite("LAx")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefresh.toString());
        return ResponseEntity.ok("Logged out successfully");
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
