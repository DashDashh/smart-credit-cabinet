package com.hac.cabinet.service;

import com.hac.cabinet.dto.ml.DecisionRequest;
import com.hac.cabinet.dto.ml.DecisionResponse;
import com.hac.cabinet.dto.ml.MlHealthResponse;
import com.hac.cabinet.mapper.application.ApplicationMapper;
import com.hac.cabinet.mapper.decision.DecisionMapper;
import com.hac.cabinet.model.application.Application;
import com.hac.cabinet.model.application.ApplicationId;
import com.hac.cabinet.model.decision.Decision;
import com.hac.cabinet.model.decision.DecisionConditions;
import com.hac.cabinet.repository.ApplicationRepository;
import com.hac.cabinet.repository.DecisionRepository;
import com.hac.cabinet.security.FullLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class RestCommunicationService {

  private final RestClient restClient;
  private final RetryTemplate retryTemplate;
  private final ApplicationRepository applicationRepository;
  private final DecisionMapper decisionMapper;
  private final DecisionRepository decisionRepository;

  public RestCommunicationService(RestClient.Builder builder, ApplicationRepository applicationRepository, DecisionMapper decisionMapper, DecisionRepository decisionRepository) {
    this.applicationRepository = applicationRepository;
    this.decisionMapper = decisionMapper;
    this.decisionRepository = decisionRepository;
    HttpClientSettings settings = HttpClientSettings.defaults()
        .withConnectTimeout(Duration.ofSeconds(3))
        .withReadTimeout(Duration.ofSeconds(10));

    this.restClient = builder
        .baseUrl("http://cabinet-ml:8000")
        .requestFactory(new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build()
        ))
        .requestInterceptor(new FullLoggingInterceptor())
        .build();

    RetryPolicy retryPolicy = RetryPolicy.builder()
        .maxRetries(3) // всего попыток: 1 + 3 = 4
        .delay(Duration.ofSeconds(1))
        .multiplier(2.0) // экспоненциальная задержка: 1s, 2s, 4s
        .includes(RuntimeException.class)
        .build();

    this.retryTemplate = new RetryTemplate(retryPolicy);
  }

  public void requestDecision(DecisionRequest request) {
    CompletableFuture.runAsync(() -> {
      try {
        retryTemplate.invoke(() -> {
          checkHealth();
          doRequestDecision(request);
        });
      } catch (Exception ex) {
        log.error("Failed to get decision from ML service after retries", ex);
      }
    });
  }


  private void doRequestDecision(DecisionRequest request) {
    log.info("Sending decision request to ML service");
    DecisionResponse response = restClient.post()
        .uri("/decision")
        .body(request)
        .retrieve()
        .body(DecisionResponse.class);

    log.info("ML decision received: score={}, decision={}, next_step={}",
        response.score(), response.decision(), response.next_step());
    log.info("ML decision received (FULL): {}", response);

    writeResponse(response);
  }

  private void checkHealth() {
    MlHealthResponse health = restClient.get()
        .uri("/health")
        .retrieve()
        .body(MlHealthResponse.class);

    if (health == null || !"ok".equalsIgnoreCase(health.status())) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "ML service is not healthy: status=" + (health != null ? health.status() : "null")
              + ", mlAvailable=" + (health != null ? health.mlAvailable() : "null")
              + ", loadError=" + (health != null ? health.loadError() : "null")
      );
    }
  }

  @Transactional
  private void writeResponse(DecisionResponse decisionResponse) {
    Decision decision = decisionMapper.toDecision(decisionResponse);

    ApplicationId applicationId = decisionMapper.toApplicationId(decisionResponse.application_id());
    Application application = applicationRepository.findById(applicationId).orElseThrow();
    decision.setApplication(application);

    decision.setConditions(null);
    if (decisionResponse.conditions().isPresent()) {
      DecisionConditions decisionConditions = decisionMapper
          .toDecisionConditions(decisionResponse.conditions().get());

      decision.setConditions(decisionConditions);
      decisionConditions.setDecision(decision);
    }

    decisionRepository.save(decision);
  }
}

