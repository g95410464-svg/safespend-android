# SafeSpend

Aplicación Android para registrar manualmente gastos, ingresos, transferencias y suscripciones. También permite escanear recibos y guardar tarjetas como referencia local.

## Funciones

- Panel de saldo disponible, cuentas, actividad reciente y ritmo de gasto.
- Registro manual de ingresos, gastos y movimientos entre banco y efectivo.
- Escaneo local de recibos con la cámara.
- Bóveda de suscripciones con montos reservados.
- Referencias de tarjetas cifradas en el dispositivo y protegidas con biometría.
- Bloqueo biométrico al abrir la app y al regresar desde segundo plano.

SafeSpend no se conecta a bancos y no procesa pagos. Los saldos y movimientos son registros manuales del usuario. La tarjeta guardada es solo una referencia.

## Requisitos

- Android Studio Ladybug o más reciente.
- JDK 17.
- Android SDK Platform 35 y Build Tools 35.0.0.
- Un teléfono Android con huella o biometría facial fuerte para usar la bóveda y el bloqueo de acceso.

## Compilar

Abre esta carpeta en Android Studio y sincroniza Gradle, o ejecuta desde PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
./gradlew.bat assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar por ADB

Conecta el teléfono, habilita la depuración USB y ejecuta:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Borrar datos de prueba

En Android, abre **Ajustes → Aplicaciones → SafeSpend → Almacenamiento → Borrar datos**. Esto elimina movimientos, saldos, suscripciones, tarjetas guardadas y preferencias de la app de ese teléfono.

También puedes usar `adb shell pm clear com.safespend.app`; este comando tiene el mismo efecto.
