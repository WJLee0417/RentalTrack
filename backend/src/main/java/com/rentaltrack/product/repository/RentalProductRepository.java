package com.rentaltrack.product.repository;

import com.rentaltrack.product.domain.RentalProduct;
import com.rentaltrack.product.domain.RentalProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentalProductRepository extends JpaRepository<RentalProduct, Long> {

    @Query("""
            select p
            from RentalProduct p
            join fetch p.category c
            join fetch p.currentLocation l
            where p.status = :status
              and l.active = true
              and c.active = true
              and (:locationId is null or l.id = :locationId)
              and (:categoryId is null or c.id = :categoryId)
            order by l.name asc, c.name asc, p.displayName asc
            """)
    List<RentalProduct> findAvailableProducts(
            @Param("status") RentalProductStatus status,
            @Param("locationId") Long locationId,
            @Param("categoryId") Long categoryId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from RentalProduct p
            join fetch p.category
            join fetch p.currentLocation
            where p.id = :id
            """)
    Optional<RentalProduct> findByIdForUpdate(@Param("id") Long id);
}
