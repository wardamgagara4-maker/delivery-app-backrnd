package com.warda.delivery_app.controller;

import com.warda.delivery_app.entity.Delivery;
import com.warda.delivery_app.entity.Payment;
import com.warda.delivery_app.entity.User;
import com.warda.delivery_app.repository.DeliveryRepository;
import com.warda.delivery_app.repository.PaymentRepository;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
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

    private boolean ownsDelivery(User user, Delivery delivery) {
        return user != null
                && delivery != null
                && delivery.getDeliveryRequest() != null
                && delivery.getDeliveryRequest().getCustomer() != null
                && delivery.getDeliveryRequest().getCustomer().getEmail().equals(user.getEmail());
    }

    private boolean ownsPayment(User user, Payment payment) {
        return ownsDelivery(user, payment.getDelivery());
    }

    // ----------------------------------------------------------------------

    // Get All Payments (admin only)
    @GetMapping
    public ResponseEntity<?> getAllPayments() {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can view all payments");
        }

        return ResponseEntity.ok(paymentRepository.findAll());
    }

    // Create Payment — only for a delivery the current customer owns (or an admin)
    @PostMapping
    public ResponseEntity<?> savePayment(
            @Valid @RequestBody Payment payment) {

        User user = currentUser();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in to make a payment");
        }

        if (payment.getDelivery() == null || payment.getDelivery().getId() == null) {
            return ResponseEntity.badRequest().body("A delivery must be specified");
        }

        Delivery delivery = deliveryRepository.findById(payment.getDelivery().getId())
                .orElse(null);

        if (delivery == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Delivery not found");
        }

        if (!isAdmin(user) && !ownsDelivery(user, delivery)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only pay for your own deliveries");
        }

        payment.setDelivery(delivery);

        Payment savedPayment = paymentRepository.save(payment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedPayment);
    }

    // Update Payment (the owning customer, or an admin)
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePayment(
            @PathVariable Long id,
            @Valid @RequestBody Payment payment) {

        Payment existing = paymentRepository.findById(id)
                .orElse(null);

        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Payment not found");
        }

        User user = currentUser();

        if (!isAdmin(user) && !ownsPayment(user, existing)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have permission to edit this payment");
        }

        existing.setAmount(payment.getAmount());
        existing.setPaymentMethod(payment.getPaymentMethod());
        existing.setPaymentStatus(payment.getPaymentStatus());

        Payment updatedPayment = paymentRepository.save(existing);

        return ResponseEntity.ok(updatedPayment);
    }

    // Delete Payment (admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can delete payments");
        }

        if (!paymentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Payment not found");
        }

        paymentRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
