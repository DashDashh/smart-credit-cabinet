package com.hac.cabinet.service;

import com.hac.cabinet.dto.product.UpsertProductRequest;
import com.hac.cabinet.dto.product.ProductResponse;
import com.hac.cabinet.mapper.product.ProductMapper;
import com.hac.cabinet.model.product.Product;
import com.hac.cabinet.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;
  private final AccountService accountService;

  @Transactional
  public ProductResponse create(UpsertProductRequest request) {
    Product product = productMapper.fromRequest(request);
    return productMapper.toResponse(productRepository.save(product));
  }

  @Transactional(readOnly = true)
  public ProductResponse getById(UUID accountId, UUID id) {
    accountService.existsById(accountId);

    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Product not found: " + id));

    return productMapper.toResponse(product);
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> getAll(UUID accountId) {
    accountService.existsById(accountId);
    return productRepository.findAll().stream()
        .map(productMapper::toResponse)
        .toList();
  }

  @Transactional
  public ProductResponse update(UUID id, UpsertProductRequest request) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Product not found: " + id));

    productMapper.applyUpdate(request, product);

    return productMapper.toResponse(productRepository.save(product));
  }
}
