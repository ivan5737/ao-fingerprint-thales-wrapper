package com.fingerprint.thales.service;

import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_AcquisitionEvents;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_AcquisitionOptions;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_DiagnosticMessages;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_EventInfo;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_FlatAutoCaptureModes;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_DeviceCharacteristicsDefines.GBMSAPI_JAVA_DeviceInfoConstants;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_DeviceCharacteristicsDefines.GBMSAPI_JAVA_DeviceInfoStruct;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_DeviceCharacteristicsDefines.GBMSAPI_JAVA_ScanObjectsUtilities;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_DeviceCharacteristicsDefines.GBMSAPI_JAVA_ScannableObjects;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_ErrorCodesDefines.GBMSAPI_JAVA_ErrorCodes;
import GBMSAPI_JAVA_LibraryFunctions.GBMSAPI_JAVA_AcquisitionEventsManagerCallbackInterface;
import GBMSAPI_JAVA_LibraryFunctions.GBMSAPI_JAVA_DLL_WRAPPER;
import com.fingerprint.thales.constants.Constants;
import com.fingerprint.thales.enums.AcquisitionStatesEnum;
import com.fingerprint.thales.exception.AcquisitionException;
import com.fingerprint.thales.jna.FixedDeviceInfoStruct;
import com.fingerprint.thales.model.ResponseOk;
import com.fingerprint.thales.utils.GbmsApiDeviceUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class AcquisitionService implements GBMSAPI_JAVA_AcquisitionEventsManagerCallbackInterface {

  //=== Device Information ===
  private GBMSAPI_JAVA_DeviceInfoStruct[] structList;
  private String deviceType = Constants.NOT_AVAILABLE;
  private String deviceSerial = Constants.NOT_AVAILABLE;
  private int deviceScanOptions;
  private int deviceScannableTypes;
  private boolean obfuscatedPreviewSupported;

  // === Acquisition State ===
  private boolean acqBusy;
  private boolean frameReady;
  private boolean acqEnded;
  private int acqState;
  private int acqContrast;
  private int acqDiagnostics;
  private int acqOldDiagnostic;

  // === Object to Scan ===
  private int objToScan = GBMSAPI_JAVA_ScannableObjects.GBMSAPI_JAVA_SBT_NO_OBJECT;

  // === Acquisition Data ===
  private byte[] acqFrame;
  private Timer acqTimer;

  // === Diagnostics ===
  private final List<String> diagnosticsList = new ArrayList<>();

  // === Last Response ===
  private ResponseOk lastResponse;

  //
  private volatile long lastActivityTime = Constants.ZERO_LONG;

  /**
   * Constructor - initializes SDK and refreshes device list.
   */
  public AcquisitionService() {
    initialize();
  }

  /**
   * Initializes the SDK and prepares the first available device.
   */
  private void initialize() {
    log.info("=== Initializing Acquisition Service ===");

    resetScannerState(); // Garantiza que arranque limpio
    acqState = AcquisitionStatesEnum.IDLE.getCode();

    try {
      initializeGbmsApi();
      refreshDeviceList();
      acqState = AcquisitionStatesEnum.PREVIEW.getCode();
      log.info("=== Initialization completed successfully ===");
    } catch (AcquisitionException ex) {
      acqState = AcquisitionStatesEnum.IDLE.getCode();
      resetScannerState();
      throw ex;
    } catch (Exception ex) {
      acqState = AcquisitionStatesEnum.IDLE.getCode();
      resetScannerState();
      throw new AcquisitionException(AcquisitionException.ErrorCode.INTERNAL_ERROR, ex);
    }
  }

  /**
   * Loads and validates the GBMSAPI SDK.
   */
  private void initializeGbmsApi() {
    log.info("Loading GBMSAPI library...");
    GbmsApiDeviceUtil.throwIfError(
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_LoadLibrary());
    var v1 = new ByteByReference();
    var v2 = new ByteByReference();
    var v3 = new ByteByReference();
    var v4 = new ByteByReference();
    GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_GetMultiScanAPIVersion(v1, v2, v3,
            v4);

    log.info("GBMSAPI SDK version: {}.{}.{}.{}", v1.getValue(), v2.getValue(), v3.getValue(),
            v4.getValue());
  }

  /**
   * Scans connected devices and initializes the first one.
   */
  private void refreshDeviceList() {
    log.info("Scanning for devices...");

    structList = (FixedDeviceInfoStruct[]) new FixedDeviceInfoStruct().toArray(
            GBMSAPI_JAVA_DeviceInfoConstants.GBMSAPI_JAVA_MAX_PLUGGED_DEVICE_NUM);

    var deviceCount = new IntByReference();
    var usbError = new IntByReference();

    GbmsApiDeviceUtil.throwIfError(
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
                    .GBMSAPI_GetAttachedDeviceList(structList, deviceCount, usbError));

    int count = deviceCount.getValue();
    if (count == Constants.ZERO) {
      throw new AcquisitionException(AcquisitionException.ErrorCode.INIT_NO_DEVICES_FOUND);
    }

    log.info("Detected {} device(s).", count);
    for (int i = Constants.ZERO; i < count; i++) {
      var devType = GbmsApiDeviceUtil.gbmsApiExampleGetDevNameFromDevID(structList[i].DeviceID);
      var serial = GbmsApiDeviceUtil.extractStringFromByteArray(structList[i].DeviceSerialNumber);
      log.info("Device[{}] TYPE: {}, SN: {}", i, devType, serial);
    }

    initializeSelectedDevice(count);
  }

  /**
   * Sets up the selected scanner device.
   */
  private void initializeSelectedDevice(int deviceCount) {
    log.info("Initializing selected device...");
    resetScannerState();
    setupScanner(deviceCount);
    log.info("Scanner initialized successfully: {} ({})", deviceType, deviceSerial);
  }

  /**
   * Configures scanner settings for the given device.
   */
  private void setupScanner(int deviceCount) {
    if (deviceCount <= Constants.ZERO) {
      throw new AcquisitionException(AcquisitionException.ErrorCode.INIT_NO_DEVICES_FOUND);
    }

    deviceType = GbmsApiDeviceUtil.gbmsApiExampleGetDevNameFromDevID(
            structList[Constants.ZERO].DeviceID);
    deviceSerial = GbmsApiDeviceUtil.extractStringFromByteArray(
            structList[Constants.ZERO].DeviceSerialNumber);

    // Seleccionar dispositivo
    GbmsApiDeviceUtil.throwIfError(
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
                    .GBMSAPI_SetCurrentDevice(structList[Constants.ZERO].DeviceID, deviceSerial));

    var ref = new IntByReference();

    // Obtener características y opciones
    GbmsApiDeviceUtil.throwIfError(
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_GetDeviceFeatures(ref));
    GbmsApiDeviceUtil.throwIfError(
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_GetSupportedScanOptions(ref));
    deviceScanOptions = ref.getValue();

    // Tipos escaneables
    GbmsApiDeviceUtil.throwIfError(
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_GetScannableTypes(ref));
    deviceScannableTypes = ref.getValue();

    // Verificar modo Obfuscated Preview
    GbmsApiDeviceUtil.throwIfError(
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
                    .GBMSAPI_FlatAutoCaptureModeIsSupported(
                            GBMSAPI_JAVA_FlatAutoCaptureModes.GBMSAPI_FAM_OBFUSCATED_PREVIEW, ref)
                                  );

    obfuscatedPreviewSupported = ref.getValue() != Constants.ZERO;
    if (obfuscatedPreviewSupported) {
      int result = GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
              .GBMSAPI_SetFlatAutoCaptureMode(
                      GBMSAPI_JAVA_FlatAutoCaptureModes.GBMSAPI_FAM_OBFUSCATED_PREVIEW);
      log.info("FlatAutoCaptureMode (OBFUSCATED_PREVIEW) result: {}", result);
    }

    acqDiagnostics = acqOldDiagnostic = Constants.ZERO;
    objToScan = GBMSAPI_JAVA_ScannableObjects.GBMSAPI_JAVA_SBT_NO_OBJECT;
  }

  /**
   * Resets all scanner-related fields to a clean state.
   */
  private void resetScannerState() {
    deviceType = deviceSerial = Constants.NOT_AVAILABLE;
    deviceScanOptions = deviceScannableTypes = Constants.ZERO;
    obfuscatedPreviewSupported = Boolean.FALSE;
    diagnosticsList.clear();
    acqState = AcquisitionStatesEnum.IDLE.getCode();
    log.info("Scanner state reset.");
  }


  /**
   * Inicia el proceso de adquisición de huella.
   */
  public ResponseOk startAcquisition(Long timeout) {
    int acqOptions;
    log.info("Iniciando adquisición con timeout de {} milisegundos", timeout);

    if (acqState != AcquisitionStatesEnum.IDLE.getCode()) {
      log.warn("No se puede iniciar adquisición — estado actual: {}",
              AcquisitionStatesEnum.getAcquisitionStateString(acqState));
    }

    objToScan = GBMSAPI_JAVA_ScanObjectsUtilities
            .GBMSAPI_JAVA_GetObjectToScanFromString("FLAT_RIGHT_INDEX");

    if (objToScan == GBMSAPI_JAVA_ScannableObjects.GBMSAPI_JAVA_SBT_NO_OBJECT) {
      throw new AcquisitionException(AcquisitionException.ErrorCode.INTERNAL_ERROR,
              new RuntimeException("Objeto a escanear no válido"));
    }

    resetAcquisitionState();
    acqOptions = prepareAcquisitionOptions(objToScan);

    int result = GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_StartAcquisition(
            objToScan, acqOptions, this, Pointer.NULL,
            Constants.ZERO, Constants.ZERO_B, Constants.ZERO_B);

    if (result != GBMSAPI_JAVA_ErrorCodes.GBMSAPI_JAVA_ERROR_CODE_NO_ERROR) {
      resetAcquisitionState();
      GbmsApiDeviceUtil.throwIfError(result);
    }

    acqState = AcquisitionStatesEnum.SCANNER_START.getCode();
    startResultPolling();

    lastActivityTime = System.currentTimeMillis();
    while (!acqEnded) {
      long sinceLastActivity = System.currentTimeMillis() - lastActivityTime;

      if (sinceLastActivity >= timeout) {
        stopAcquisition();
        resetAcquisitionState();
        throw new AcquisitionException(AcquisitionException.ErrorCode.ACQUISITION_TIMEOUT);
      }

      try {
        Thread.sleep(Constants.POLLING_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("La adquisición fue interrumpida: {}", e.getMessage());
        break;
      }
    }

    log.info("Adquisición finalizada correctamente.");
    return lastResponse != null ? lastResponse : ResponseOk.builder().fingerprint(null).build();
  }

  private void startResultPolling() {
    acqTimer = new Timer();
    acqTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        if (acqState != AcquisitionStatesEnum.IDLE.getCode()) {
          handleAcquisitionResults();
          if (acqEnded && acqTimer != null) acqTimer.cancel();
        }
      }
    }, 100, 100);
    log.info("Polling de adquisición iniciado.");
  }

  private int prepareAcquisitionOptions(int objToScan) {
    int options = Constants.ZERO;
    int objTypeMask = GBMSAPI_JAVA_ScanObjectsUtilities.GBMSAPI_JAVA_GetTypeFromObject(objToScan);

    if (GBMSAPI_JAVA_ScanObjectsUtilities.IsFlatType(objTypeMask)) {
      options |= GBMSAPI_JAVA_AcquisitionOptions.GBMSAPI_JAVA_AO_AUTOCAPTURE;
      GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
              .GBMSAPI_SetSelectImageTimeout(Constants.ZERO);
    }

    GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
            .GBMSAPI_SetMembraneUsageForFakeFingerDetection(Constants.ONE);
    GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
            .GBMSAPI_EnableAutoCaptureBlockForDetectedFakes(Constants.ZERO_B);

    return options;
  }

  private void resetAcquisitionState() {
    acqDiagnostics = acqOldDiagnostic = Constants.ZERO;
    acqBusy = frameReady = acqEnded = false;
    acqContrast = Constants.ZERO;
    setAcqFrame(null);
    acqState = AcquisitionStatesEnum.IDLE.getCode();
    acqTimer = null;
    log.info("Estado de adquisición reiniciado.");
  }

  private void handleAcquisitionResults() {
    if (acqBusy) return;

    try {
      acqBusy = true;
      if (frameReady) {
        frameReady = false;
        if (acqDiagnostics != acqOldDiagnostic) {
          processDiagnostics(acqDiagnostics);
          acqOldDiagnostic = acqDiagnostics;
        }
      }

      if (acqState == AcquisitionStatesEnum.ACQUISITION_END.getCode()
              || acqState == AcquisitionStatesEnum.SCANNER_ERROR.getCode()) {
        acqState = AcquisitionStatesEnum.IDLE.getCode();
      }
    } catch (Exception ex) {
      log.error("Error procesando resultados de adquisición: {}", ex.getMessage(), ex);
      stopAcquisition();
    } finally {
      acqBusy = false;
    }
  }

  private void stopAcquisition() {
    GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_StopAcquisition();
  }

  @Override
  public int invoke(int eventCode, int frameError, int eventInfo, Pointer framePtr,
                    int sizeX, int sizeY, double currentRate, double nominalRate,
                    int diagnostic, Pointer userParams) {
    acqBusy = true;
    frameReady = false;

    try {
      switch (eventCode) {
        case GBMSAPI_JAVA_AcquisitionEvents.GBMSAPI_JAVA_AE_SCANNER_STARTED ->
                handleScannerStarted();
        case GBMSAPI_JAVA_AcquisitionEvents.GBMSAPI_JAVA_AE_VALID_FRAME_ACQUIRED ->
                handleFrameAcquired(framePtr, sizeX, sizeY, diagnostic);
        case GBMSAPI_JAVA_AcquisitionEvents.GBMSAPI_JAVA_AE_PREVIEW_PHASE_END ->
                handlePreviewPhaseEnd();
        case GBMSAPI_JAVA_AcquisitionEvents.GBMSAPI_JAVA_AE_ACQUISITION_END ->
                setLastResponse(handleAcquisitionEnd(eventInfo, framePtr, sizeX, sizeY,
                        diagnostic));
        case GBMSAPI_JAVA_AcquisitionEvents.GBMSAPI_JAVA_AE_ACQUISITION_ERROR ->
                handleAcquisitionError(frameError);
        default -> log.warn("Evento no manejado: {}", eventCode);
      }
      return Constants.ONE;
    } catch (Exception ex) {
      log.error("Excepción en invoke(): {}", ex.getMessage(), ex);
      return Constants.ZERO;
    } finally {
      acqBusy = false;
    }
  }

  private void handleScannerStarted() {
    log.info("Evento: SCANNER_STARTED");
    acqState = AcquisitionStatesEnum.SCANNER_START.getCode();
  }

  private void handlePreviewPhaseEnd() {
    log.info("Evento: PREVIEW_PHASE_END");
    acqState = AcquisitionStatesEnum.ACQUISITION.getCode();
  }

  private void handleFrameAcquired(Pointer framePtr, int width, int height, int diagnostic) {
    var contrastRef = new ByteByReference();
    GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_GetFingerprintContrast(contrastRef);
    acqContrast = Byte.toUnsignedInt(contrastRef.getValue());

    if (acqContrast > Constants.ZERO) {
      lastActivityTime = System.currentTimeMillis();
    }

    log.info("Frame adquirido ({}x{}), contraste={}", width, height, acqContrast);

    if (framePtr != null && width > Constants.ZERO && height > Constants.ZERO) {
      setAcqFrame(GbmsApiDeviceUtil.getImageBytesFromFramePtr(framePtr, width, height));
      frameReady = true;
    }

    acqDiagnostics = diagnostic;
    acqState = AcquisitionStatesEnum.PREVIEW.getCode();
  }

  private ResponseOk handleAcquisitionEnd(int eventInfo, Pointer framePtr, int width, int height,
                                          int diagnostic) {
    String base64Fingerprint = null;
    log.info("Evento: ACQUISITION_END");
    GbmsApiDeviceUtil.decodeEventFlags(eventInfo);

    boolean finalize = GbmsApiDeviceUtil.isDiagnosticAcceptable(diagnostic);
    if ((eventInfo & GBMSAPI_JAVA_EventInfo.GBMSAPI_JAVA_EI_IS_ISO_19794_2_2005_TEMPLATE) != Constants.ZERO) {
      base64Fingerprint = extractIsoTemplate();
    }

    if (finalize && framePtr != null) {
      GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_ImageFinalization(framePtr);
      setAcqFrame(GbmsApiDeviceUtil.getImageBytesFromFramePtr(framePtr, width, height));
    }

    acqState = AcquisitionStatesEnum.ACQUISITION_END.getCode();
    acqEnded = true;

    if (base64Fingerprint != null) {
      return ResponseOk.builder().fingerprint(base64Fingerprint).build();
    } else {
      return ResponseOk.builder().fingerprint(null).build();
    }
  }

  private String extractIsoTemplate() {
    var sizeRef = new IntByReference();
    GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_GetTemplateBufferSize(sizeRef);
    var buffer = new Memory(sizeRef.getValue());
    var outSize = new IntByReference(sizeRef.getValue());
    int res =
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_GetIso19794_2_2005_Template(buffer, sizeRef.getValue(), outSize);

    if (res == GBMSAPI_JAVA_ErrorCodes.GBMSAPI_JAVA_ERROR_CODE_NO_ERROR) {
      byte[] template = buffer.getByteArray(Constants.ZERO, sizeRef.getValue());
      String base64 = Base64.getEncoder().encodeToString(template);
      log.info("ISO Template generado correctamente ({} bytes)", outSize.getValue());
      return base64;
    } else {
      log.error("Error generando ISO Template: {}", res);
      return null;
    }
  }

  private void handleAcquisitionError(int errorCode) {
    acqState = AcquisitionStatesEnum.SCANNER_ERROR.getCode();
    acqEnded = true;
    try {
      GbmsApiDeviceUtil.throwIfError(errorCode);
    } catch (AcquisitionException ex) {
      log.error("Error en adquisición: {}", ex.getMessage());
    }
  }

  private void processDiagnostics(int diag) {
    var newDiags = GbmsApiDeviceUtil.getDiagsToDisplay(diag);
    for (var newDiag : newDiags) {
      if (!diagnosticsList.contains(newDiag)) {
        diagnosticsList.add(newDiag);
        log.warn("Nuevo diagnóstico: {}", newDiag);
      }
    }

    diag &= ~(GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_VSROLL_ROLL_DIRECTION_DOWN
            | GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_VSROLL_ROLL_DIRECTION_LEFT
            | GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_VSROLL_ROLL_DIRECTION_RIGHT
            | GBMSAPI_JAVA_DiagnosticMessages.GBMSAPI_JAVA_DM_VSROLL_ROLL_DIRECTION_UP);

    log.info("Configurando LED blink para diagnósticos (diag={})", diag);
    if (diag != Constants.ZERO) {
      GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_VUI_LED_BlinkDuringAcquisition(Constants.ONE);
    } else {
      GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_VUI_LED_BlinkDuringAcquisition(Constants.ZERO);
      GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_SetAutoCaptureBlocking(Constants.ZERO);
    }
  }

}