package org.firstinspires.ftc.teamcode.core

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.Servo
import kotlin.math.abs
import kotlinx.coroutines.*

/**
 * Shooter subsystem: flywheel motor, gate servos, and color sensor.
 *
 * Handles flywheel power presets, gate open/close logic, color-based artifact detection, and a
 * coroutine-driven delayed gate opening that waits for the flywheel to spin up before releasing.
 *
 * Usage:
 * ```
 * shooter.windmill()               // Spin flywheel slowly
 * shooter.shoot(scope)             // Spin up + delayed gate open
 * shooter.setFlywheelPower(0.8)    // Custom power
 * shooter.openGate() / closeGate()
 * shooter.detectedColor            // "PURPLE", "GREEN", or "NONE"
 * ```
 */
class Shooter(
	private val flywheel: DcMotorEx,
	private val gateLeft: Servo,
	private val gateRight: Servo,
) {
	companion object {
		// ── Flywheel Power Presets ──
		const val LONGSHOT_PWR = 1.0
		const val SHORTSHOT_PWR = 0.8
		const val WINDMILL_POWER = 0.3
		const val POWER_STEP = 0.05

		// ── Gate Servo Positions ──
		const val GATE_LEFT_OPEN_POS = 1.0
		const val GATE_LEFT_CLOSED_POS = 0.65
		const val GATE_RIGHT_OPEN_POS = 0.2
		const val GATE_RIGHT_CLOSED_POS = 0.5
	}

	// ── State ──
	/** Whether the flywheel is currently active. */
	var flywheelActive = false
		private set

	/** Current target power for the flywheel. */
	var targetPower = LONGSHOT_PWR

	/** Whether the gate is currently open. */
	var gateOpened = false
		private set

	// ── Flywheel Control ──
	/** Set the flywheel to a specific power and activate it. */
	fun setFlywheelPower(power: Double) {
		targetPower = power.coerceIn(0.0, 1.0)
		flywheelActive = true
		applyFlywheel()
	}

	/** Toggle the flywheel on/off at windmill power. Closes the gate. */
	fun toggleWindmill() {
		flywheelActive = !flywheelActive
		targetPower = WINDMILL_POWER
		closeGate()
		applyFlywheel()
	}

	/** Set flywheel to windmill power (stays on). Closes the gate. */
	fun windmill() {
		flywheelActive = true
		targetPower = WINDMILL_POWER
		closeGate()
		applyFlywheel()
	}

	/** Increase target power by one step. */
	fun increasePower() {
		targetPower = (targetPower + POWER_STEP).coerceIn(0.0, 1.0)
		applyFlywheel()
	}

	/** Decrease target power by one step. */
	fun decreasePower() {
		targetPower = (targetPower - POWER_STEP).coerceIn(0.0, 1.0)
		applyFlywheel()
	}

	/** Set target to longshot preset. */
	fun longshot() {
		flywheelActive = true
		targetPower = LONGSHOT_PWR
		applyFlywheel()
	}

	/** Set target to shortshot preset. */
	fun shortshot() {
		flywheelActive = true
		targetPower = SHORTSHOT_PWR
		applyFlywheel()
	}

	/**
	 * Spin up the flywheel to target power and open the gate after a calculated delay based on RPM
	 * difference. Also feeds intake at full.
	 *
	 * @param scope CoroutineScope to launch the delayed gate opening in.
	 * @param onGateOpen Optional callback when the gate opens (e.g., to push intake).
	 */
	fun shoot(scope: CoroutineScope, onGateOpen: (() -> Unit)? = null) {
		flywheelActive = true
		applyFlywheel()

		if (targetPower <= WINDMILL_POWER) return;

		scope.launch {
			val targetRpm = targetPower * 1500.0
			val rpmDiff = targetRpm - flywheelVelocity

			

			// Wait proportional to how far the flywheel needs to spin up
			val waitTimeMs = (rpmDiff * 20).toLong().coerceAtLeast(0)
			delay(waitTimeMs)

			openGate()
			onGateOpen?.invoke()

			//delay(2000)

			//windmill()
		}
	}

	/** Stop the flywheel. */
	fun stopFlywheel() {
		flywheelActive = false
		applyFlywheel()
	}

	/** Stop everything (flywheel + close gate). */
	fun stop() {
		stopFlywheel()
		closeGate()
	}

	private fun applyFlywheel() {
		flywheel.power = if (flywheelActive) targetPower else 0.0
	}

	// ── Gate Control ──

	/** Open the gate servos. */
	fun openGate() {
		gateOpened = true
		gateLeft.position = GATE_LEFT_OPEN_POS
		gateRight.position = GATE_RIGHT_OPEN_POS
	}

	/** Close the gate servos. */
	fun closeGate() {
		gateOpened = false
		gateLeft.position = GATE_LEFT_CLOSED_POS
		gateRight.position = GATE_RIGHT_CLOSED_POS
	}

	/** Toggle gate open/closed. */
	fun toggleGate() {
		if (gateOpened) closeGate() else openGate()
	}


	/** Current flywheel velocity (ticks/sec from encoder). */
	val flywheelVelocity: Double
		get() = flywheel.velocity

	/** Current actual flywheel power. */
	val flywheelPower: Double
		get() = flywheel.power
}
