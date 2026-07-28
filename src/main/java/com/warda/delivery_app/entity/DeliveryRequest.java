package com.warda.delivery_app.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "delivery_requests")
public class DeliveryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Pickup location is required")
    @Column(nullable = false)
    private String pickupLocation;

    @Column(nullable = false)
    private Double pickupLatitude = 0.0;

    @Column(nullable = false)
    private Double pickupLongitude = 0.0;

    @NotBlank(message = "Destination is required")
    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private Double destinationLatitude = 0.0;

    @Column(nullable = false)
    private Double destinationLongitude = 0.0;

    @NotBlank(message = "Receiver name is required")
    @Column(nullable = false)
    private String receiverName;

    @NotBlank(message = "Receiver phone is required")
    @Column(nullable = false)
    private String receiverPhone;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false)
    private Boolean completed = false;

    @Column(nullable = false)
    private Double estimatedDistance = 0.0;

    @Column(nullable = false)
    private Double estimatedPrice = 0.0;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @OneToOne(
            mappedBy = "deliveryRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private PackageItem packageItem;

    public DeliveryRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(Double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(Double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Double getEstimatedDistance() {
        return estimatedDistance;
    }

    public void setEstimatedDistance(Double estimatedDistance) {
        this.estimatedDistance = estimatedDistance;
    }

    public Double getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(Double estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public PackageItem getPackageItem() {
        return packageItem;
    }

    public void setPackageItem(PackageItem packageItem) {
        this.packageItem = packageItem;
    }
}