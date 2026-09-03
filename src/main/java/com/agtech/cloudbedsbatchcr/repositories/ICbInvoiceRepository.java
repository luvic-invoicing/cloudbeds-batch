package com.agtech.cloudbedsbatchcr.repositories;

import com.agtech.cloudbedsbatchcr.entities.CbInvoice;
import com.agtech.cloudbedsbatchcr.entities.CbInvoiceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ICbInvoiceRepository extends JpaRepository<CbInvoice, CbInvoiceId> {
    // Se realiza la busqueda por claveHacienda, es optional.
    Optional<CbInvoice> findByClaveHacienda(String claveHacienda);
}
