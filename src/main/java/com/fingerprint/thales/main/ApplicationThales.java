package com.fingerprint.thales.main;

import com.fingerprint.thales.constants.Constants;
import com.fingerprint.thales.exception.AcquisitionException;
import com.fingerprint.thales.model.RequestArg;
import com.fingerprint.thales.model.ResponseError;
import com.fingerprint.thales.model.ResponseOk;
import com.fingerprint.thales.service.AcquisitionService;
import com.fingerprint.thales.utils.ExceptionMapper;
import com.fingerprint.thales.utils.LogLevels;
import com.fingerprint.thales.utils.ResourceReader;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fingerprint.thales.utils.Jsons.toPrettyJson;

/**
 * Clase principal de la consola de adquisicion Thales.
 */
@Slf4j
public class ApplicationThales {

  /**
   * Logger especifico para registrar el resultado en JSON.
   */
  private static final Logger RESULT_LOG = LoggerFactory.getLogger(Constants.RESULT_LOGGER);

  /**
   * Indica si la operacion fue exitosa.
   */
  private static boolean success = false;


  public static void main(String[] args) {
    String resultJson = Constants.EMPTY_STR;
    RequestArg requestArg = RequestArg.from(args);
    LogLevels.apply(requestArg.logsEnabled());
    RESULT_LOG.info(Constants.INITIALIZATION_LOG);

    // 🧪 MOCK: si isMock es true, retornar un resultado simulado y terminar
    if (requestArg.isMock()) {
      String mockFingerprint = ResourceReader.readResourceFile(Constants.RESOURCE_MOCK_FINGERPRINT);
      resultJson = toPrettyJson(ResponseOk.builder()
              .fingerprint(mockFingerprint)
              .build());
      finallyProcess(resultJson, Constants.END_LOG_MOCK);
      return;
    }

    // ✅ Flujo normal
    try {
      AcquisitionService service = new AcquisitionService();
      ResponseOk result = service.startAcquisition(requestArg.timeout());

      if (result.fingerprint() == null || result.fingerprint().isBlank())
        throw new AcquisitionException(AcquisitionException.ErrorCode.INTERNAL_ERROR);

      resultJson = toPrettyJson(result);
      success = true;
    } catch (Exception e) {
      ResponseError error = ExceptionMapper.mapAndLog(log, e);
      resultJson = toPrettyJson(error);
    } finally {
      finallyProcess(resultJson, Constants.END_LOG);
    }
  }

  /**
   * Procesa el resultado final, registra los logs y termina la aplicacion.
   *
   * @param resultJson Resultado en formato JSON.
   * @param endLog     Mensaje de log final.
   */
  private static void finallyProcess(String resultJson, String endLog) {
    log.info(endLog);
    RESULT_LOG.info(Constants.ARG, resultJson);
    System.exit(success ? Constants.ZERO : Constants.ONE);
  }

}
