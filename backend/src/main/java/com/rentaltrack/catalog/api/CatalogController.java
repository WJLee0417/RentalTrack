package com.rentaltrack.catalog.api;

import com.rentaltrack.catalog.application.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/locations")
    public List<LocationResponse> getLocations() {
        return catalogService.findActiveLocations();
    }

    @GetMapping("/products")
    public List<ProductResponse> getAvailableProducts(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long categoryId
    ) {
        return catalogService.findAvailableProducts(locationId, categoryId);
    }
}
