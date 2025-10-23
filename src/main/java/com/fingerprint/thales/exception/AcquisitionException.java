package com.fingerprint.thales.exception;

import com.fingerprint.thales.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class AcquisitionException extends RuntimeException {

  private final ErrorCode errorCode;

  public AcquisitionException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public AcquisitionException(ErrorCode errorCode, Exception ex) {
    super(errorCode.getMessage(), ex);
    this.errorCode = errorCode;
  }

  @Getter
  @AllArgsConstructor
  public enum ErrorCode {
    // Internos
    INTERNAL_ERROR(-1, "Unexpected internal error", "Error interno inesperado"),
    INIT_NO_DEVICES_FOUND(-10, "No Green Bit devices detected", "No se detectaron dispositivos Green Bit conectados"),
    UNKNOWN(-11, "Unknown error", Constants.UNKNOWN_MSG),
    ACQUISITION_TIMEOUT(-20, "Acquisition timeout", "No se detectó actividad durante el tiempo máximo permitido"),
    NO_FINGERPRINT(-30, "No Fingerprint", "No se generó huella — respuesta vacía."),

    // SDK GBMSAPI (0 - 255)
    NO_ERROR(0, "No error", "Sin error"),
    USB_DRIVER(1, "USB driver error", "Error en el controlador USB"),
    DEVICE_NOT_FOUND(2, "Device not found", "Dispositivo no encontrado"),
    USB_THREAD(3, "USB thread error", "Error en el hilo de comunicación USB"),
    PARAMETER(4, "Invalid parameter", "Parámetro inválido o fuera de rango"),
    SCANNER_NOT_CONFIGURED(5, "Scanner not configured", "El escáner no está configurado"),
    DEVICE_NOT_RESPONDING(6, "Device not responding", "El dispositivo no responde"),
    SCANNER_COMMUNICATION(7, "Scanner communication error", "Error de comunicación con el escáner"),
    UNAVAILABLE_OPTION(8, "Unavailable option", "Opción no disponible en este dispositivo"),
    INTERNAL(9, "Internal error", "Error interno del sistema o SDK"),
    USB_FULLSPEED_NOT_SUPPORTED(10, "USB 1.1 not supported", "USB 1.1 no soportado; use un puerto USB 2.0 o superior"),
    DEVICE_LOCKED(11, "Device locked", "El dispositivo está bloqueado o protegido"),
    SPECIFIC_DLL_NOT_LOADED(30, "Specific DLL not loaded", "No se pudo cargar la DLL específica del escáner"),
    METHOD_NOT_SUPPORTED(31, "Method not supported", "Método o función no soportada"),
    OBJECT_TYPE_NOT_SUPPORTED(32, "Object type not supported", "Tipo de objeto no soportado"),
    SCAN_AREA_NOT_SUPPORTED(33, "Scan area not supported", "Área de escaneo no soportada"),
    ACQUISITION_THREAD(34, "Acquisition thread error", "Error en el hilo de adquisición"),
    ACQUISITION_ALREADY_STARTED(35, "Acquisition already started", "Ya existe una adquisición en progreso"),
    FEATURE_NOT_SUPPORTED(36, "Feature not supported", "Funcionalidad no soportada"),
    CURRENT_DEV_NOT_SET(37, "Current device not set", "No se ha establecido un dispositivo activo"),
    MEMORY_ALLOCATION(38, "Memory allocation failed", "Error de asignación de memoria"),
    GENERIC(39, "Generic error", "Error genérico no especificado"),
    OUTSIDE_ACQUISITION(40, "Called outside acquisition context", "Llamada fuera del contexto de adquisición"),
    NOT_ALLOWED_FAKE_FINGER_DETECTED(41, "Fake finger detected - not allowed", "Dedo falso detectado: operación no permitida"),
    EXCEPTION(255, "Exception occurred in GBMSAPI library", "Excepción detectada dentro de la biblioteca GBMSAPI");

    private final int code;

    private final String message;

    private final String messageEs;

    public static ErrorCode fromCode(int code) {
      for (ErrorCode e : values()) {
        if (e.code == code) return e;
      }
      return GENERIC;
    }

  }

}