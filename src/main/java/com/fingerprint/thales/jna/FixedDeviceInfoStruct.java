package com.fingerprint.thales.jna;

import GBMSAPI_JAVA_Defines.GBMSAPI_JAVA_DeviceCharacteristicsDefines.GBMSAPI_JAVA_DeviceInfoStruct;
import java.util.Arrays;
import java.util.List;

public class FixedDeviceInfoStruct extends GBMSAPI_JAVA_DeviceInfoStruct {

  @Override
  protected List<String> getFieldOrder() {
    return Arrays.asList("DeviceID", "DeviceSerialNumber");
  }
}
