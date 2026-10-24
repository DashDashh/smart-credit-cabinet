package com.hac.cabinet.repository;

import com.hac.cabinet.model.decision.Decision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DecisionRepository extends JpaRepository<Decision, UUID> {
}
