package org.firstinspires.ftc.teamcode

import com.bylazar.configurables.annotations.Configurable
import com.bylazar.telemetry.JoinedTelemetry
import com.bylazar.telemetry.PanelsTelemetry
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorSimple
import kotlinx.coroutines.*
import org.firstinspires.ftc.teamcode.core.Robot
import kotlin.time.Duration.Companion.milliseconds

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

		robot.drive.tankDrive(drive, turn)
	}

	fun handleIntake() {
		// Feed the artifact
		if (gamepad1.rightTriggerWasPressed()) {
			robot.intake.toggle(DcMotorSimple.Direction.FORWARD, coroutineScope)
		}

		//// Reverse feed the artifact
		if (gamepad1.leftTriggerWasPressed()) {
			robot.intake.toggle(DcMotorSimple.Direction.REVERSE, coroutineScope)
		}

		if (gamepad1.rightBumperWasPressed()) {
			robot.intake.feed(coroutineScope)
		}
	}

	fun handleFlywheel() {
		// A: Toggle windmill on/off; close the gate
		// X: Fire — waits for target RPM, opens gate, closes after timeout
		// Y: Set to windmill RPM; close the gate

		// Dpad Up: Longshot RPM
		// Dpad Down: Shortshot RPM
		// Dpad Right: Increase target RPM
		// Dpad Left: Decrease target RPM

		if (gamepad1.dpadUpWasPressed()) {
			robot.shooter.longshot()
		} else if (gamepad1.dpadDownWasPressed()) {
			robot.shooter.shortshot()
		} else if (gamepad1.dpadRightWasPressed()) {
			robot.shooter.increaseRpm()
		} else if (gamepad1.dpadLeftWasPressed()) {
			robot.shooter.decreaseRpm()
		}

		if (gamepad1.aWasPressed()) {
			robot.shooter.toggleWindmill()
			robot.intake.stop()

		} else if (gamepad1.xWasPressed()) {
			robot.shooter.shoot(coroutineScope) {
				robot.intake.start(coroutineScope)
			}
		} else if (gamepad1.yWasPressed()) {
			robot.shooter.windmill()
			robot.intake.stop()
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
		tm.addData("Encoder ticks/sec", robot.shooter.flywheelVelocity.toInt())
		tm.addData("Target RPM", robot.shooter.targetRpm.toInt())
		tm.addData("Actual RPM", robot.shooter.flywheelRpm.toInt())

		tm.addLine()
		tm.addLine("── GATE ──")
		tm.addData("Gate Status", if (robot.shooter.gateOpened) "OPEN" else "CLOSED")

		tm.update()
	}

	override fun stop() {
		coroutineScope.cancel()
		robot.stopAll()
		super.stop()
	}
}
