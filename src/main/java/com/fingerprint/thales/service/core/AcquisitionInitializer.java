package com.fingerprint.thales.service.core;

import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_DeviceCharacteristicsDefines.GBMSAPI_JAVA_DeviceInfoConstants;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_DeviceCharacteristicsDefines.GBMSAPI_JAVA_DeviceInfoStruct;
import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_AcquisitionProcessDefines.GBMSAPI_JAVA_FlatAutoCaptureModes;
import GBMSAPI_JAVA_LibraryFunctions.GBMSAPI_JAVA_DLL_WRAPPER;
import com.fingerprint.thales.constants.Constants;
import com.fingerprint.thales.exception.AcquisitionException;
import com.fingerprint.thales.jna.FixedDeviceInfoStruct;
import com.fingerprint.thales.utils.GbmsApiDeviceUtil;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Encargado de inicializar y configurar el dispositivo biométrico Thales.
 * Carga la librería GBMSAPI, detecta dispositivos conectados y deja el escáner listo.
 */
@Slf4j
@Getter
public class AcquisitionInitializer {

  private GBMSAPI_JAVA_DeviceInfoStruct[] structList;
  private String deviceType = Constants.NOT_AVAILABLE;
  private String deviceSerial = Constants.NOT_AVAILABLE;
  private int deviceScanOptions;
  private int deviceScannableTypes;
  private boolean obfuscatedPreviewSupported;

  /**
   * Inicializa completamente el SDK y el dispositivo.
   * Carga la librería, detecta dispositivos y configura el escáner.
   */
  public void initialize() {
    log.info("=== Inicializando SDK y dispositivo Thales ===");

    resetScannerState();
    try {
      initializeGbmsApi();
      refreshDeviceList();
      log.info("Inicialización de dispositivo completada correctamente.");
    } catch (AcquisitionException e) {
      resetScannerState();
      log.error("Error de inicialización: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      resetScannerState();
      log.error("Excepción inesperada durante la inicialización.", e);
      throw new AcquisitionException(AcquisitionException.ErrorCode.INTERNAL_ERROR, e);
    }
  }

  /**
   * Carga la librería GBMSAPI y muestra la versión detectada.
   */
  private void initializeGbmsApi() {
    log.info("Cargando librería GBMSAPI...");
    GbmsApiDeviceUtil.throwIfError(
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE.GBMSAPI_LoadLibrary()
                                  );

    var v1 = new ByteByReference();
    var v2 = new ByteByReference();
    var v3 = new ByteByReference();
    var v4 = new ByteByReference();

    GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
            .GBMSAPI_GetMultiScanAPIVersion(v1, v2, v3, v4);

    log.info("Versión SDK GBMSAPI: {}.{}.{}.{}", v1.getValue(), v2.getValue(), v3.getValue(), v4.getValue());
  }

  /**
   * Escanea los dispositivos conectados y configura el primero disponible.
   */
  private void refreshDeviceList() {
    log.info("Escaneando dispositivos conectados...");

    structList = (FixedDeviceInfoStruct[]) new FixedDeviceInfoStruct().toArray(
            GBMSAPI_JAVA_DeviceInfoConstants.GBMSAPI_JAVA_MAX_PLUGGED_DEVICE_NUM);

    var deviceCount = new IntByReference();
    var usbError = new IntByReference();

    GbmsApiDeviceUtil.throwIfError(
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
                    .GBMSAPI_GetAttachedDeviceList(structList, deviceCount, usbError)
                                  );

    int count = deviceCount.getValue();
    if (count == Constants.ZERO) {
      throw new AcquisitionException(AcquisitionException.ErrorCode.INIT_NO_DEVICES_FOUND);
    }

    log.info("Se detectaron {} dispositivo(s).", count);
    for (int i = Constants.ZERO; i < count; i++) {
      var devType = GbmsApiDeviceUtil.gbmsApiExampleGetDevNameFromDevID(structList[i].DeviceID);
      var serial = GbmsApiDeviceUtil.extractStringFromByteArray(structList[i].DeviceSerialNumber);
      log.info("Device[{}] TYPE: {}, SN: {}", i, devType, serial);
    }

    setupScanner(count);
  }

  /**
   * Configura y activa el primer dispositivo disponible.
   */
  private void setupScanner(int deviceCount) {
    if (deviceCount <= Constants.ZERO) {
      throw new AcquisitionException(AcquisitionException.ErrorCode.INIT_NO_DEVICES_FOUND);
    }

    deviceType = GbmsApiDeviceUtil.gbmsApiExampleGetDevNameFromDevID(
            structList[Constants.ZERO].DeviceID);
    deviceSerial = GbmsApiDeviceUtil.extractStringFromByteArray(
            structList[Constants.ZERO].DeviceSerialNumber);

    GbmsApiDeviceUtil.throwIfError(
            GBMSAPI_JAVA_DLL_WRAPPER.GBMSAPI_Library.INSTANCE
                    .GBMSAPI_SetCurrentDevice(structList[Constants.ZERO].DeviceID, deviceSerial)
                                  );

    var ref = new IntByReference();

    // Obtener características y opciones del dispositivo
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
      log.info("Modo OBFUSCATED_PREVIEW habilitado, resultado: {}", result);
    }

    log.info("Escáner inicializado exitosamente: {} ({})", deviceType, deviceSerial);
  }

  /**
   * Limpia el estado interno del escáner.
   */
  private void resetScannerState() {
    deviceType = deviceSerial = Constants.NOT_AVAILABLE;
    deviceScanOptions = deviceScannableTypes = Constants.ZERO;
    obfuscatedPreviewSupported = false;
    log.info("Estado del escáner reiniciado.");
  }
}
