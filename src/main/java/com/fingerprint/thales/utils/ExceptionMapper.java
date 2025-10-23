package com.fingerprint.thales.utils;

import com.fingerprint.thales.constants.Constants;
import com.fingerprint.thales.exception.AcquisitionException;
import com.fingerprint.thales.model.ResponseError;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

/**
 * Clase que mapea excepciones a objetos ResponseError y las registra en el log.
 */
@UtilityClass
public class ExceptionMapper {

  /**
   * Mapea una excepción a un ResponseError y la registra en el log.
   *
   * @param log       Logger para registrar la excepción.
   * @param throwable Excepción a mapear.
   * @return Objeto ResponseError correspondiente.
   */
  public ResponseError mapAndLog(Logger log, Throwable throwable) {
    if (throwable instanceof AcquisitionException ae) {
      final var errorCode = ae.getErrorCode();
      final var code = errorCode.getCode();
      final var msg = errorCode.getMessage();
      log.error(Constants.ACQUISITION_EXCEPTION_LOG, code, msg, ae);
      return ResponseError.from(ae);
    }

    log.error(Constants.EXCEPTION, throwable);
    return ResponseError.fromUnknown(throwable);
  }

}
