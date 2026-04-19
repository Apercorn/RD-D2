package org.firstinspires.ftc.teamcode.core

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.Servo
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*

/**
 * Intake subsystem for feeding artifacts into the robot.
 *
 * Usage:
 * ```
 * intake.start()           // Start intake forward
 * intake.reverse()         // Start intake in reverse
 * intake.stop()            // Stop intake
 * intake.toggle()          // Toggle on/off
 * intake.windmill = true   // Enable slow mode
 * ```
 */
class Intake(
	private val motor: DcMotorEx,
	private val feeder: Servo
) {

	companion object {
		/** Power applied during windmill mode. */
		const val WINDMILL_PWR = 0.3

		/** Power applied during normal operation. */
		const val FULL_PWR = 1.0

		/** Time it takes the servo to do a full sweep */
		const val FEEDER_SWEEP_DELAY = 500L

		const val FEEDER_SWEEP_END = 0.7
		const val FEEDER_SWEEP_START = 0.1
	}

	/** Whether the intake is currently running. */
	var active = false
		private set

	/** Current intake direction. */
	var direction: DcMotorSimple.Direction = DcMotorSimple.Direction.FORWARD
		private set

	/** When true, intake runs at reduced power. */
	var windmill = false

	/** Whether the feeder is currently mid-sweep. */
	var feederBusy = false
		private set

	private var feederJob: Job? = null

	private fun launchContinuousFeeder(scope: CoroutineScope) {
		feederJob?.cancel()
		feederJob = scope.launch {
			while (active) {
				feeder.position = FEEDER_SWEEP_END
				delay((FEEDER_SWEEP_DELAY - 50).milliseconds)
				feeder.position = FEEDER_SWEEP_START
				delay((FEEDER_SWEEP_DELAY - 50).milliseconds)
			}
		}
	}

	/** Start the intake running forward. */
	fun start(scope: CoroutineScope? = null) {
		active = true
		direction = DcMotorSimple.Direction.FORWARD
		apply()

		scope?.let { launchContinuousFeeder(it) }
	}

	/** Start the intake running in reverse. */
	fun reverse() {
		active = true
		direction = DcMotorSimple.Direction.REVERSE
		apply()
	}

	/** Stop the intake. */
	fun stop() {
		active = false
		apply()

		feederJob?.cancel()
		feederJob = null
	}

	/** Toggle the intake on/off in the current direction. */
	fun toggle(scope: CoroutineScope? = null) {
		active = !active
		apply()

		if (active) {
			scope?.let { launchContinuousFeeder(it) }
		} else {
			feederJob?.cancel();
			feederJob = null
		}
	}

	/** Toggle with a specific direction. */
	fun toggle(dir: DcMotorSimple.Direction, scope: CoroutineScope? = null) {
		active = !active
		direction = dir
		apply()

		if (active && dir == DcMotorSimple.Direction.FORWARD) {
			scope?.let { launchContinuousFeeder(it) }
		} else {
			feederJob?.cancel();
			feederJob = null
		}
	}

	/** Toggle windmill mode on/off. */
	fun toggleWindmill() {
		windmill = !windmill
		apply()
	}

	/**
	 * Sweep the feeder servo from start → end to push the artifact into the second intake wheels.
	 *
	 * 1. Moves to [feederStart] first
	 * 2. Waits [feederResetDelayMs] for the servo to arrive
	 * 3. Swings to [feederEnd]
	 */
	fun feed(scope: CoroutineScope) {
		if (feederBusy || feederJob?.isActive == true) return

		feederBusy = true
		feeder.position = FEEDER_SWEEP_END

		scope.launch {
			delay(FEEDER_SWEEP_DELAY.milliseconds)
			feeder.position = FEEDER_SWEEP_START
			feederBusy = false
		}
	}

	fun resetFeeder() {
		feeder.position = FEEDER_SWEEP_START
	}

	/** Apply current state to the motor. */
	fun apply() {
		motor.direction = direction
		motor.power =
			if (active) {
				if (windmill) WINDMILL_PWR else FULL_PWR
			} else {
				0.0
			}
	}

	/** Current motor power being applied. */
	val power: Double
		get() = motor.power
}
