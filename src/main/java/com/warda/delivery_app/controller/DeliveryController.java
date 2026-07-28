package com.warda.delivery_app.controller;

import com.warda.delivery_app.entity.Delivery;
import com.warda.delivery_app.entity.DeliveryRequest;
import com.warda.delivery_app.entity.User;
import com.warda.delivery_app.dto.LocationUpdateRequest;
import com.warda.delivery_app.repository.DeliveryRepository;
import com.warda.delivery_app.repository.DeliveryRequestRepository;
import com.warda.delivery_app.repository.UserRepository;
import com.warda.delivery_app.websocket.DeliveryTrackingWebSocketHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRequestRepository deliveryRequestRepository;
    private final UserRepository userRepository;
    private final DeliveryTrackingWebSocketHandler deliveryTrackingWebSocketHandler;

    // --- Helpers ---------------------------------------------------------

    private User currentUser() {

        String email = currentUserEmail();

        if (email == null) {
            return null;
        }

        return userRepository.findByEmail(email).orElse(null);
    }

    private String currentUserEmail() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        return authentication.getName();
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equals(user.getRole());
    }

    private boolean isAssignedDriver(User user, Delivery delivery) {
        return user != null
                && "DRIVER".equals(user.getRole())
                && delivery.getDriverName() != null
                && delivery.getDriverName().equals(user.getFullName());
    }

    private boolean isOwningCustomer(User user, Delivery delivery) {
        return user != null
                && delivery.getDeliveryRequest() != null
                && delivery.getDeliveryRequest().getCustomer() != null
                && delivery.getDeliveryRequest().getCustomer().getEmail().equals(user.getEmail());
    }

    // ----------------------------------------------------------------------

    // Admin only — not currently used by the frontend, kept for admin tooling
    @GetMapping
    public ResponseEntity<?> getAllDeliveries() {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can view all deliveries");
        }

        return ResponseEntity.ok(deliveryRepository.findAll());
    }

    // Single delivery, used by the live tracking map (polled periodically).
    // Accessible to the assigned driver, the owning customer, or an admin.
    @GetMapping("/{id}")
    public ResponseEntity<?> getDeliveryById(@PathVariable Long id) {

        Delivery delivery = deliveryRepository.findById(id).orElse(null);

        if (delivery == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Delivery not found");
        }

        User user = currentUser();

        if (!isAdmin(user) && !isAssignedDriver(user, delivery) && !isOwningCustomer(user, delivery)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have access to this delivery");
        }

        return ResponseEntity.ok(delivery);
    }

    // The driver's device pushes its current GPS position here while
    // the delivery is in progress, so the customer's live map can follow it.
    // Only the driver assigned to this exact delivery may update it.
    @PutMapping("/{id}/location")
    public ResponseEntity<?> updateDriverLocation(
            @PathVariable Long id,
            @RequestBody LocationUpdateRequest location) {

        Delivery delivery = deliveryRepository.findById(id).orElse(null);

        if (delivery == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Delivery not found");
        }

        User user = currentUser();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in to share your location");
        }

        if (!isAssignedDriver(user, delivery)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only the assigned driver can update this delivery's location");
        }

        delivery.setDriverLatitude(location.getLatitude());
        delivery.setDriverLongitude(location.getLongitude());

        Delivery updated = deliveryRepository.save(delivery);

        deliveryTrackingWebSocketHandler.broadcast(id, updated);

        return ResponseEntity.ok(updated);
    }

    // Deliveries belonging to the logged-in customer's own requests
    @GetMapping("/my-deliveries")
    public ResponseEntity<?> getMyDeliveries() {

        String email = currentUserEmail();

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in to view your deliveries");
        }

        return ResponseEntity.ok(
                deliveryRepository.findByDeliveryRequest_Customer_EmailOrderByIdDesc(email)
        );
    }

    // Deliveries assigned to the logged-in driver
    @GetMapping("/my-assigned")
    public ResponseEntity<?> getMyAssignedDeliveries() {

        User driver = currentUser();

        if (driver == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in to view your assigned deliveries");
        }

        return ResponseEntity.ok(
                deliveryRepository.findByDriverNameOrderByIdDesc(driver.getFullName())
        );
    }

    // Admin only — deliveries are normally created via the assign-driver flow
    @PostMapping
    public ResponseEntity<?> createDelivery(
            @Valid @RequestBody Delivery delivery) {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can create deliveries directly");
        }

        if (delivery.getDeliveryStatus() == null ||
                delivery.getDeliveryStatus().isBlank()) {

            delivery.setDeliveryStatus("Assigned");
        }

        Delivery savedDelivery = deliveryRepository.save(delivery);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedDelivery);
    }

    // Only the assigned driver (updating status) or an admin may update a delivery
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDelivery(
            @PathVariable Long id,
            @Valid @RequestBody Delivery delivery) {

        Delivery existing = deliveryRepository.findById(id)
                .orElse(null);

        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Delivery not found");
        }

        User user = currentUser();

        if (!isAdmin(user) && !isAssignedDriver(user, existing)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have permission to update this delivery");
        }

        existing.setDriverName(delivery.getDriverName());
        existing.setVehicleNumber(delivery.getVehicleNumber());
        existing.setDeliveryStatus(delivery.getDeliveryStatus());

        if (delivery.getDeliveryRequest() != null) {
            existing.setDeliveryRequest(delivery.getDeliveryRequest());
        }

        if ("Delivered".equalsIgnoreCase(existing.getDeliveryStatus())) {

            DeliveryRequest request = existing.getDeliveryRequest();

            if (request != null) {
                request.setStatus("Completed");
                request.setCompleted(true);
                deliveryRequestRepository.save(request);
            }
        }

        Delivery updatedDelivery = deliveryRepository.save(existing);

        deliveryTrackingWebSocketHandler.broadcast(id, updatedDelivery);

        return ResponseEntity.ok(updatedDelivery);
    }

    // Admin only
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDelivery(@PathVariable Long id) {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can delete deliveries");
        }

        if (!deliveryRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Delivery not found");
        }

        deliveryRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
