package com.warda.delivery_app.controller;

import com.warda.delivery_app.entity.DeliveryRequest;
import com.warda.delivery_app.entity.PackageItem;
import com.warda.delivery_app.entity.User;
import com.warda.delivery_app.repository.DeliveryRequestRepository;
import com.warda.delivery_app.repository.PackageItemRepository;
import com.warda.delivery_app.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageItemController {

    private final PackageItemRepository packageRepository;
    private final DeliveryRequestRepository deliveryRequestRepository;
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

    private boolean ownsRequest(User user, DeliveryRequest request) {
        return user != null
                && request.getCustomer() != null
                && request.getCustomer().getEmail().equals(user.getEmail());
    }

    // ----------------------------------------------------------------------

    // Get All Packages (admin only)
    @GetMapping
    public ResponseEntity<?> getAllPackages() {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can view all packages");
        }

        return ResponseEntity.ok(packageRepository.findAll());
    }

    // Add a package to a delivery request — only the owning customer (or an admin)
    @PostMapping("/{requestId}")
    public ResponseEntity<?> savePackage(
            @PathVariable Long requestId,
            @Valid @RequestBody PackageItem packageItem) {

        DeliveryRequest request = deliveryRequestRepository.findById(requestId)
                .orElse(null);

        if (request == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Delivery request not found");
        }

        User user = currentUser();

        if (!isAdmin(user) && !ownsRequest(user, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only add a package to your own delivery request");
        }

        packageItem.setDeliveryRequest(request);

        PackageItem savedPackage = packageRepository.save(packageItem);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedPackage);
    }
}
