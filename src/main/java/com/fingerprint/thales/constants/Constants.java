package com.fingerprint.thales.constants;

import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constantes usadas en la aplicacion.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

  public static final String FINGERPRINT = "Fingerprint";

  public static final String RESULT_LOGGER = "result";

  public static final String RESOURCE_MOCK_FINGERPRINT = "mock/fingerprint_base64.txt";

  // Constantes RequestArg.

  public static final Long DEFAULT_TIMEOUT = 30000L;

  public static final int DEFAULT_THRESHOLD = 50;

  public static final boolean DEFAULT_LOGS_ENABLED = Boolean.FALSE;

  public static final Set<String> TRUE_VALUES = Set.of("true", "1", "yes", "y", "on");

  public static final Set<String> FALSE_VALUES = Set.of("false", "0", "no", "n", "off");

  // Constantes ApplicationThales class.

  public static final String INITIALIZATION_LOG = "Comenzando captura de huella...";

  public static final String END_LOG = "Terminando proceso de captura de huella...";

  public static final String END_LOG_MOCK = "Terminando proceso de captura de huella (MOCK)...";

  // Constantes AcquisitionService class.

  public static final String NOT_AVAILABLE = "NOT AVAILABLE";

  // Constantes GbmsApiDeviceUtil class

  public static final String FLAG_ACQUISITION_PHASE = "ACQUISITION_PHASE";

  public static final String FLAG_STOP_TYPE = "STOP_TYPE";

  public static final String FLAG_PREVIEW_RES = "PREVIEW_RES";

  public static final String FLAG_ACQUISITION_TIMEOUT = "ACQUISITION_TIMEOUT";

  public static final String FLAG_ENCRYPTED_FRAME_AES_256 = "ENCRYPTED_FRAME_AES_256";

  public static final String FLAG_IS_ISO_TEMPLATE = "IS_ISO_TEMPLATE";

  public static final String FLAG_FRAME_NOT_PRESENT = "FRAME_NOT_PRESENT";

  // Constantes for ResourceReader class.

  public static final String MOCK_FINGERPRINT = "MOCKED-FINGERPRINT-BASE64==";

  public static final String FILE_NOT_FOUND = "Archivo no encontrado en resources: {}";

  public static final String ERROR_READING_FILE = "Error leyendo archivo en resources: {}";

  // Constantes exceptions.

  public static final String ERROR = "Error";

  public static final String MESSAGE = "message";

  public static final String TYPE = "type";

  public static final String CODE = "code";

  public static final String IDEMIA_ERROR_MESSAGE = "idemiaErrorMessage";

  public static final String ACQUISITION_EXCEPTION_LOG = "[AcquisitionException] code={} | msg={}";

  public static final String ACQUISITION_EXCEPTION = "AcquisitionException";

  public static final String EXCEPTION = "Exception";

  public static final String UNKNOWN_MSG = "Error en la captura.";

  // Constantes Jsons class.

  public static final String LOG_ERR_JSON_SERIALIZE_PRETTY = "Error serializando JSON (pretty): {}";

  public static final String ERROR_MAPPER = "{\"error\":\"JsonSerializationError\"}";

  // Otras constantes.

  public static final String MSG_TEMPLATE_ES = "%s (%s), codigo de error: %d";

  public static final String UNKNOWN = "UNKNOWN";

  public static final String EMPTY_STR = "";

  public static final String COMMA = ", ";

  public static final String NULL_STR = "null";

  public static final String ARG = "{}";

  public static final String NEWLINE = "\n";

  public static final int ZERO = 0;

  public static final int ONE = 1;

  public static final int TWO = 2;

  public static final int THREE = 3;

  public static final int FOUR = 4;

  public static final int FIVE = 5;

  public static final byte ZERO_B = 0x00;

  public static final Long ZERO_LONG = 0L;

  public static final Long MILLISECONDS = 1000L;

  public static final int POLLING_INTERVAL_MS = 200;

  // AcquisitionStatesEnum descriptions.

  public static final String DESC_IDLE = "IDLE";

  public static final String DESC_PREVIEW = "PREVIEW";

  public static final String DESC_ACQUISITION = "ACQUISITION";

  public static final String DESC_SCANNER_START = "SCANNER START";

  public static final String DESC_ACQUISITION_END = "ACQUISITION END";

  // AcquisitionHandler class

  public static final String FLAT_RIGHT_INDEX = "FLAT_RIGHT_INDEX";

}
