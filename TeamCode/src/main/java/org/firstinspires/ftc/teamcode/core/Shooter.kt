package org.firstinspires.ftc.teamcode.core

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import com.qualcomm.robotcore.hardware.Servo
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*

/**
 * Shooter subsystem: flywheel motor and gate servos.
 *
 * The flywheel runs in velocity mode (RUN_USING_ENCODER) with PIDF control.
 * [shoot] polls actual RPM and opens the gate only once the target is reached.
 *
 * PIDF tuning steps:
 *  1. Spin the flywheel at unrestricted full power, record the max ticks/sec from telemetry.
 *  2. Set [PIDF_F] = 32767 / maxTicksPerSec.
 *  3. If velocity sags under load, increase [PIDF_P] in small steps.
 */
class Shooter(
	private val flywheel: DcMotorEx,
	private val gateLeft: Servo,
	private val gateRight: Servo,
) {
	companion object {
		// ── RPM Targets ──
		const val TICKS_PER_REV = 28.0
		const val SHORTSHOT_RPM = 1720.0
		const val LONGSHOT_RPM = 2200.0
		const val WINDMILL_RPM = 600.0
		const val RPM_STEP = 100.0
		const val RPM_TOLERANCE = 50.0  // gate opens within this many RPM of target

		/** Max time to wait for the flywheel before firing anyway. */
		const val SPINUP_TIMEOUT_MS = 5000L

		/** How long to hold the gate open after firing. */
		const val SHOOT_TIMEOUT_MS = 5000L

		// ── PIDF (velocity mode) ──
		// F formula: 32767 / (LONGSHOT_RPM * TICKS_PER_REV / 60) ≈ 32.0
		const val PIDF_F = 32.0
		const val PIDF_P = 10.0
		const val PIDF_I = 0.0
		const val PIDF_D = 0.0

		// ── Gate Positions ──
		const val GATE_LEFT_OPEN_POS = 1.0
		const val GATE_LEFT_CLOSED_POS = 0.65
		const val GATE_RIGHT_OPEN_POS = 0.2
		const val GATE_RIGHT_CLOSED_POS = 0.5
	}

	// ── State ──
	var flywheelActive = false
		private set

	var targetRpm = LONGSHOT_RPM
		private set

	var gateOpened = false
		private set

	private var shootJob: Job? = null

	init {
		flywheel.mode = DcMotor.RunMode.RUN_USING_ENCODER
		flywheel.setPIDFCoefficients(
			DcMotor.RunMode.RUN_USING_ENCODER,
			PIDFCoefficients(PIDF_P, PIDF_I, PIDF_D, PIDF_F)
		)
	}

	// ── Flywheel Control ──

	fun longshot() {
		flywheelActive = true
		targetRpm = LONGSHOT_RPM
		applyFlywheel()
	}

	fun shortshot() {
		flywheelActive = true
		targetRpm = SHORTSHOT_RPM
		applyFlywheel()
	}

	/** Slow spin to keep the flywheel warm between shots. */
	fun windmill() {
		flywheelActive = true
		targetRpm = WINDMILL_RPM
		closeGate()
		applyFlywheel()
	}

	/** Toggle between windmill and stopped. */
	fun toggleWindmill() {
		if (flywheelActive && targetRpm == WINDMILL_RPM) stopFlywheel()
		else windmill()
	}

	fun increaseRpm() {
		targetRpm = (targetRpm + RPM_STEP).coerceAtMost(LONGSHOT_RPM)
		applyFlywheel()
	}

	fun decreaseRpm() {
		targetRpm = (targetRpm - RPM_STEP).coerceAtLeast(WINDMILL_RPM)
		applyFlywheel()
	}

	/**
	 * Fire when ready: polls flywheel RPM and opens the gate once [targetRpm] is reached.
	 * If called again mid-flight it cancels the previous shot and starts a fresh one.
	 *
	 * @param onGateOpen Optional suspend callback that runs immediately after the gate opens.
	 */
	fun shoot(scope: CoroutineScope, onGateOpen: (suspend () -> Unit)? = null) {
		if (!flywheelActive || targetRpm <= WINDMILL_RPM) return

		shootJob?.cancel()
		shootJob = scope.launch {
			val thresholdTPS = (targetRpm - RPM_TOLERANCE) * TICKS_PER_REV / 60.0
			val deadline = System.currentTimeMillis() + SPINUP_TIMEOUT_MS

			while (flywheelVelocity < thresholdTPS) {
				if (System.currentTimeMillis() > deadline) break
				delay(50.milliseconds)
			}

			openGate()
			onGateOpen?.invoke()

			delay(SHOOT_TIMEOUT_MS.milliseconds)
			closeGate()
		}
	}

	fun stopFlywheel() {
		flywheelActive = false
		shootJob?.cancel()
		shootJob = null
		applyFlywheel()
	}

	fun stop() {
		stopFlywheel()
		closeGate()
	}

	private fun applyFlywheel() {
		if (flywheelActive) {
			flywheel.mode = DcMotor.RunMode.RUN_USING_ENCODER
			flywheel.velocity = targetRpm * TICKS_PER_REV / 60.0
		} else {
			flywheel.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
			flywheel.power = 0.0
		}
	}

	// ── Gate Control ──

	fun openGate() {
		gateOpened = true
		gateLeft.position = GATE_LEFT_OPEN_POS
		gateRight.position = GATE_RIGHT_OPEN_POS
	}

	fun closeGate() {
		gateOpened = false
		gateLeft.position = GATE_LEFT_CLOSED_POS
		gateRight.position = GATE_RIGHT_CLOSED_POS
	}

	fun toggleGate() {
		if (gateOpened) closeGate() else openGate()
	}

	/** Current flywheel velocity in ticks/sec. */
	val flywheelVelocity: Double
		get() = flywheel.velocity

	/** Current flywheel speed in RPM. */
	val flywheelRpm: Double
		get() = flywheelVelocity / TICKS_PER_REV * 60.0
}
