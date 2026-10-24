package com.hac.cabinet.repository;

import com.hac.cabinet.model.application.Application;
import com.hac.cabinet.model.application.ApplicationId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, ApplicationId> {
  List<Application> findAllByProductId(UUID productId);
  List<Application> findAllByClientId(UUID clientId);

  @EntityGraph(attributePaths = {
      "product",
      "client",
      "client.financialData",
      "client.creditHistory"
  })
  @Query("SELECT a FROM Application a WHERE a.id = :id")
  Optional<Application> findFullById(@Param("id") ApplicationId id);
}
