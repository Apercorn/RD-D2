package org.firstinspires.ftc.teamcode.tuning

import com.bylazar.configurables.annotations.Configurable
import com.bylazar.telemetry.JoinedTelemetry
import com.bylazar.telemetry.PanelsTelemetry
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import org.firstinspires.ftc.teamcode.core.Shooter

/**
 * TUNING STEP 2 — PIDF step-response tuner.
 *
 * Lets you adjust PIDF coefficients live from the Panels dashboard and see the step response
 * (target vs actual RPM) graphed in real time. Work through the coefficients in order:
 *
 *   F first — zero error in steady state at no load.
 *   P second — faster recovery from load disturbances (e.g. a ball hitting the flywheel).
 *   I/D last — only if there is a persistent steady-state error (I) or oscillation (D).
 *
 * Procedure:
 *   1. Set PIDF_F to the value from FlywheelFreeRunTuner; leave P/I/D = 0.
 *   2. Set TARGET_RPM to the shot RPM you're testing (e.g. 2200).
 *   3. Press A — the motor steps from 0 to TARGET_RPM. Watch the "Actual RPM" graph line.
 *      - Actual way above target → F too high, reduce it, press A again.
 *      - Actual way below target → F too low, raise it, press A again.
 *      - Actual settles close to target → F is good.
 *   4. Bump PIDF_P in steps of ~2–5. After each change press X to hot-reload, then push
 *      against the flywheel with a ball. Watch how fast it recovers.
 *      Stop raising P when the velocity starts ringing/oscillating.
 *   5. Copy final values to Shooter.PIDF_F, Shooter.PIDF_P (etc.).
 *
 * Controls:
 *   A – apply PIDF coefficients and step motor to TARGET_RPM
 *   B – stop flywheel
 *   X – hot-reload PIDF while motor is already running (no restart needed)
 */
@Configurable
@TeleOp(name = "2. Flywheel PIDF Tuner", group = "Tuning")
class FlywheelPIDFTuner : OpMode() {

	companion object {
		// ── Edit these from the Panels dashboard ──
		@JvmField
		var PIDF_F = 15.6   // Starting estimate; replace with Free-Run Tuner result
		@JvmField
		var PIDF_P = 0.0
		@JvmField
		var PIDF_I = 0.0
		@JvmField
		var PIDF_D = 0.0

		@JvmField
		var TARGET_RPM = 2200.0
	}

	private val tm by lazy { JoinedTelemetry(PanelsTelemetry.ftcTelemetry, telemetry) }
	private lateinit var flywheel: DcMotorEx

	private var spinning = false

	override fun init() {
		flywheel = hardwareMap.get(DcMotorEx::class.java, "outtake")
		flywheel.direction = DcMotorSimple.Direction.REVERSE
		flywheel.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
		flywheel.mode = DcMotor.RunMode.RUN_USING_ENCODER
		applyPIDF()

		tm.addData("Status", "Ready — press A to step")
		tm.update()
	}

	override fun loop() {
		// A: apply latest PIDF values and command the step
		if (gamepad1.aWasPressed()) {
			applyPIDF()
			spinning = true
			flywheel.velocity = TARGET_RPM * Shooter.TICKS_PER_REV / 60.0
		}

		// B: stop
		if (gamepad1.bWasPressed()) {
			spinning = false
			flywheel.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
			flywheel.power = 0.0
		}

		// X: hot-reload PIDF without restarting the motor
		if (gamepad1.xWasPressed()) {
			applyPIDF()
		}

		val actualRpm = flywheel.velocity / Shooter.TICKS_PER_REV * 60.0
		val errorRpm = TARGET_RPM - actualRpm
		val voltage = hardwareMap.voltageSensor.iterator().next().voltage

		// Numeric values — graphed as lines in the Panels dashboard
		if (spinning) {
			tm.addData("Target RPM", TARGET_RPM)
			tm.addData("Actual RPM", actualRpm)
			tm.addData("Error RPM", errorRpm)
		}

		tm.addLine("── PIDF TUNER ──")
		tm.addData("Motor", if (spinning) "ON" else "OFF")
		tm.addData("Target RPM", TARGET_RPM.toInt())
		tm.addData("Actual RPM", actualRpm.toInt())
		tm.addData("Error RPM", if (spinning) errorRpm.toInt() else 0)
		tm.addData("Battery (V)", "%.2f".format(voltage))
		tm.addLine("── ACTIVE PIDF ──")
		tm.addData("F", PIDF_F)
		tm.addData("P", PIDF_P)
		tm.addData("I", PIDF_I)
		tm.addData("D", PIDF_D)
		tm.addLine("── CONTROLS ──")
		tm.addData("A", "Apply PIDF + step to TARGET_RPM")
		tm.addData("B", "Stop flywheel")
		tm.addData("X", "Hot-reload PIDF (no restart)")
		tm.update()
	}

	private fun applyPIDF() {
		flywheel.mode = DcMotor.RunMode.RUN_USING_ENCODER
		flywheel.setPIDFCoefficients(
			DcMotor.RunMode.RUN_USING_ENCODER,
			PIDFCoefficients(PIDF_P, PIDF_I, PIDF_D, PIDF_F)
		)
	}

	override fun stop() {
		flywheel.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
		flywheel.power = 0.0
	}
}
