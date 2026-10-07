package com.rentaltrack.rental.repository;

import com.rentaltrack.rental.domain.RentalContract;
import com.rentaltrack.rental.domain.RentalContractStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rc
            from RentalContract rc
            join fetch rc.member
            join fetch rc.rentalProduct p
            join fetch p.category
            join fetch p.currentLocation
            join fetch rc.pickupLocation
            left join fetch rc.returnLocation
            where rc.id = :id
            """)
    Optional<RentalContract> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select rc
            from RentalContract rc
            join fetch rc.member
            join fetch rc.rentalProduct p
            join fetch p.category
            join fetch p.currentLocation
            join fetch rc.pickupLocation
            left join fetch rc.returnLocation
            where rc.id = :id
            """)
    Optional<RentalContract> findDetailById(@Param("id") Long id);

    @Query("select rc.member.id from RentalContract rc where rc.id = :id")
    Optional<Long> findMemberIdById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "rentalProduct",
            "rentalProduct.category",
            "pickupLocation",
            "returnLocation"
    })
    Page<RentalContract> findAllByMember_Id(Long memberId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "rentalProduct",
            "rentalProduct.category",
            "pickupLocation",
            "returnLocation"
    })
    Page<RentalContract> findAllByMember_IdAndStatus(
            Long memberId,
            RentalContractStatus status,
            Pageable pageable
    );
}
