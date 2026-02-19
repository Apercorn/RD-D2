package org.firstinspires.ftc.teamcode

import com.bylazar.configurables.annotations.Configurable
import com.bylazar.telemetry.JoinedTelemetry
import com.bylazar.telemetry.PanelsTelemetry
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorSimple
import kotlinx.coroutines.*
import org.firstinspires.ftc.teamcode.core.Robot

@Configurable
@TeleOp(name = "Competition TeleOp", group = "Competition")
class CompetitionTeleOp : OpMode() {
  private val tm = JoinedTelemetry(PanelsTelemetry.ftcTelemetry, telemetry)
  private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  private lateinit var robot: Robot

  override fun init() {
    robot = Robot(hardwareMap)

    tm.addData("Status", "TeleOp Initialized")
    tm.update()
  }

  fun handleDrive() {
    val drive = -gamepad1.left_stick_y.toDouble()
    val turn = gamepad1.right_stick_x.toDouble()

    if (gamepad1.leftBumperWasPressed()) {
      robot.drive.precisionMode = !robot.drive.precisionMode
    }

    robot.drive.arcadeDrive(drive, turn)
  }

  fun handleIntake() {
    // Feed the artifact
    if (gamepad1.rightTriggerWasPressed()) {
      robot.intake.toggle(DcMotorSimple.Direction.FORWARD)
    }

    // Reverse feed the artifact
    if (gamepad1.leftTriggerWasPressed()) {
      robot.intake.toggle(DcMotorSimple.Direction.REVERSE)
    }

    //// Toggle windmill mode
    // if (gamepad1.rightBumperWasPressed()) {
    //  robot.intake.toggleWindmill()
    // }

    if (gamepad1.rightBumperWasPressed()) {
      robot.intake.feed(coroutineScope)
    }
  }

  fun handleFlywheel() {
    // A: Toggle flywheel on and off and Close the servo gates
    // X: Accelerate flywheel to target power (long shot or short shot); then open the servo gates
    // Y: Go back to windmill power (0.3 power); close the servo gates

    // Dpad Up: Set flywheel to long shot power
    // Dpad Down: Set flywheel to short shot power
    // Dpad Right: Increase flywheel power
    // Dpad Left: Decrease flywheel power

    if (gamepad1.dpadUpWasPressed()) {
      robot.shooter.longshot()
    } else if (gamepad1.dpadDownWasPressed()) {
      robot.shooter.shortshot()
    } else if (gamepad1.dpadRightWasPressed()) {
      robot.shooter.increasePower()
    } else if (gamepad1.dpadLeftWasPressed()) {
      robot.shooter.decreasePower()
    }

    if (gamepad1.aWasPressed()) {
      robot.shooter.toggleWindmill()
    } else if (gamepad1.xWasPressed()) {
      robot.shooter.shoot(coroutineScope) {
        // Push artifact into flywheel when gate opens
        robot.intake.start()
      }
    } else if (gamepad1.yWasPressed()) {
      robot.shooter.windmill()
    }
  }

  fun handleGate() {
    if (gamepad1.bWasPressed()) {
      robot.shooter.toggleGate()
    }
  }

  override fun loop() {
    handleDrive()
    handleIntake()
    handleFlywheel()
    handleGate()

    // Control color sensor LED
    robot.shooter.enableLed(robot.intake.active || robot.shooter.flywheelActive)

    // ── Telemetry ──
    tm.addLine("── DRIVE ──")
    tm.addData("Left Power", "%.2f".format(robot.drive.leftPower))
    tm.addData("Right Power", "%.2f".format(robot.drive.rightPower))
    tm.addData("Precision Mode", if (robot.drive.precisionMode) "ON" else "OFF")

    tm.addLine()
    tm.addLine("── INTAKE ──")
    tm.addData("Intake Active", if (robot.intake.active) "YES" else "NO")
    if (robot.intake.active) {
      val mode =
              if (robot.intake.direction == DcMotorSimple.Direction.FORWARD) "Forward"
              else "Reverse"
      tm.addData("Direction", mode)
      tm.addData("Windmill", if (robot.intake.windmill) "ON" else "OFF")
    }

    tm.addLine()
    tm.addLine("── FLYWHEEL ──")
    tm.addData("Flywheel Active", if (robot.shooter.flywheelActive) "YES" else "NO")
    tm.addData("Flywheel Velocity", "${robot.shooter.flywheelVelocity.toInt()} ticks/s")
    tm.addData("Target Power", "${(robot.shooter.targetPower * 100).toInt()}%%")
    tm.addData("Actual Power", "${(robot.shooter.flywheelPower * 100).toInt()}%%")

    tm.addLine()
    tm.addLine("── GATE ──")
    tm.addData("Gate Status", if (robot.shooter.gateOpened) "OPEN" else "CLOSED")
    tm.addData("Artifact Color", robot.shooter.detectedColor)
    tm.addData("Distance", "${robot.shooter.alpha}")

    tm.update()
  }

  override fun stop() {
    coroutineScope.cancel()
    robot.stopAll()
    super.stop()
  }
}
