package org.firstinspires.ftc.teamcode.core

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.VoltageSensor
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*

/**
 * Shooter subsystem that manages the flywheel motor and gate servos.
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
	private val voltageSensor: VoltageSensor
) {
	companion object {
		const val VOLTAGE_CONST = 12.7

		// ── Flywheel Power Presets ──
		const val LONGSHOT_PWR = 1.0
		const val SHORTSHOT_PWR = 0.7
		const val WINDMILL_POWER = 0.3
		const val POWER_STEP = 0.05

		/** The time taken for the motor to get up to high RPM */
		const val SPINUP_DURATION = 4000L
		/** How long after shoot() until the gate is closed again */
		const val SHOOT_TIMEOUT = 5000L

		// ── RPM Targets ──
		const val TICKS_PER_REV = 28.0
		const val SHORTSHOT_RPM = 1720.0
		const val LONGSHOT_RPM = 2200.0
		const val RPM_TOLERANCE = 50.0

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

	/** Target RPM for the current shot mode. Used by shoot() to know when the motor is ready. */
	var targetRpm = LONGSHOT_RPM
		private set

	/** System.currentTimeMillis() at which the spinup window expires. -1 = not started. */
	private var spinupReadyAt = -1L

	/** Whether enough time has elapsed since the shot mode was selected. */
	val spinupReady: Boolean
		get() = spinupReadyAt >= 0 && System.currentTimeMillis() >= spinupReadyAt

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
		targetRpm = LONGSHOT_RPM
		spinupReadyAt = System.currentTimeMillis() + SPINUP_DURATION
		applyFlywheel()
	}

	/** Set target to shortshot preset. */
	fun shortshot() {
		flywheelActive = true
		targetRpm = SHORTSHOT_RPM
		spinupReadyAt = System.currentTimeMillis() + SPINUP_DURATION

		val target = (SHORTSHOT_PWR * VOLTAGE_CONST) / voltageSensor.voltage
		targetPower = target.coerceIn(0.0, 1.0)

		applyFlywheel()
	}

	/**
	 * Spin up the flywheel to target power and open the gate after a calculated delay based on RPM
	 * difference. Also feeds intake at full.
	 *
	 * @param scope CoroutineScope to launch the delayed gate opening in.
	 * @param onGateOpen Optional callback when the gate opens (e.g., to push intake).
	 */
	fun shoot(scope: CoroutineScope, onGateOpen: (suspend () -> Unit)? = null) {
		flywheelActive = true
		applyFlywheel()

		if (targetPower <= WINDMILL_POWER) return;

		scope.launch {
			// Wait for spinup timer
			val spinupWait = spinupReadyAt - System.currentTimeMillis()
			if (spinupWait > 0) delay(spinupWait.milliseconds)

			// Then wait for actual RPM to reach threshold
			val thresholdTicksPerSec = (targetRpm - RPM_TOLERANCE) * TICKS_PER_REV / 60.0
			val deadline = System.currentTimeMillis() + SHOOT_TIMEOUT
			while (flywheelVelocity < thresholdTicksPerSec) {
				if (System.currentTimeMillis() > deadline) break
				delay(50.milliseconds)
			}

			openGate()
			onGateOpen?.invoke()

			// Auto-close gate after shot
			delay(SHOOT_TIMEOUT.milliseconds)
			closeGate()
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

	/** Apply current state to the motor. */
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
