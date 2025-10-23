package com.fingerprint.thales.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fingerprint.thales.constants.Constants;
import com.fingerprint.thales.exception.AcquisitionException;
import com.fingerprint.thales.idemia.IdemiaError;
import lombok.Builder;

/**
 * Modelo de respuesta de error en formato JSON.
 */
@Builder
@JsonPropertyOrder({
        Constants.ERROR,
        Constants.CODE,
        Constants.MESSAGE,
        Constants.IDEMIA_ERROR_MESSAGE
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseError(
        @JsonProperty(Constants.ERROR) String error,
        @JsonProperty(Constants.TYPE) String type,
        @JsonProperty(Constants.CODE) Integer code,
        @JsonProperty(Constants.MESSAGE) String message,
        @JsonProperty(Constants.IDEMIA_ERROR_MESSAGE) String idemiaErrorMessage
) {

  /**
   * Construye un ResponseError a partir de una AcquisitionException.
   */
  public static ResponseError from(AcquisitionException ae) {
    final var errorCode = ae.getErrorCode();
    final var ide = mapThalesToIdemia(errorCode);
    return ResponseError.builder()
            .error(Constants.ACQUISITION_EXCEPTION)
            .code(errorCode.getCode())
            .message(String.valueOf(ae.getMessage()))
            .idemiaErrorMessage(ide.format(errorCode.name()))
            .build();
  }

  /**
   * Construye un ResponseError a partir de una excepción desconocida.
   */
  public static ResponseError fromUnknown(Throwable th) {
    final var type = AcquisitionException.ErrorCode.UNKNOWN;
    final var ide = IdemiaError.INTERNAL_ERROR;
    return ResponseError.builder()
            .error(Constants.EXCEPTION)
            .code(-1)
            .message(String.valueOf(th.getMessage()))
            .idemiaErrorMessage(ide.format(type.name()))
            .build();
  }

  public static IdemiaError mapThalesToIdemia(AcquisitionException.ErrorCode errorCode) {
    if (errorCode == null) return IdemiaError.MSO_UNKNOWN_STATUS;

    return switch (errorCode) {
      // ERRORES INTERNOS / GENERALES
      case INTERNAL_ERROR -> IdemiaError.INTERNAL_ERROR;
      case INIT_NO_DEVICES_FOUND, DEVICE_NOT_FOUND, USB_FULLSPEED_NOT_SUPPORTED ->
              IdemiaError.USB_NOT_CONNECTED;
      case UNKNOWN, GENERIC -> IdemiaError.MSO_UNKNOWN_STATUS;
      case NO_ERROR -> IdemiaError.NO_ERROR;
      case EXCEPTION -> IdemiaError.INTERNAL_ERROR;
      case INTERNAL -> IdemiaError.INTERNAL_ERROR;

      // ERRORES DE CONEXIÓN / DISPOSITIVO
      case USB_DRIVER -> IdemiaError.DEVICE_CONNECT_FAILED;
      case DEVICE_NOT_RESPONDING, ACQUISITION_TIMEOUT -> IdemiaError.TIMEOUT;
      case SCANNER_COMMUNICATION, USB_THREAD -> IdemiaError.COMM_PROTOCOL_ERROR;
      case DEVICE_LOCKED -> IdemiaError.DEVICE_BLOCKED;

      // ERRORES DE CONFIGURACIÓN
      case SCANNER_NOT_CONFIGURED -> IdemiaError.NO_PARAMETER_INITIALIZED;
      case CURRENT_DEV_NOT_SET -> IdemiaError.NO_PARAMETER_INITIALIZED;

      // ERRORES DE SDK / FUNCIONES
      case SPECIFIC_DLL_NOT_LOADED -> IdemiaError.MSO_UNKNOWN_STATUS;
      case METHOD_NOT_SUPPORTED -> IdemiaError.COMMAND_NOT_IMPLEMENTED;
      case OBJECT_TYPE_NOT_SUPPORTED, SCAN_AREA_NOT_SUPPORTED, FEATURE_NOT_SUPPORTED,
           UNAVAILABLE_OPTION -> IdemiaError.FEATURE_NOT_AVAILABLE_ON_DEVICE;

      // ERRORES DE MEMORIA Y PARÁMETROS
      case PARAMETER -> IdemiaError.INVALID_PARAMETER;
      case MEMORY_ALLOCATION -> IdemiaError.PC_NO_MEMORY;

      case NO_FINGERPRINT -> IdemiaError.QUALITY_NOT_MEETING_THRESHOLD;

      // ERRORES DE CAPTURA / ADQUISICIÓN
      case ACQUISITION_THREAD -> IdemiaError.COMM_RETURN_ERROR_RANGE;
      case ACQUISITION_ALREADY_STARTED -> IdemiaError.ABORTED;
      case OUTSIDE_ACQUISITION -> IdemiaError.INVALID_PARAMETER;
      case NOT_ALLOWED_FAKE_FINGER_DETECTED -> IdemiaError.FAKE_FINGER_DETECTED;
    };
  }

}
