package com.rentaltrack.location.repository;

import com.rentaltrack.location.domain.RentalLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalLocationRepository extends JpaRepository<RentalLocation, Long> {

    List<RentalLocation> findAllByActiveTrueOrderByNameAsc();
}
