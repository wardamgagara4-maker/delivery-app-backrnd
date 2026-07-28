package com.warda.delivery_app.repository;

import com.warda.delivery_app.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findTop5ByOrderByIdDesc();

    List<Delivery> findByDeliveryStatus(String deliveryStatus);

    List<Delivery> findByDeliveryRequest_Customer_EmailOrderByIdDesc(String email);

    List<Delivery> findByDriverNameOrderByIdDesc(String driverName);

}