package com.agtech.cloudbedsbatchcr.repositories;

import com.agtech.cloudbedsbatchcr.entities.CbAccount;
import com.agtech.cloudbedsbatchcr.entities.CbProperties;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ICbPropertiesRepository extends JpaRepository<CbProperties, Long> {

    Optional<CbProperties> findByCbAccount(CbAccount account);

    @Query(value = "select nextval(?1)", nativeQuery = true)
    Long getNextSequenceId(String seq);
}
