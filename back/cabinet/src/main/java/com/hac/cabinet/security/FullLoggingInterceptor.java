package com.hac.cabinet.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FullLoggingInterceptor implements ClientHttpRequestInterceptor {

  private static final Logger log = LoggerFactory.getLogger(FullLoggingInterceptor.class);

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    // Логируем запрос
    log.info(">>> Request: {} {}", request.getMethod(), request.getURI());
    log.info(">>> Headers: {}", request.getHeaders());
    if (body.length > 0) {
      log.info(">>> Body: {}", new String(body, StandardCharsets.UTF_8));
    }

    // Выполняем и оборачиваем ответ, чтобы тело можно было прочитать повторно
    ClientHttpResponse response = execution.execute(request, body);
    BufferingClientHttpResponseWrapper wrapped = new BufferingClientHttpResponseWrapper(response);

    // Логируем ответ
    log.info("<<< Status: {}", wrapped.getStatusCode());
    log.info("<<< Headers: {}", wrapped.getHeaders());
    byte[] responseBody = StreamUtils.copyToByteArray(wrapped.getBody());
    if (responseBody.length > 0) {
      log.info("<<< Body: {}", new String(responseBody, StandardCharsets.UTF_8));
    }

    return wrapped;
  }

  private static class BufferingClientHttpResponseWrapper implements ClientHttpResponse {
    private final ClientHttpResponse response;
    private byte[] body;

    BufferingClientHttpResponseWrapper(ClientHttpResponse response) {
      this.response = response;
    }

    @Override
    public InputStream getBody() throws IOException {
      if (body == null) {
        body = StreamUtils.copyToByteArray(response.getBody());
      }
      return new ByteArrayInputStream(body);
    }

    @Override public org.springframework.http.HttpStatusCode getStatusCode() throws IOException { return response.getStatusCode(); }
    @Override public String getStatusText() throws IOException { return response.getStatusText(); }
    @Override public void close() { response.close(); }
    @Override public org.springframework.http.HttpHeaders getHeaders() { return response.getHeaders(); }
  }
}
