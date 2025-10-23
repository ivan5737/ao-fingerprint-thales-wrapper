package com.fingerprint.thales.enums;

import com.fingerprint.thales.constants.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AcquisitionStatesEnum {

  IDLE(Constants.ZERO, Constants.DESC_IDLE),
  PREVIEW(Constants.ONE, Constants.DESC_PREVIEW),
  ACQUISITION(Constants.TWO, Constants.DESC_ACQUISITION),
  SCANNER_START(Constants.THREE, Constants.DESC_SCANNER_START),
  SCANNER_ERROR(Constants.FOUR, Constants.ERROR),
  ACQUISITION_END(Constants.FIVE, Constants.DESC_ACQUISITION_END);

  private final int code;

  private final String description;

  /**
   * 🔍 Devuelve el enum correspondiente o null si no existe
   */
  public static AcquisitionStatesEnum fromCode(int code) {
    for (var state : values()) {
      if (state.code == code) return state;
    }
    return null;
  }

  /**
   * 🔍 Devuelve descripción legible o "UNKNOWN" si el código no existe
   */
  public static String getAcquisitionStateString(int code) {
    var state = fromCode(code);
    return (state != null) ? state.getDescription() : Constants.UNKNOWN;
  }

  @Override
  public String toString() {
    return description;
  }
}
