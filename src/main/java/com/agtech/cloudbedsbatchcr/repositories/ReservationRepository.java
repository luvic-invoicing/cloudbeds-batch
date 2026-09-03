package com.agtech.cloudbedsbatchcr.repositories;

import com.agtech.cloudbedsbatchcr.entities.CbReservation;
import com.agtech.cloudbedsbatchcr.entities.CbReservationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<CbReservation, CbReservationId> {
}
