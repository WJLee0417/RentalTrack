package com.rentaltrack.catalog.application;

import com.rentaltrack.catalog.api.LocationResponse;
import com.rentaltrack.catalog.api.ProductResponse;
import com.rentaltrack.location.repository.RentalLocationRepository;
import com.rentaltrack.product.domain.RentalProductStatus;
import com.rentaltrack.product.repository.RentalProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final RentalLocationRepository rentalLocationRepository;
    private final RentalProductRepository rentalProductRepository;

    public CatalogService(
            RentalLocationRepository rentalLocationRepository,
            RentalProductRepository rentalProductRepository
    ) {
        this.rentalLocationRepository = rentalLocationRepository;
        this.rentalProductRepository = rentalProductRepository;
    }

    public List<LocationResponse> findActiveLocations() {
        return rentalLocationRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(LocationResponse::from)
                .toList();
    }

    public List<ProductResponse> findAvailableProducts(Long locationId, Long categoryId) {
        return rentalProductRepository.findAvailableProducts(
                        RentalProductStatus.AVAILABLE,
                        locationId,
                        categoryId
                ).stream()
                .map(ProductResponse::from)
                .toList();
    }
}
