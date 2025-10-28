# 🧬 AO Fingerprint Capture (Thales)

Proyecto Maven para capturar una huella con **GBMSAPI Thales**, que incluye:

- Captura de tipo `FLAT_SINGLE_FINGER`
- Escucha de eventos para obtener la imagen final
- Generación de **Base64** directamente desde el lector de huellas

---

## 📦 Librerías utilizadas

Este proyecto hace uso de las siguientes dependencias:

- 📦 **Lombok** (requiere habilitar *annotation processing* en tu IDE)
- 🔍 **SLF4J** como API de logging
- 📝 **Logback** como implementación de logging
- 📄 **Jackson** (`annotations`, `databind`) para serialización/deserialización JSON

---

## ⚙️ Requisitos

- ✅ Java 17+
- ✅ Maven 3.8+
- ✅ JAR de **GBMSAPI_JAVA_Wrapper** (incluido en `resources/libs`)

  > 💡 *Este JAR no están en Maven Central. Debes instalarlos manualmente en tu repositorio local:*

  ```bash
  mvn install:install-file -Dfile=src/main/resources/libs/GBMSAPI_JAVA_Wrapper.jar -DgroupId=com.gbms -DartifactId=gbmsapi-java-wrapper -Dversion=1.0 -Dpackaging=jar

- DLLs de **csd101_gb_wrapper** / **csd101-sdk_1.7.1** / **GBMSAPI** / **libcrypto-1_1** /
  **TFFD_csd201i** (incluidos en `resources/dll`).

---

## ⚠️ Requisito previo: agregar ruta del SDK al PATH del sistema

  ```text
  🪟 En Windows:
      1. Abre el menú de inicio y busca:
          “Editar las variables de entorno del sistema”
          o
          “Environment Variables”

      2. En la sección Variables del sistema, selecciona la variable llamada Path y haz clic en Editar.
            
      3. Agrega la siguiente ruta (ajústala si tu instalación es diferente):
          C:\ProgramData\ScannerKit\AgentFingerPrint\resources\DLL-Thales

      4. Haz clic en Aceptar en todas las ventanas y reinicia tu terminal o IDE (IntelliJ, Eclipse, etc.) para que se apliquen los cambios.
  ```

---

## 🚀 Ejecución

Compila y ejecuta desde línea de comandos:

  ```bash
    mvn clean package
    
    java -jar target/ao-thales-fingerprint-1.0.0.jar 30 30 false true
  ```

### Parámetros

| Posición | Descripción                                | Ejemplo | Valor default |
|----------|--------------------------------------------|---------|---------------|
| 1        | ⏱️ Timeout (en segundos)                   | 30      | 30            |
| 2        | 📈 Threshold (umbral de calidad)           | 30      | 30            |
| 3        | 🧪 isMock (true/false) - solo para pruebas | false   | false         |
| 4        | 🪵 LogsEnabled (true/false)                | true    | false         |

- Si no se pasan parámetros, se usan los valores por default.

---

## 📤 📝 Contrato de la Interfaz (CLI)

### ✅ Salida exitosa

**Exit code:** 0  
**STDOUT:** JSON con el template en Base64.

 ```json lines
{
  "Fingerprint": "Rk1SACAyMAABHE/…(Base64)…"
}
 ```

> 📝 **Nota**:  
> **Fingerprint** es el template biométrico en Base64 (no es imagen).
>
> El contenido corresponde a un template ANSI-378.

### ❌ Respuesta de Error

**Exit code:** ≠ 0  
**STDOUT:** JSON con mensaje IDEMIA mapeado.

```json lines
{
  "Error": "<mensaje IDEMIA (ENUM), código de error: <code>>"
}
```

- Los detalles técnicos (tipo de excepción, código Thales/propio, stack trace) no viajan en el
  response: quedan en logs.

#### ❌ Ejemplos respuesta de error

```json lines
{
  "Error": "No hay respuesta tras el tiempo definido (TIMEOUT), código de error: 19"
}
```

```json lines
{
  "Error": "El dispositivo USB no esta conectado (INIT_NO_DEVICES_FOUND), código de error: 42"
}
```

- El texto incluye:
    - Descripción IDEMIA (en español)
    - El enum original de la exception de Thales Acquisition (por ejemplo, TIMEOUT)
    - El código IDEMIA (por ejemplo, 19)

#### 🔍 Depuración de errores (STDERR)

- `logsEnabled=true` (parámetro 4), se genera un log detallado en la consola.

    - AcquisitionException
    - `type`, `code` (Thales o propios -20 a 254)
    - `message`
    - stack trace

``` ini
[AcquisitionException] type=TIMEOUT | code=-10 | msg=No Green Bit devices detected, …
```

---

## 📋 Mapeo de errores (Thales → IDEMIA)

La API publica el mensaje de error usando la semántica de IDEMIA.

La siguiente tabla muestra cómo se mapean los errores del SDK Thales a un código +
descripción de IDEMIA.

<details>
  <summary>Ver tabla completa de mapeo (Thales → IDEMIA)</summary>

</details>

---


## ⚙️ Flujo General
```text
ApplicationThales.main()
│
▼
AcquisitionService.startAcquisition(timeout)
│
├── [1] AcquisitionInitializer.initialize()
│ ├── Carga librería GBMSAPI
│ ├── Escanea dispositivo
│ ├── Configura primer escáner disponible
│ └── Deja SDK listo para capturar
│
└── [2] AcquisitionHandler.captureFingerprint(timeout)
├── Configura opciones de adquisición
├── Llama a GBMSAPI_StartAcquisition(...)
│
├── [3] SDK llama invoke() (callback)
│ ├── Evento: SCANNER_STARTED
│ ├── Evento: FRAME_ACQUIRED → genera bytes imagen
│ ├── Evento: PREVIEW_PHASE_END
│ ├── Evento: ACQUISITION_END → genera ResponseOk
│ └── Evento: ACQUISITION_ERROR → lanza excepción
│
├── Monitoreo con TimerTask + timeout
├── Procesamiento de diagnósticos (LEDs / Fake Finger)
└── Retorna ResponseOk(fingerprintBase64)

