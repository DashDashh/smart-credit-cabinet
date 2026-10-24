package com.hac.cabinet.dto.ml;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlHealthResponse(
    String status,
    Boolean mlAvailable,
    String modelVersion,
    String loadError
) {
}
