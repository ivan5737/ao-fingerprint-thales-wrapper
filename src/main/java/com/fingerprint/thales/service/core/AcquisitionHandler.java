package com.fingerprint.thales.service.core;

import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_AcquisitionEvents;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_AcquisitionOptions;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_DiagnosticMessages;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_EventInfo;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_DeviceCharacteristicsDefines.GBMSAPI_JAVA_ScanObjectsUtilities;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_DeviceCharacteristicsDefines.GBMSAPI_JAVA_ScannableObjects;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_ErrorCodesDefines.GBMSAPI_JAVA_ErrorCodes;
import GBMSAPI_JAVA_LibraryFunctions.GBMSAPI_JAVA_AcquisitionEventsManagerCallbackInterface;
import GBMSAPI_JAVA_LibraryFunctions.GBMSAPI_JAVA_DLL_WRAPPER;
import com.fingerprint.thales.constants.Constants;
import com.fingerprint.thales.enums.AcquisitionStatesEnum;
import com.fingerprint.thales.exception.AcquisitionException;
import com.fingerprint.thales.model.ResponseOk;
import com.fingerprint.thales.utils.BiometricAdapter;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Inicia el proceso de adquisicion de huella.
 * Implementa los callbacks del SDK y devuelve un ResponseOk con la huella en Base64.
 */
@Slf4j
@Getter
public class AcquisitionHandler implements GBMSAPI_JAVA_AcquisitionEventsManagerCallbackInterface {

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

  private volatile long lastActivityTime = Constants.ZERO_LONG;

  /**
   * Inicia el proceso de adquisición de huella.
   *
   * @param timeout tiempo máximo de espera en milisegundos.
   * @return ResponseOk con la huella en base64 o null si no se obtuvo.
   */
  public ResponseOk captureFingerprint(Long timeout) {
    log.info("Iniciando adquisición de huella (timeout={}ms)...", timeout);
    resetAcquisitionState();

    objToScan = GBMSAPI_JAVA_ScanObjectsUtilities
            .GBMSAPI_JAVA_GetObjectToScanFromString(Constants.FLAT_RIGHT_INDEX);

    if (objToScan == GBMSAPI_JAVA_ScannableObjects.GBMSAPI_JAVA_SBT_NO_OBJECT) {
      throw new AcquisitionException(AcquisitionException.ErrorCode.INTERNAL_ERROR,
              new RuntimeException("Objeto a escanear no válido"));
    }

    int acqOptions = prepareAcquisitionOptions(objToScan);

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
    synchronized (this) {
      while (!acqEnded) {
        long sinceLastActivity = System.currentTimeMillis() - lastActivityTime;

        if (sinceLastActivity >= timeout) {
          stopAcquisition();
          resetAcquisitionState();
          throw new AcquisitionException(AcquisitionException.ErrorCode.ACQUISITION_TIMEOUT);
        }
        try {
          this.wait(Constants.POLLING_INTERVAL_MS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.error("La adquisición fue interrumpida: {}", e.getMessage());
          break;
        }
      }
    }

    log.info("Adquisición finalizada correctamente.");
    return getLastResponse() != null ? getLastResponse()
            : ResponseOk.builder().fingerprint(null).build();
  }

  /**
   * Inicia el polling periódico para verificar resultados de adquisición.
   */
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
      GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_SetSelectImageTimeout(Constants.ZERO);
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
    acqFrame = null;
    acqState = AcquisitionStatesEnum.IDLE.getCode();
    acqTimer = null;
    diagnosticsList.clear();
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

  // ========== CALLBACKS DEL SDK ==========

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
                lastResponse = handleAcquisitionEnd(eventInfo, framePtr, sizeX, sizeY, diagnostic);
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
      acqFrame = GbmsApiDeviceUtil.getImageBytesFromFramePtr(framePtr, width, height);
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
      acqFrame = GbmsApiDeviceUtil.getImageBytesFromFramePtr(framePtr, width, height);
    }

    acqState = AcquisitionStatesEnum.ACQUISITION_END.getCode();
    acqEnded = true;

    return ResponseOk.builder().fingerprint(base64Fingerprint).build();
  }

  private String extractIsoTemplate() {
    var sizeRef = new IntByReference();
    GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_GetTemplateBufferSize(sizeRef);
    var buffer = new Memory(sizeRef.getValue());
    var outSize = new IntByReference(sizeRef.getValue());
    int res = GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
            .GBMSAPI_GetIso19794_2_2005_Template(buffer, sizeRef.getValue(), outSize);

    if (res == GBMSAPI_JAVA_ErrorCodes.GBMSAPI_JAVA_ERROR_CODE_NO_ERROR) {
      byte[] template = buffer.getByteArray(Constants.ZERO, sizeRef.getValue());
      byte[] newFileBytes = BiometricAdapter.adapterINE(template);
      String base64 = Base64.getEncoder().encodeToString(newFileBytes);
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

    log.info("Configurando LED blink (diag={})", diag);
    if (diag != Constants.ZERO) {
      GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
              .GBMSAPI_VUI_LED_BlinkDuringAcquisition(Constants.ONE);
    } else {
      GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
              .GBMSAPI_VUI_LED_BlinkDuringAcquisition(Constants.ZERO);
      GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
              .GBMSAPI_SetAutoCaptureBlocking(Constants.ZERO);
    }
  }
}
