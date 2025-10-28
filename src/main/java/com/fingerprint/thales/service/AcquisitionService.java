package com.fingerprint.thales.service;

import com.fingerprint.thales.model.ResponseOk;
import com.fingerprint.thales.service.core.AcquisitionHandler;
import com.fingerprint.thales.service.core.AcquisitionInitializer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class AcquisitionService {

  private final AcquisitionInitializer initializer;

  private final AcquisitionHandler handler;

  /**
   * Constructor - initializes SDK Thales.
   */
  public AcquisitionService() {
    this.initializer = new AcquisitionInitializer();
    this.handler = new AcquisitionHandler();
    log.info("Inicializando SDK Thales...");
    initializer.initialize();
  }

  /**
   * Inicia el proceso de adquisición biométrica.
   *
   * @param timeout Tiempo máximo de espera en milisegundos.
   * @return ResponseOk con la huella capturada en Base64.
   */
  public ResponseOk startAcquisition(Long timeout) {
    log.info("Iniciando proceso de adquisición...");
    return handler.captureFingerprint(timeout);
  }

}