package com.warda.delivery_app.repository;

import com.warda.delivery_app.entity.PackageItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageItemRepository extends JpaRepository<PackageItem, Long> {

    List<PackageItem> findByDeliveryRequestId(Long deliveryRequestId);

}