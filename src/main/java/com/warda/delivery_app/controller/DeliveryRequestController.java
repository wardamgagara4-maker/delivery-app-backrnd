package com.warda.delivery_app.controller;

import com.warda.delivery_app.entity.DeliveryRequest;
import com.warda.delivery_app.entity.User;
import com.warda.delivery_app.repository.DeliveryRequestRepository;
import com.warda.delivery_app.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.warda.delivery_app.dto.AssignDriverRequest;
import com.warda.delivery_app.entity.Delivery;
import com.warda.delivery_app.service.DeliveryRequestService;
import java.util.List;

@RestController
@RequestMapping("/api/delivery-requests")
@RequiredArgsConstructor
public class DeliveryRequestController {

    private final DeliveryRequestRepository repository;
    private final DeliveryRequestService deliveryRequestService;
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

    // Get All Requests (admin only — used by the admin requests page)
    @GetMapping
    public ResponseEntity<?> getAllRequests() {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can view all delivery requests");
        }

        return ResponseEntity.ok(repository.findAll());
    }

    // Get Requests Belonging To The Logged-in Customer
    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests() {

        User user = currentUser();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in to view your requests");
        }

        return ResponseEntity.ok(
                repository.findByCustomerEmailOrderByIdDesc(user.getEmail())
        );
    }

    // Get Request By ID (admin or the owning customer)
    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(@PathVariable Long id) {

        DeliveryRequest request = repository.findById(id).orElse(null);

        if (request == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Delivery Request not found");
        }

        User user = currentUser();

        if (!isAdmin(user) && !ownsRequest(user, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have access to this delivery request");
        }

        return ResponseEntity.ok(request);
    }

    // Create Request
    @PostMapping
    public ResponseEntity<?> createRequest(
            @Valid @RequestBody DeliveryRequest request) {

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("PENDING");
        }

        // The RequestDelivery form does not send a customer id, so we
        // attach the currently logged-in user (from the JWT) as the customer.
        if (request.getCustomer() == null) {

            User customer = currentUser();

            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("You must be logged in to create a delivery request");
            }

            request.setCustomer(customer);
        }

        DeliveryRequest savedRequest = repository.save(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedRequest);
    }

    // Update Request (admin or the owning customer)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryRequest updatedRequest) {

        DeliveryRequest request = repository.findById(id).orElse(null);

        if (request == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Delivery Request not found");
        }

        User user = currentUser();

        if (!isAdmin(user) && !ownsRequest(user, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have permission to edit this delivery request");
        }

        request.setPickupLocation(updatedRequest.getPickupLocation());
        request.setDestination(updatedRequest.getDestination());
        request.setReceiverName(updatedRequest.getReceiverName());
        request.setReceiverPhone(updatedRequest.getReceiverPhone());

        // Only an admin may change status/completed directly; a customer
        // editing their own request should not be able to mark it completed.
        if (isAdmin(user)) {
            request.setStatus(updatedRequest.getStatus());
            request.setCompleted(updatedRequest.getCompleted());
        }

        DeliveryRequest saved = repository.save(request);

        return ResponseEntity.ok(saved);
    }

    // Delete Request (admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can delete delivery requests");
        }

        if (!repository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Delivery Request not found");
        }

        repository.deleteById(id);

        return ResponseEntity.ok("Delivery Request deleted successfully");
    }

    // Search (admin only — used by the admin requests page)
    @GetMapping("/search")
    public ResponseEntity<?> searchRequests(
            @RequestParam String keyword) {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can search delivery requests");
        }

        List<DeliveryRequest> requests =
                repository.findByPickupLocationContainingIgnoreCase(keyword);

        if (requests.isEmpty()) {
            requests = repository.findByDestinationContainingIgnoreCase(keyword);
        }

        if (requests.isEmpty()) {
            requests = repository.findByReceiverNameContainingIgnoreCase(keyword);
        }

        return ResponseEntity.ok(requests);
    }

    // Assign a driver to a request (admin only)
    @PutMapping("/{requestId}/assign-driver")
    public ResponseEntity<?> assignDriver(
            @PathVariable Long requestId,
            @RequestBody AssignDriverRequest request) {

        User user = currentUser();

        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only admins can assign a driver");
        }

        Delivery delivery =
                deliveryRequestService.assignDriver(requestId, request);

        return ResponseEntity.ok(delivery);
    }

}
