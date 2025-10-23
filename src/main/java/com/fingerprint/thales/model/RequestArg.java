package com.fingerprint.thales.model;

import com.fingerprint.thales.constants.Constants;
import lombok.Builder;

/**
 * Modelo de argumentos de solicitud.
 */
@Builder
public record RequestArg(
        Long timeout,
        int threshold,
        boolean isMock,
        boolean logsEnabled
) {

  /**
   * Crea una instancia de RequestArg a partir de un array de Strings.
   * El array puede contener hasta 4 elementos:
   * - args[0]: timeout en segundos (int, por defecto 30)
   * - args[1]: isMock (boolean, por defecto false)
   * - args[2]: logsEnabled (boolean, por defecto false)
   * Si algún valor no es válido o no está presente, se usa el valor por defecto.
   *
   * @param args Array de Strings con los parámetros.
   * @return Instancia de RequestArg con los valores parseados o por defecto.
   */
  public static RequestArg from(String[] args) {

    Long timeout = Constants.DEFAULT_TIMEOUT;

    int threshold = Constants.DEFAULT_THRESHOLD;

    boolean isMock = Boolean.FALSE;

    boolean logsEnabled = Constants.DEFAULT_LOGS_ENABLED;

    if (args != null) {
      if (args.length >= Constants.ONE)
        timeout = parseLongOrDefault(args[Constants.ZERO]);
      if (args.length >= Constants.TWO)
        threshold = parseIntOrDefault(args[Constants.ONE]);
      if (args.length >= Constants.THREE)
        isMock = parseBoolOrDefault(args[Constants.TWO]);
      if (args.length >= Constants.FOUR)
        logsEnabled = parseBoolOrDefault(args[Constants.THREE]);
    }

    return RequestArg.builder()
            .timeout(timeout)
            .threshold(threshold)
            .isMock(isMock)
            .logsEnabled(logsEnabled)
            .build();
  }

  /**
   * Parsea un String a Long, devolviendo un valor por defecto si falla.
   *
   * @param arg String a parsear.
   * @return Long parseado o valor por defecto.
   */
  private static Long parseLongOrDefault(String arg) {
    try {
      return Long.parseLong(arg.trim()) * Constants.MILLISECONDS;
    } catch (Exception ignored) {
      return Constants.DEFAULT_TIMEOUT;
    }
  }

  /**
   * Parsea un String a int, devolviendo un valor por defecto si falla.
   *
   * @param arg String a parsear.
   * @return int parseado o valor por defecto.
   */
  private static int parseIntOrDefault(String arg) {
    try {
      return Integer.parseInt(arg.trim());
    } catch (Exception ignored) {
      return Constants.DEFAULT_THRESHOLD;
    }
  }

  /**
   * Parsea un String a boolean, devolviendo un valor por defecto si es null o no reconocible.
   *
   * @param arg String a parsear.
   * @return boolean parseado o valor por defecto.
   */
  private static boolean parseBoolOrDefault(String arg) {
    if (arg == null) return Constants.DEFAULT_LOGS_ENABLED;
    String value = arg.trim().toLowerCase();
    if (Constants.TRUE_VALUES.contains(value))
      return Boolean.TRUE;
    if (Constants.FALSE_VALUES.contains(value))
      return Boolean.FALSE;
    return Constants.DEFAULT_LOGS_ENABLED;
  }

}
