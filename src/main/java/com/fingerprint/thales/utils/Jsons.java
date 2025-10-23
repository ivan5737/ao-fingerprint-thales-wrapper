package com.fingerprint.thales.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fingerprint.thales.constants.Constants;
import com.fingerprint.thales.model.ResponseError;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Clase de utilidades para manejo de JSON.
 */
@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Jsons {

  /**
   * ObjectMapper para serializar a JSON ignorando campos nulos.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper()
          .findAndRegisterModules()
          .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  /**
   * Convierte el Object a un String JSON con formato "pretty" (con saltos de
   * línea y sangrías). Si el objeto es una instancia de ResponseError, se
   * formatea el mensaje de error para incluir el código de error.
   */
  public static String toPrettyJson(Object value) {
    try {
      if (value instanceof ResponseError err) {
        return MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(Map.of(Constants.ERROR, err.idemiaErrorMessage()));
      }
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (Exception e) {
      log.error(Constants.LOG_ERR_JSON_SERIALIZE_PRETTY, safeClass(value), e);
      return Constants.ERROR_MAPPER;
    }
  }

  /**
   * Devuelve el nombre de la clase del objeto, o "null" si el objeto es nulo.
   */
  private static String safeClass(Object value) {
    return (value == null) ? Constants.NULL_STR : value.getClass().getName();
  }
}
