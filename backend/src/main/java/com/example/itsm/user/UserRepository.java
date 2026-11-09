package com.example.itsm.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByOrderByNameAsc();

    @Query("select distinct t from Technician t left join fetch t.skills where t.active = true")
    List<Technician> findActiveTechniciansWithSkills();

    @Query("select distinct t from Technician t left join fetch t.skills where t.id = :id")
    Optional<Technician> findTechnicianWithSkills(@Param("id") Long id);
}
