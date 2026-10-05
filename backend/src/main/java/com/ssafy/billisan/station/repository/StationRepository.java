package com.ssafy.billisan.station.repository;

import com.ssafy.billisan.station.domain.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StationRepository extends JpaRepository<Station, UUID> {
}
