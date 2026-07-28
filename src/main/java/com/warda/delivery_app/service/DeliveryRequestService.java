package com.warda.delivery_app.service;

import com.warda.delivery_app.dto.AssignDriverRequest;
import com.warda.delivery_app.entity.Delivery;
import com.warda.delivery_app.entity.DeliveryRequest;
import com.warda.delivery_app.entity.User;
import com.warda.delivery_app.repository.DeliveryRepository;
import com.warda.delivery_app.repository.DeliveryRequestRepository;
import com.warda.delivery_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryRequestService {

    private final DeliveryRequestRepository deliveryRequestRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;

    public DeliveryRequest saveRequest(DeliveryRequest request) {
        return deliveryRequestRepository.save(request);
    }

    public List<DeliveryRequest> getAllRequests() {
        return deliveryRequestRepository.findAll();
    }

    public DeliveryRequest getRequestById(Long id) {
        return deliveryRequestRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Delivery Request not found with id: " + id));
    }

    public DeliveryRequest updateRequest(Long id, DeliveryRequest updatedRequest) {

        DeliveryRequest existingRequest = getRequestById(id);

        existingRequest.setPickupLocation(updatedRequest.getPickupLocation());
        existingRequest.setDestination(updatedRequest.getDestination());
        existingRequest.setReceiverName(updatedRequest.getReceiverName());
        existingRequest.setReceiverPhone(updatedRequest.getReceiverPhone());
        existingRequest.setStatus(updatedRequest.getStatus());
        existingRequest.setCompleted(updatedRequest.getCompleted());

        if (updatedRequest.getCustomer() != null) {
            existingRequest.setCustomer(updatedRequest.getCustomer());
        }

        if (updatedRequest.getPackageItem() != null) {
            existingRequest.setPackageItem(updatedRequest.getPackageItem());
        }

        return deliveryRequestRepository.save(existingRequest);
    }

    public void deleteRequest(Long id) {

        DeliveryRequest request = getRequestById(id);

        deliveryRequestRepository.delete(request);
    }

    // ==========================
    // ASSIGN DRIVER
    // ==========================

    public Delivery assignDriver(Long requestId, AssignDriverRequest assignRequest) {

        DeliveryRequest deliveryRequest = deliveryRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("Delivery Request not found"));

        User driver = userRepository.findById(assignRequest.getDriverId())
                .orElseThrow(() ->
                        new RuntimeException("Driver not found"));

        Delivery delivery = new Delivery();

        delivery.setDriverName(driver.getFullName());
        delivery.setVehicleNumber("Not Assigned");
        delivery.setDeliveryStatus("ASSIGNED");
        delivery.setDeliveryRequest(deliveryRequest);

        deliveryRequest.setStatus("ASSIGNED");
        deliveryRequestRepository.save(deliveryRequest);

        return deliveryRepository.save(delivery);
    }

}