package com.warda.delivery_app.service;

import com.warda.delivery_app.entity.Delivery;
import com.warda.delivery_app.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public Delivery saveDelivery(Delivery delivery) {
        return deliveryRepository.save(delivery);
    }

    public List<Delivery> getAllDeliveries() {
        return deliveryRepository.findAll();
    }

    public Delivery getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Delivery not found with id: " + id));
    }

    public void deleteDelivery(Long id) {

        Delivery delivery = getDeliveryById(id);

        deliveryRepository.delete(delivery);
    }
}