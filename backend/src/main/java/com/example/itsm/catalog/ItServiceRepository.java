package com.example.itsm.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItServiceRepository extends JpaRepository<ItService, Long> {

    Optional<ItService> findByCodeIgnoreCase(String code);

    Optional<ItService> findByNameIgnoreCase(String name);

    List<ItService> findByActiveTrueOrderByNameAsc();
}
