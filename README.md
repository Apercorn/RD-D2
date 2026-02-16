# RD-D2

FTC Robot Controller project for team RD-D2, built with Kotlin and featuring Panels dashboard integration.

## Features

- **Kotlin-based TeleOp** - Modern, type-safe robot control code
- **PIDF Flywheel Control** - Velocity-based control with real-time RPM monitoring
- **Panels Dashboard** - Real-time telemetry, configuration, and debugging
- **RoadRunner Integration** - Advanced autonomous path planning
- **Custom Intake/Outtake System** - Toggle and oneshot operation modes

## Prerequisites

### Required Software

- **Java JDK 17 or 21** (LTS versions recommended)
  - Download from [Adoptium](https://adoptium.net/)
  - Note: Java 25 is not yet supported by the Kotlin Language Server
- **Android SDK** - Installed via Android Studio or command line tools
- **ADB (Android Debug Bridge)** - For deploying to Control Hub
- **VS Code** (recommended) or Android Studio

### VS Code Extensions

- **Kotlin Language** (fwcd.kotlin) - Kotlin language support
- **Gradle for Java** (vscjava.vscode-gradle) - Gradle build integration

## Local Development Setup

### 1. Clone the Repository

```bash
git clone https://github.com/antipoly/FtcRobotController.git RD-D2
cd RD-D2
```

### 2. Configure Java Version

If using VS Code, create or edit `.vscode/settings.json`:

```json
{
  "java.jdt.ls.java.home": "C:\\Program Files\\Java\\jdk-17",
  "kotlin.languageServer.javaHome": "C:\\Program Files\\Java\\jdk-17"
}
```

### 3. Sync Dependencies

```bash
./gradlew build
```

## Building the Project

### Command Line

```bash
# Build debug APK
./gradlew assembleDebug

# Clean build
./gradlew clean assembleDebug

# Build and install to connected device
./gradlew assembleDebug installDebug
```

### VS Code

Press **Ctrl+Shift+B** to run the default build task, or:

1. Open Command Palette (`Ctrl+Shift+P`)
2. Select **Tasks: Run Task**
3. Choose from:
   - **Build and Deploy to Control Hub** - Builds and installs to connected device
   - **Build APK** - Builds debug APK only
   - **Clean Build** - Cleans and rebuilds project

## Deploying to Control Hub

### Via WiFi (Wireless ADB)

1. Connect to the Control Hub's WiFi network
2. Enable wireless ADB in Control Hub settings
3. Connect via ADB:
   ```bash
   adb connect 192.168.43.1:5555
   ```
4. Deploy:
   ```bash
   ./gradlew installDebug
   ```
   Or press **Ctrl+Shift+B** in VS Code

### Via USB

1. Connect Control Hub via USB cable
2. Enable USB debugging in Control Hub settings
3. Verify connection:
   ```bash
   adb devices
   ```
4. Deploy using the same commands as WiFi

## Project Structure

```
RD-D2/
├── FtcRobotController/    # SDK robot controller module
├── TeamCode/              # Team-specific code
│   └── src/main/java/org/firstinspires/ftc/teamcode/
│       └── CompetitionTeleOp.kt    # Main TeleOp program
├── build.gradle           # Root build configuration
├── build.common.gradle    # Common build settings
├── build.dependencies.gradle  # Dependency management
└── .vscode/
    └── tasks.json         # VS Code build tasks
```

## Hardware Configuration

Expected hardware map names:

- `left_drive` - Left drive motor (DcMotorEx)
- `right_drive` - Right drive motor (DcMotorEx)
- `intake` - Intake motor (DcMotorEx)
- `outtake` - Flywheel motor (DcMotorEx)

## TeleOp Controls

### Driver 1 (Gamepad1)

- **Left Stick Y** - Drive forward/backward
- **Right Stick X** - Turn left/right
- **Right Trigger (press)** - Toggle intake (forward feeding)
- **Left Trigger (press)** - Toggle intake (reverse feeding)
- **A Button (press)** - Oneshot outtake (5 second timer)
- **B Button (press)** - Toggle outtake on/off

## PIDF Tuning

The flywheel uses velocity control with PIDF coefficients defined in `CompetitionTeleOp.kt`:

```kotlin
private val flywheelP = 10.0
private val flywheelI = 3.0
private val flywheelD = 0.0
private val flywheelF = 12.5
private val targetFlywheelVelocity = 2000.0  // ticks/second
```

### Tuning Steps

1. Start with default values
2. Adjust `flywheelP` - Increase if slow to reach target, decrease if oscillating
3. Add `flywheelI` if there's steady-state error
4. Calculate `flywheelF` (feedforward): `F = 32767 / max_velocity`
5. Modify `targetFlywheelVelocity` for desired shooting RPM
6. Use Panels dashboard to monitor real-time RPM and tuning effectiveness

## Panels Dashboard

This project includes [Panels](https://panels.bylazar.com/) for real-time dashboard and telemetry.

### Features

- Real-time telemetry display
- Gamepad support
- Live graphs and field visualization
- Camera streaming
- Configurable variables (tune on-the-fly)
- Battery and ping monitoring

### Accessing Panels

1. Deploy the app to Control Hub
2. Connect to Control Hub WiFi
3. Open browser to `http://192.168.43.1:8000` (or Control Hub IP)
4. Telemetry automatically syncs from your OpMode

## Dependencies

- **FTC SDK 11.0.0** - FIRST Tech Challenge SDK
- **Kotlin 1.9+** - Kotlin language support
- **Panels 1.0.5** - Real-time dashboard (includes all plugins)
- **RoadRunner 0.5.6** - Path planning library
- **FTC Dashboard 0.4.15** - Additional dashboard support

## Troubleshooting

### Build Failures

**File locked during build:**
```bash
# Stop Gradle daemons
./gradlew --stop

# Force clean
Remove-Item -Path "TeamCode\build" -Recurse -Force
./gradlew clean
```

### Kotlin Language Server Crashes

If you see "Java version 25" errors:
1. Install Java 17 or 21 (not Java 25)
2. Configure VS Code to use correct Java version (see setup above)
3. Reload VS Code window

### ADB Connection Issues

```bash
# Restart ADB server
adb kill-server
adb start-server

# For wireless: reconnect
adb connect 192.168.43.1:5555
```

## Resources

- [FTC SDK Documentation](https://github.com/FIRST-Tech-Challenge/FtcRobotController)
- [Panels Documentation](https://panels.bylazar.com/docs)
- [Kotlin for FTC](https://kotlinlang.org/)
- [RoadRunner Docs](https://learnroadrunner.com/)

## License

This project uses the FTC SDK which is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Team

Team RD-D2 - FIRST Tech Challenge

---

*Last updated: February 2026*