```
---
## 🔍 Diagrama Lógico Simplificado
```text
+---------------------------------------------------+
|                ApplicationThales                  |
|---------------------------------------------------|
|  - Ejecuta servicio y muestra resultado            |
+--------------------------┬------------------------+
                           │
                           ▼
+---------------------------------------------------+
|              AcquisitionService                   |
|---------------------------------------------------|
|  + startAcquisition(Long timeout): ResponseOk     |
|     ├─ initializer.initialize()                   |
|     └─ handler.captureFingerprint(timeout)        |
+--------------------------┬------------------------+
                           │
            +--------------┼------------------+
            │                                   │
            ▼                                   ▼
+----------------------------+     +-----------------------------+
|   AcquisitionInitializer   |     |     AcquisitionHandler      |
|----------------------------|     |-----------------------------|
|  - initialize()            |     |  - captureFingerprint()     |
|  - refreshDeviceList()     |     |  - invoke() (callbacks)     |
|  - setupScanner()          |     |  - handleFrameAcquired()    |
|                            |     |  - handleAcquisitionEnd()   |
|  (Carga y configura SDK)   |     |  (Ejecuta adquisición real) |
+----------------------------+     +-----------------------------+
```
---

## 🧠 Nota final

Asegúrate de que las DLLs estén disponibles ya sea:

- En una ruta incluida en la variable de entorno PATH, o
- En la misma carpeta desde la que ejecutas el .jar

---

### 🧪 Sobre el modo `isMock`

El argumento `isMock` (posición 3) permite **probar el flujo sin necesidad del lector de huellas**.  
Cuando se activa (`true`), la aplicación retorna un valor simulado (fingerprint en Base64) desde un
archivo en `resources`.  
Este modo es útil para pruebas de integración, mock de API, o verificación visual del JSON de
salida.

---