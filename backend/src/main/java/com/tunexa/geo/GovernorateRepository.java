package com.tunexa.geo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GovernorateRepository extends JpaRepository<Governorate, Long> {

    List<Governorate> findAllByOrderByNameFrAsc();
}
