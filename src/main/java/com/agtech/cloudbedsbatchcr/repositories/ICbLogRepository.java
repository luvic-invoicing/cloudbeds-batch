package com.agtech.cloudbedsbatchcr.repositories;


import com.agtech.cloudbedsbatchcr.entities.CbLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICbLogRepository extends JpaRepository<CbLog, Long> {

}
