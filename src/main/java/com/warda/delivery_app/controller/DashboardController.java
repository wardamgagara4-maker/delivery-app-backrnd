package com.warda.delivery_app.controller;

import com.warda.delivery_app.entity.Delivery;
import com.warda.delivery_app.entity.DeliveryRequest;
import com.warda.delivery_app.entity.Payment;
import com.warda.delivery_app.entity.User;
import com.warda.delivery_app.repository.DeliveryRepository;
import com.warda.delivery_app.repository.DeliveryRequestRepository;
import com.warda.delivery_app.repository.PaymentRepository;
import com.warda.delivery_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final DeliveryRequestRepository deliveryRequestRepository;
    private final DeliveryRepository deliveryRepository;
    private final PaymentRepository paymentRepository;

    // --- Helpers ---------------------------------------------------------

    private boolean isCurrentUserAdmin() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return false;
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);

        return user != null && "ADMIN".equals(user.getRole());
    }

    // ----------------------------------------------------------------------
    // All dashboard stats are admin-only — they summarize every customer's,
    // driver's and payment's data across the whole platform.

    @GetMapping
    public ResponseEntity<?> getDashboardData() {

        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can view dashboard stats");
        }

        Map<String, Long> data = new HashMap<>();

        data.put("users", userRepository.count());
        data.put("requests", deliveryRequestRepository.count());
        data.put("deliveries", deliveryRepository.count());
        data.put("payments", paymentRepository.count());

        return ResponseEntity.ok(data);
    }

    @GetMapping("/recent-requests")
    public ResponseEntity<?> getRecentRequests() {

        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can view recent requests");
        }

        return ResponseEntity.ok(
                deliveryRequestRepository.findTop5ByOrderByIdDesc()
        );
    }

    @GetMapping("/recent-deliveries")
    public ResponseEntity<?> getRecentDeliveries() {

        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can view recent deliveries");
        }

        return ResponseEntity.ok(
                deliveryRepository.findTop5ByOrderByIdDesc()
        );
    }

    @GetMapping("/recent-payments")
    public ResponseEntity<?> getRecentPayments() {

        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can view recent payments");
        }

        return ResponseEntity.ok(
                paymentRepository.findTop5ByOrderByIdDesc()
        );
    }
}
