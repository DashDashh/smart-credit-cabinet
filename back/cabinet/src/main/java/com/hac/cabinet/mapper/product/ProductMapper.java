package com.hac.cabinet.mapper.product;

import com.hac.cabinet.dto.product.UpsertProductRequest;
import com.hac.cabinet.dto.product.ProductResponse;
import com.hac.cabinet.model.product.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

  public Product fromRequest(UpsertProductRequest request) {
    Product p = new Product();
    p.setName(request.name());
    p.setAmount(request.amount());
    p.setTermMonths(request.termMonths());
    p.setRate(request.rate());
    p.setPurpose(request.purpose());
    return p;
  }

  public void applyUpdate(UpsertProductRequest request, Product p) {
    p.setName(request.name());
    p.setAmount(request.amount());
    p.setTermMonths(request.termMonths());
    p.setRate(request.rate());
    p.setPurpose(request.purpose());
  }

  public ProductResponse toResponse(Product p) {
    return new ProductResponse(
        p.getId(),
        p.getName(),
        p.getAmount(),
        p.getTermMonths(),
        p.getRate(),
        p.getPurpose()
    );
  }
}
