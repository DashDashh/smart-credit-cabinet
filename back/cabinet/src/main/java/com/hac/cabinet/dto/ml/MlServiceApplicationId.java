package com.hac.cabinet.dto.ml;

import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlServiceApplicationId(
    UUID product_id,
    UUID client_id
) {
}
