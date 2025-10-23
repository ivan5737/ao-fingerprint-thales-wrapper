package com.fingerprint.thales.utils;

import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_DiagnosticMessages;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_EventInfo;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_DeviceCharacteristicsDefines.GBMSAPI_JAVA_DeviceName;
import com.fingerprint.thales.constants.Constants;
import com.fingerprint.thales.exception.AcquisitionException;
import com.fingerprint.thales.exception.AcquisitionException.ErrorCode;
import com.sun.jna.Pointer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GbmsApiDeviceUtil {

  private static final Map<Integer, String> DIAGNOSTIC_MESSAGES = Map.ofEntries(
          Map.entry(GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_SCANNER_SURFACE_NOT_NORMA,
                  "SUPERFICIE DEL ESCANER SUCIA"),
          Map.entry(GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_SCANNER_FAILURE,
                  "FALLA EN EL ILUMINADOR"),
          Map.entry(GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_DRY_FINGER,
                  "DEDO SECO"),
          Map.entry(GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_WET_FINGER,
                  "DEDO HUMEDO"),
          Map.entry(GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_EXT_LIGHT_TOO_STRONG,
                  "LUZ EXTERNA DEMASIADO INTENSA"));

  public static List<String> getDiagsToDisplay(int diagnostic) {
    return DIAGNOSTIC_MESSAGES.entrySet().stream()
            .filter(entry -> (diagnostic & entry.getKey()) != Constants.ZERO)
            .map(Map.Entry::getValue).toList();
  }

  private static final Map<Byte, String> DEVICE_NAMES = new HashMap<>();

  static {
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_DS84, "DactyScan84");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_DN_DS84t, "DactyScan84t");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_DN_DSID20, "DactyID20");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_MS1000, "MultiScan1000");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_VS3, "Visascan3");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_PS2, "Poliscan2");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_DS40, "DactyScan40");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_DS40I, "DactyScan40I");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_DS26, "DactyScan26");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_MS500, "MC500");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_MSC500, "MSC500");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_DS84C, "DactyScan84C");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_MC517, "MC517");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_MSC517, "MSC517");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_DS32, "DactyScanS32");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_MS527, "MS527");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_DN_MS527t, "MS527t");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_DN_MS1027, "MS1027");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_DN_CS500F, "CS500F");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_DN_CS500Q, "CS500Q");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_DN_CSTFT50, "CSTFT50");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_DN_CSD101, "CSD101");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_DN_CS1000q, "CS1000Q");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_DS40p, "DS40p");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_DS32p, "DS32p");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_CSNOVA, "Nova");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_CSD201, "CSD201");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_DS84C_V2, "DS84C_V2");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_DS84t_V2, "DS84t_V2");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_CS500F_V2, "CS500F_V2");
    DEVICE_NAMES.put(GBMSAPI_JAVA_DeviceName.GBMSAPI_JAVA_DN_CS1000s, "CS1000s");
  }

  /**
   * Returns a human-readable name for a given GBMSAPI device ID.
   *
   * @param devId The device ID (Byte)
   * @return Device name if known, or "Unknown Device" otherwise.
   */
  public static String gbmsApiExampleGetDevNameFromDevID(Byte devId) {
    if (devId == null) {
      return "Unknown Device";
    }
    return DEVICE_NAMES.getOrDefault(devId, "Unknown Device");
  }

  public static String extractStringFromByteArray(byte[] binary) {
    return new String(binary);
  }

  public static byte[] getImageBytesFromFramePtr(Pointer framePtr, int width, int height) {
    try {
      return framePtr.getByteArray(0L, width * height);
    } catch (Exception ex) {
      return new byte[Constants.ZERO];
    }
  }

  public static void decodeEventFlags(int info) {
    var flags = new ArrayList<String>();
    if ((info & GBMSAPI_JAVA_EventInfo.GBMSAPI_JAVA_EI_ACQUISITION_PHASE) != Constants.ZERO)
      flags.add(Constants.FLAG_ACQUISITION_PHASE);
    if ((info & GBMSAPI_JAVA_EventInfo.GBMSAPI_JAVA_EI_STOP_TYPE) != Constants.ZERO)
      flags.add(Constants.FLAG_STOP_TYPE);
    if ((info & GBMSAPI_JAVA_EventInfo.GBMSAPI_JAVA_EI_PREVIEW_RES) != Constants.ZERO)
      flags.add(Constants.FLAG_PREVIEW_RES);
    if ((info & GBMSAPI_JAVA_EventInfo.GBMSAPI_JAVA_EI_ACQUISITION_TIMEOUT) != Constants.ZERO)
      flags.add(Constants.FLAG_ACQUISITION_TIMEOUT);
    if ((info & GBMSAPI_JAVA_EventInfo.GBMSAPI_JAVA_EI_ENCRYPTED_FRAME_AES_256) != Constants.ZERO)
      flags.add(Constants.FLAG_ENCRYPTED_FRAME_AES_256);
    if ((info & GBMSAPI_JAVA_EventInfo.GBMSAPI_JAVA_EI_IS_ISO_19794_2_2005_TEMPLATE) != Constants.ZERO)
      flags.add(Constants.FLAG_IS_ISO_TEMPLATE);
    if ((info & GBMSAPI_JAVA_EventInfo.GBMSAPI_JAVA_EI_FRAME_NOT_PRESENT) != Constants.ZERO)
      flags.add(Constants.FLAG_FRAME_NOT_PRESENT);
    log.debug("EventInfo flags: {}", flags.isEmpty() ? "none" : String.join(Constants.COMMA,
            flags));
  }

  public static boolean isDiagnosticAcceptable(int diag) {
    int mask = GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_SCANNER_FAILURE
            | GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_SCANNER_SURFACE_NOT_NORMA;
    return (diag & mask) == Constants.ZERO;
  }

  public static void throwIfError(int sdkResult) {
    if (sdkResult == GBMSAPI_JAVA_Defines
            .GBMSAPI_JAVA_ErrorCodesDefines
            .GBMSAPI_JAVA_ErrorCodes
            .GBMSAPI_JAVA_ERROR_CODE_NO_ERROR) {
      return;
    }

    ErrorCode errorCode = ErrorCode.fromCode(sdkResult);

    throw new AcquisitionException(errorCode);
  }


}
