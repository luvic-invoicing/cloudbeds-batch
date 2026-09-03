package com.agtech.cloudbedsbatchcr.repositories;

import com.agtech.cloudbedsbatchcr.entities.PaymentType;
import com.agtech.cloudbedsbatchcr.entities.PaymentTypeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTypeRepository extends JpaRepository<PaymentType, PaymentTypeId> {
}
