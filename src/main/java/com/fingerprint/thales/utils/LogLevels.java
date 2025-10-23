package com.fingerprint.thales.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import lombok.NoArgsConstructor;
import org.slf4j.LoggerFactory;

/**
 * Utilidad para configurar los niveles de log en tiempo de ejecución.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class LogLevels {

  /**
   * Configura el nivel de log del logger raíz.
   *
   * @param logsEnabled Si es true, establece el nivel en INFO; si es false, en WARN.
   */
  public static void apply(boolean logsEnabled) {
    LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
    var root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    root.setLevel(logsEnabled ? Level.INFO : Level.WARN);
  }

}
