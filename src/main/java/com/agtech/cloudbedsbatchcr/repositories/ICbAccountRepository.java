package com.agtech.cloudbedsbatchcr.repositories;

import com.agtech.cloudbedsbatchcr.entities.CbAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ICbAccountRepository extends JpaRepository<CbAccount, Long> {
}
