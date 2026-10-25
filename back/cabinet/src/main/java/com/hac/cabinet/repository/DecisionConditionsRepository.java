package com.hac.cabinet.repository;

import com.hac.cabinet.model.decision.DecisionConditions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DecisionConditionsRepository extends JpaRepository<DecisionConditions, UUID> {
}
