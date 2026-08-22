package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.UserLoginDTO;
import com.example.estate.Rives.estate.DTO.UserRegisterDTO;
import com.example.estate.Rives.estate.DTO.UserResponseDTO;
import com.example.estate.Rives.estate.DTO.config.UserMapper;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.exception.ApiException;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.security.JwtUtil;
import com.example.estate.Rives.estate.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
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
    @Autowired
    UserMapper userMapper;
    @Autowired
    RefreshTokenService refreshTokenService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody UserLoginDTO userLoginDTO, HttpServletResponse httpServletResponse){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userLoginDTO.getUsername(), userLoginDTO.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User dbUser = userRepository.findByUsername(userDetails.getUsername());

        if (dbUser == null) {
            throw new UsernameNotFoundException("User not found");
        }
        String accessToken= jwtUtil.generateAccessToken(dbUser.getUsername(), dbUser.getRole().toString());
        String refreshToken= jwtUtil.generateRefreshToken(dbUser.getUsername(), dbUser.getRole().toString());
        refreshTokenService.issue(dbUser, refreshToken);

        setAuthCookies(httpServletResponse, accessToken, refreshToken);
        return ResponseEntity.ok("Login successful");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");

        if (refreshToken == null || !jwtUtil.validateJwtToken(refreshToken)) {
            clearAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        User user;
        try {
            // Validates against the persisted record (not just the JWT's own
            // signature/expiry) and rotates it - the presented token is marked
            // used, and reuse of it afterward revokes every session for this
            // user (see RefreshTokenServiceImpl).
            user = refreshTokenService.rotate(refreshToken);
        } catch (ApiException e) {
            clearAuthCookies(response);
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole().toString());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getRole().toString());
        refreshTokenService.issue(user, newRefreshToken);

        setAuthCookies(response, newAccessToken, newRefreshToken);
        return ResponseEntity.ok("access token refreshed successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
        clearAuthCookies(response);
        return ResponseEntity.ok("Logged out successfully");
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeMillis) {
        // SameSite=None: frontend (Vercel) and backend (Render) are different
        // sites, so the auth cookie must be sent on cross-site requests. This
        // requires Secure, which is already set.
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAgeMillis / 1000)
                .build();
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie("accessToken", accessToken, JwtUtil.ACCESS_TOKEN_EXPIRY_MS).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie("refreshToken", refreshToken, JwtUtil.REFRESH_TOKEN_EXPIRY_MS).toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("accessToken", "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("refreshToken", "", 0).toString());
    }

    @PostMapping("/signup")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        if (userRepository.existsByUsername(userRegisterDTO.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already in use");
        }
        if (userRepository.existsByEmail(userRegisterDTO.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already in use");
        }
        if(userRegisterDTO.getRole()==Role.ADMIN){
            throw new ApiException(HttpStatus.FORBIDDEN, "cannot register as ADMIN");
        }
        User newUser=userMapper.dtoToUser(userRegisterDTO);
        newUser.setPassword(encoder.encode(userRegisterDTO.getPassword()));
        newUser.setRole(userRegisterDTO.getRole()== null ?Role.USER:userRegisterDTO.getRole());

        userRepository.save(newUser);
        return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);

    }

    @PreAuthorize("hasAnyRole('USER','DEALER','ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal User loggedInUser) {
        return ResponseEntity.ok(userMapper.userToDto(loggedInUser));
    }
}
