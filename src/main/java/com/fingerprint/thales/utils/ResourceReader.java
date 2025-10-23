package com.fingerprint.thales.utils;

import com.fingerprint.thales.constants.Constants;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ResourceReader {

  public static String readResourceFile(String resourcePath) {
    try (InputStream inputStream =
                 ResourceReader.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        log.warn(Constants.FILE_NOT_FOUND, resourcePath);
        return Constants.MOCK_FINGERPRINT;
      }
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        return reader.lines().collect(Collectors.joining(Constants.NEWLINE));
      }
    } catch (Exception e) {
      log.error(Constants.ERROR_READING_FILE, resourcePath, e);
      return Constants.MOCK_FINGERPRINT;
    }
  }
}
