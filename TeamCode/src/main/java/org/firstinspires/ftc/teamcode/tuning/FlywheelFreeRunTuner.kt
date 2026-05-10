package org.firstinspires.ftc.teamcode.tuning

import com.bylazar.configurables.annotations.Configurable
import com.bylazar.telemetry.JoinedTelemetry
import com.bylazar.telemetry.PanelsTelemetry
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple

/**
 * TUNING STEP 1 — Measure free-running max ticks/sec.
 *
 * Spins the flywheel at full power with no velocity controller (RUN_WITHOUT_ENCODER)
 * so you can read the motor's true physical ceiling. That ceiling is what F must be
 * calculated from, not the desired target RPM.
 *
 * Procedure:
 *   1. Run this op-mode with a fully charged battery (12.2–12.5 V).
 *   2. Press A — wait ~3 seconds for "Max TPS" to plateau.
 *   3. Copy "Suggested PIDF_F" into Shooter.PIDF_F and note the battery voltage.
 *   4. Press B to stop. Move on to FlywheelPIDFTuner.
 *
 * Controls:
 *   A – toggle motor on / off
 *   B – stop motor and reset the max-TPS tracker
 */
@Configurable
@TeleOp(name = "1. Flywheel Free-Run Tuner", group = "Tuning")
class FlywheelFreeRunTuner : OpMode() {

	private val tm by lazy { JoinedTelemetry(PanelsTelemetry.ftcTelemetry, telemetry) }
	private lateinit var flywheel: DcMotorEx

	private var spinning = false
	private var maxTps = 0.0

	override fun init() {
		flywheel = hardwareMap.get(DcMotorEx::class.java, "outtake")
		flywheel.direction = DcMotorSimple.Direction.REVERSE
		flywheel.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
		flywheel.power = 0.0

		tm.addData("Status", "Ready — press A to spin")
		tm.update()
	}

	override fun loop() {
		if (gamepad1.aWasPressed()) {
			spinning = !spinning
			flywheel.power = if (spinning) 1.0 else 0.0
			if (!spinning) maxTps = 0.0
		}

		if (gamepad1.bWasPressed()) {
			spinning = false
			flywheel.power = 0.0
			maxTps = 0.0
		}

		val tps = flywheel.velocity
		if (tps > maxTps) maxTps = tps

		val suggestedF = if (maxTps > 0.0) 32767.0 / maxTps else 0.0
		val voltage = hardwareMap.voltageSensor.iterator().next().voltage

		// Numeric values — graphed as lines in the Panels dashboard
		tm.addData("Actual TPS", tps)
		tm.addData("Max TPS", maxTps)

		tm.addLine("── FREE-RUN TUNER ──")
		tm.addData("Motor", if (spinning) "ON (full power)" else "OFF")
		tm.addData("Actual TPS", tps.toInt())
		tm.addData("Max TPS (observed)", maxTps.toInt())
		tm.addData("Suggested PIDF_F", "%.4f".format(suggestedF))
		tm.addData("Battery (V)", "%.2f".format(voltage))
		tm.addLine("── CONTROLS ──")
		tm.addData("A", "Toggle motor on/off  (also resets max)")
		tm.addData("B", "Stop + reset max TPS")
		tm.update()
	}

	override fun stop() {
		flywheel.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
		flywheel.power = 0.0
	}
}
