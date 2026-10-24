package com.hac.cabinet.repository;

import com.hac.cabinet.model.application.Application;
import com.hac.cabinet.model.application.ApplicationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, ApplicationId> {
  List<Application> findAllByProductId(UUID productId);
  List<Application> findAllByClientId(UUID clientId);
}
