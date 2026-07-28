package com.warda.delivery_app.repository;

import com.warda.delivery_app.entity.DeliveryRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRequestRepository extends JpaRepository<DeliveryRequest, Long> {

    List<DeliveryRequest> findTop5ByOrderByIdDesc();

    List<DeliveryRequest> findByPickupLocationContainingIgnoreCase(String pickupLocation);

    List<DeliveryRequest> findByDestinationContainingIgnoreCase(String destination);

    List<DeliveryRequest> findByReceiverNameContainingIgnoreCase(String receiverName);

    List<DeliveryRequest> findByStatus(String status);

    List<DeliveryRequest> findByCompleted(boolean completed);

    List<DeliveryRequest> findByCustomerEmailOrderByIdDesc(String email);

}