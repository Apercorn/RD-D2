# RD-D2

RD-D2 is the name of Optima Prime team #16044's FTC DECODE 2025-2026 season robot. The robot features a bootkicker intake and flywheel launcher to score artifacts.

---

## Table of Contents

- [RD-D2](#rd-d2)
	- [Table of Contents](#table-of-contents)
	- [Prerequisites](#prerequisites)
		- [Software](#software)
	- [Local Development Setup](#local-development-setup)
		- [1. Clone](#1-clone)
		- [2. Configurations](#2-configurations)
		- [3. Sync Gradle](#3-sync-gradle)
		- [4. Connect the control hub](#4-connect-the-control-hub)
		- [5. Build and install](#5-build-and-install)
	- [Project Structure](#project-structure)
	- [Hardware Map](#hardware-map)
	- [Robot Functions](#robot-functions)
		- [Drive (`core/Drive.kt`)](#drive-coredrivekt)
		- [Intake (`core/Intake.kt`)](#intake-coreintakekt)
		- [Shooter (`core/Shooter.kt`)](#shooter-coreshooterkt)
	- [TeleOp Controls](#teleop-controls)
		- [Drive](#drive)
		- [Intake](#intake)
		- [Flywheel / Shooter](#flywheel--shooter)
	- [Panels Dashboard](#panels-dashboard)
		- [Access URL](#access-url)
		- [Import the Team Layout](#import-the-team-layout)
		- [Configurable Variables](#configurable-variables)

---

## Prerequisites

### Software

| Tool        | Version          | Notes                                                                         |
| ----------- | ---------------- | ----------------------------------------------------------------------------- |
| Java JDK    | **17 or 21**     | Java 25 breaks the Kotlin LSP. Get it from [Adoptium](https://adoptium.net/). |
| Android SDK | Any recent       | Install via Android Studio or the standalone command-line tools.              |


---

## Local Development Setup

### 1. Clone

```bash
git clone https://github.com/Apercorn/RD-R2.git
cd RD-D2
```

### 2. Configurations

`local.properties`
```properties
# Linux / macOS
sdk.dir=/home/<you>/Android/Sdk

# Windows
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

### 3. Sync Gradle

```bash
./gradlew build
```

Gradle will download all dependencies on the first run (~300 MB).

### 4. Connect the control hub
You can connect to the control hub via cable or Wi-Fi. Make sure to open the REV Hardware Client to load the chip's drivers.

For the first time, you may need to run `adb connect`

```sh
adb connect 192.168.43.1:5555
```

After which, you can verify the device is connected using
```sh
adb devices
```

### 5. Build and install
Press <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>B</kbd> to run build and install on the Control Hub

---

## Project Structure

```
RD-D2/
├── CompetitionTeleOp.kt        # Main driver-controlled op-mode
├── CompetitionAuto.kt          # Autonomous op-mode
│
├── core/
│   ├── Robot.kt                # Hardware init & subsystem wiring
│   ├── Drive.kt                # Tank drive subsystem
│   ├── Intake.kt               # Intake motor + feeder servo
│   ├── Shooter.kt              # Flywheel + gate servos, PIDF constants
│   └── Vision.kt               # Camera / April Tag vision
│
├── auto/
│   ├── DriveConstants.kt       # Drive motor specs, velocity PIDF
│   ├── AutoConstants.kt        # Ticks-per-inch, drive power, PID gains
│   ├── AutoBuilder.kt          # Fluent autonomous sequence builder
│   └── AutoRunner.kt           # Executes AutoBuilder sequences
│
└── tuning/                     # Tuning op-modes
```

---

## Hardware Map

Configure these names in the Control Hub's Robot Configuration app:

| Config Name       | Type      | Subsystem           |
| ----------------- | --------- | ------------------- |
| `left_drive`      | DcMotorEx | Drive — left side   |
| `right_drive`     | DcMotorEx | Drive — right side  |
| `intake`          | DcMotorEx | Intake roller       |
| `outtake`         | DcMotorEx | Flywheel            |
| `gate_left`       | Servo     | Shooter gate, left  |
| `gate_right`      | Servo     | Shooter gate, right |
| `flywheel_feeder` | Servo     | Intake feeder sweep |

Motor directions are set in `Robot.kt` (`left_drive` reversed, `outtake` reversed).

---

## Robot Functions

### Drive (`core/Drive.kt`)

Tank-differential drive. The `tankDrive(drive, turn)` method converts a single forward axis and a single turn axis into left/right motor powers.

| Method                   | Description                                             |
| ------------------------ | ------------------------------------------------------- |
| `tankDrive(drive, turn)` | Standard tank drive from joystick axes                  |
| `setPower(left, right)`  | Raw motor power, bypasses multiplier                    |
| `stop()`                 | Zero both motors                                        |
| `precisionMode`          | When `true`, applies `0.35×` multiplier for fine aiming |

### Intake (`core/Intake.kt`)

Controls the intake roller motor and a feeder servo that sweeps artifacts into the robot.

| Method                | Description                                                    |
| --------------------- | -------------------------------------------------------------- |
| `start(scope?)`       | Run intake forward; optionally launch continuous feeder sweeps |
| `reverse()`           | Run intake in reverse                                          |
| `stop()`              | Stop motor, cancel feeder job                                  |
| `toggle(dir, scope?)` | Toggle on/off in given direction                               |
| `feed(scope)`         | Single feeder servo sweep                                      |
| `windmill`            | Property — when `true`, runs at `0.3` power instead of `1.0`   |

### Shooter (`core/Shooter.kt`)

Flywheel velocity controller + two gate servos. The flywheel runs in `RUN_USING_ENCODER` mode with a PIDF velocity loop. `shoot()` polls actual RPM and only opens the gate once the flywheel reaches within `RPM_TOLERANCE` of the target.

| Method                            | Description                                                    |
| --------------------------------- | -------------------------------------------------------------- |
| `longshot()`                      | Set target to `LONGSHOT_RPM` (2200) and spin up                |
| `shortshot()`                     | Set target to `SHORTSHOT_RPM` (1720) and spin up               |
| `windmill()`                      | Slow idle spin at `WINDMILL_RPM` (600) to keep flywheel warm   |
| `toggleWindmill()`                | Toggle between windmill and stopped                            |
| `increaseRpm()` / `decreaseRpm()` | Nudge target by `RPM_STEP` (100)                               |
| `shoot(scope, onGateOpen?)`       | Wait for target RPM, open gate, close after `SHOOT_TIMEOUT_MS` |
| `openGate()` / `closeGate()`      | Direct gate control                                            |
| `toggleGate()`                    | Toggle gate                                                    |
| `stopFlywheel()`                  | Stop flywheel, cancel any in-flight shoot job                  |
| `stop()`                          | Stop flywheel and close gate                                   |
| `flywheelRpm`                     | Read-only property: current RPM                                |
| `flywheelVelocity`                | Read-only property: current ticks/sec                          |

---

## TeleOp Controls

All controls are on **Gamepad 1**.

### Drive

| Input               | Action                              |
| ------------------- | ----------------------------------- |
| Left Stick Y        | Forward / backward                  |
| Right Stick X       | Turn left / right                   |
| Left Bumper (press) | Toggle precision mode (0.35× speed) |

### Intake

| Input                 | Action                                     |
| --------------------- | ------------------------------------------ |
| Right Trigger (press) | Toggle intake forward (runs feeder sweeps) |
| Left Trigger (press)  | Toggle intake reverse                      |
| Right Bumper (press)  | Single feeder sweep                        |

### Flywheel / Shooter

| Input       | Action                           |
| ----------- | -------------------------------- |
| D-Pad Up    | Spin up to longshot RPM          |
| D-Pad Down  | Spin up to shortshot RPM         |
| D-Pad Right | Increase target RPM by 100       |
| D-Pad Left  | Decrease target RPM by 100       |
| A (press)   | Toggle windmill idle on/off      |
| Y (press)   | Switch to windmill RPM           |
| X (press)   | **Fire**                         |
| B (press)   | Toggle gate open/closed manually |

---

## Panels Dashboard

[Panels](https://panels.bylazar.com/) is a web-based dashboard that runs on the Control Hub. It provides live telemetry graphs, on-robot variable editing, gamepad visualisation, and camera streaming — all without rebuilding the APK.

### Access URL

Connect your laptop/tablet to the Control Hub's WiFi network, then open:

```
http://192.168.43.1:8000
```

> If the Control Hub is connected to an external router instead, use its assigned IP on port 8000.

### Import the Team Layout

The repository ships with a pre-built dashboard layout in `.panels.json`. To load it:

1. Open `http://192.168.43.1:8000` in your browser.
2. Click the **settings / gear icon** in the top-right corner of Panels.
3. Select **Import layout**.
4. Upload `.panels.json` from the root of this repository.

The layout includes the following panels arranged in a 16-column grid:

| Position           | Panel                                             |
| ------------------ | ------------------------------------------------- |
| Top-left (5×5)     | Op-Mode Control + Timer                           |
| Bottom-left (5×7)  | First Gamepad visualiser + Camera Capture         |
| Centre (6×12)      | Field view + Graph + Limelight proxy              |
| Top-right (5×7)    | Telemetry + Configurables + Changed Configurables |
| Bottom-right (5×5) | Camera Stream + Telemetry                         |

### Configurable Variables

Any `companion object` field annotated with `@JvmField` inside a `@Configurable` class can be edited live from the **Configurables** panel without redeploying. Changes take effect immediately on the next loop cycle.

Classes that currently expose configurables:

- `FlywheelPIDFTuner` — `PIDF_F`, `PIDF_P`, `PIDF_I`, `PIDF_D`, `TARGET_RPM`
- `DriveConstants` — `RUN_USING_ENCODER`, `MOTOR_VELO_PID`
- `AutoConstants` — `TICKS_PER_INCH`, `DRIVE_POWER`, and PID gains

---

<!-- ## Flywheel Tuning

The flywheel uses the REV motor's built-in velocity PIDF controller. The motor runs in `RUN_USING_ENCODER` mode and you set a target velocity in ticks/sec; the controller tries to hold it. Tuning determines what gains make that controller accurate and responsive.

### Why the current constants are wrong

`tuning.csv` shows measured target vs actual RPM:

| Target RPM | Actual RPM | Ratio |
| ---------- | ---------- | ----- |
| 600        | 1050       | 1.75× |
| 1720       | 3100       | 1.80× |
| 2200       | 3900       | 1.77× |

Every setpoint overshoots by ~1.77×. This is a pure **F scaling error** — F was calculated from the desired max RPM (2200) rather than the motor's true free-running ceiling (~3900 RPM at 12.37 V). The corrected F is approximately `32767 / (3900 × 28 / 60) ≈ 18`.

### Step 1 — Measure true max TPS (`FlywheelFreeRunTuner`)

Run op-mode **"1. Flywheel Free-Run Tuner"** (Tuning group).

1. Connect a fully charged battery (12.2–12.5 V). Note the voltage.
2. Press **A** — flywheel spins at 100% power with no controller.
3. Watch **Max TPS** plateau after ~3 seconds.
4. Read **Suggested PIDF_F** from telemetry.
5. Press **A** to stop.
6. Copy the suggested value into `Shooter.PIDF_F`.

### Step 2 — Step-response tuning (`FlywheelPIDFTuner`)

Run op-mode **"2. Flywheel PIDF Tuner"** (Tuning group).

In the Panels **Configurables** panel, set:
- `PIDF_F` = value from Step 1
- `PIDF_P` = `0.0`
- `PIDF_I` = `0.0`
- `PIDF_D` = `0.0`
- `TARGET_RPM` = `2200.0` (or whatever shot you're tuning)

**Tuning F:**

Press **A** to command the step. Watch the **Graph** panel — it plots Target RPM, Actual RPM, and Error RPM.

- Actual overshoots target significantly → F still too high, reduce and press **A** again.
- Actual settles well below target → F too low, raise it.
- Actual ramps up and holds close to target at steady state → F is good.

**Tuning P:**

With F correct, increase `PIDF_P` in steps of 2–5. Press **X** to hot-reload the PIDF without stopping the motor. Flick a ball against the flywheel to simulate load; watch how quickly velocity recovers in the graph.

- Too low P: velocity sags and recovers slowly.
- Too high P: velocity oscillates (rings) after a disturbance.
- Stop just before oscillation appears.

**Leave I and D at 0** unless you see a persistent steady-state offset (add a small I) or sustained oscillation that P reduction doesn't fix (add a small D).

**Step 3 — Write back to code**

Update `Shooter.kt`:

```kotlin
const val PIDF_F = <your measured value>
const val PIDF_P = <your tuned value>
``` -->