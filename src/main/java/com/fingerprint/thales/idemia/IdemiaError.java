package com.fingerprint.thales.idemia;

import com.fingerprint.thales.constants.Constants;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IdemiaError {

  NO_ERROR(0, "No hay error"),
  INTERNAL_ERROR(1, "El dispositivo biométrico encontró un error interno"),
  COMM_PROTOCOL_ERROR(2, "Error de protocolo de comunicación"),
  DEVICE_CONNECT_FAILED(3, "No se puede conectar el dispositivo biométrico"),
  INVALID_PARAMETER(5, "Parámetro invalido"),
  PC_NO_MEMORY(6, "No hay suficiente memoria en la PC"),
  MSO_UNKNOWN_STATUS(9, "El MSO devolvió un estatus de error desconocido"),
  COMMAND_NOT_IMPLEMENTED(18, "Comando no implementado en esta versión"),
  TIMEOUT(19, "No hay respuesta tras el tiempo definido"),
  ABORTED(26, "El comando ha sido abortado"),
  COMM_RETURN_ERROR_RANGE(37, "La función de retorno de comunicación regreso un error entre -10000 y -10499"),
  USB_NOT_CONNECTED(42, "El dispositivo USB no esta conectado"),
  FAKE_FINGER_DETECTED(46, "Dedo Falso Detectado"),
  NO_PARAMETER_INITIALIZED(49, "Ningún parámetro ha sido inicializado"),
  DEVICE_BLOCKED(57, "El dispositivo esta bloqueado"),
  QUALITY_NOT_MEETING_THRESHOLD(66, "El MorphoSmart no logró capturar la huella con una calidad mayor o igual al umbral especificado"),
  FEATURE_NOT_AVAILABLE_ON_DEVICE(72, "Una funcionalidad ha sido solicitada, pero no esta disponible en el dispositivo conectado");

  private final int code;

  private final String description;

  /**
   * Mensaje final listo: "Descripción (THALES_ENUM), código de error: <code>"
   */
  public String format(String thalesEnum) {
    String enumName = (thalesEnum == null || thalesEnum.isBlank()) ? Constants.UNKNOWN : thalesEnum;
    return String.format(Locale.ROOT, Constants.MSG_TEMPLATE_ES, description, enumName, code);
  }
}
