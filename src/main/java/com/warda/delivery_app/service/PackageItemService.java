package com.warda.delivery_app.service;

import com.warda.delivery_app.entity.PackageItem;
import com.warda.delivery_app.repository.PackageItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageItemService {

    private final PackageItemRepository packageRepository;

    public PackageItem savePackage(PackageItem packageItem) {
        return packageRepository.save(packageItem);
    }

    public List<PackageItem> getAllPackages() {
        return packageRepository.findAll();
    }

    public PackageItem getPackageById(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Package not found with id: " + id));
    }

    public void deletePackage(Long id) {

        PackageItem packageItem = getPackageById(id);

        packageRepository.delete(packageItem);
    }
}