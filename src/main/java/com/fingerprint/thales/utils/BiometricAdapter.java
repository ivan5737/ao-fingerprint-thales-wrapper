package com.fingerprint.thales.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BiometricAdapter {

  public static byte[] adapterINE(byte[] pAnsi) {
    int position = 2;
    if (pAnsi.length > 65535) {
      position = 6;
    }

    byte[] vendor = getBytesByUshort(49);
    pAnsi[position + 8] = vendor[0];
    pAnsi[position + 9] = vendor[1];
    byte[] subformat = getBytesByUshort(263);
    pAnsi[position + 10] = subformat[0];
    pAnsi[position + 11] = subformat[1];
    pAnsi[position + 12] = 0;
    pAnsi[position + 13] = 0;
    return pAnsi;
  }

  private static byte[] getBytesByUshort(int value) {
    if (value >= 0 && value <= 65535) {
      byte[] result = new byte[2];
      result[0] = (byte)(value >> 8 & 255);
      result[1] = (byte)(value & 255);
      return result;
    } else {
      throw new IllegalArgumentException("Valor fuera de rango ushort: " + value);
    }
  }

}
