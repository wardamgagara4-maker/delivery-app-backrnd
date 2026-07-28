package com.warda.delivery_app.controller;

import com.warda.delivery_app.entity.User;
import com.warda.delivery_app.repository.UserRepository;
import com.warda.delivery_app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    // --- Helpers ---------------------------------------------------------

    private User currentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equals(user.getRole());
    }

    // ----------------------------------------------------------------------

    // Register User (public — no login required yet)
    @PostMapping
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        try {
            User savedUser = userService.saveUser(user);
            return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get All Users (admin only)
    @GetMapping
    public ResponseEntity<?> getAllUsers() {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can view all users");
        }

        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Get All Drivers (used by the admin "assign driver" dropdown)
    @GetMapping("/drivers")
    public ResponseEntity<?> getAllDrivers() {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can view the driver list");
        }

        return ResponseEntity.ok(userService.getUsersByRole("DRIVER"));
    }

    // Get User By ID (the user themselves, or an admin)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {

        User requester = currentUser();

        if (requester == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in");
        }

        if (!isAdmin(requester) && !requester.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only view your own profile");
        }

        try {
            return ResponseEntity.ok(userService.getUserById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // Update User (the user themselves, or an admin)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User updatedUser) {

        User requester = currentUser();

        if (requester == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in");
        }

        if (!isAdmin(requester) && !requester.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only edit your own profile");
        }

        try {
            User user = userService.updateUser(id, updatedUser);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // Delete User (the user themselves, or an admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {

        User requester = currentUser();

        if (requester == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in");
        }

        if (!isAdmin(requester) && !requester.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only delete your own account");
        }

        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
}
