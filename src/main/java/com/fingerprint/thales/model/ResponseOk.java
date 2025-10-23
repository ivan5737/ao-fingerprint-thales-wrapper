package com.fingerprint.thales.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fingerprint.thales.constants.Constants;
import lombok.Builder;

/**
 * Modelo de respuesta OK en formato JSON.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseOk(
        @JsonProperty(Constants.FINGERPRINT) String fingerprint
) {
}
