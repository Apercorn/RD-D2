package org.firstinspires.ftc.teamcode.auto

/**
 * Sealed class representing individual steps in an autonomous sequence.
 *
 * Each variant holds the parameters for that type of movement.
 */
sealed class AutoStep {

	/** Drive forward (positive) or backward (negative) by [inches]. */
	data class Drive(val inches: Double) : AutoStep()

	/**
	 * Turn in place by [degrees].
	 * Positive = counter-clockwise (left), Negative = clockwise (right).
	 */
	data class Turn(val degrees: Double) : AutoStep()

	/** Pause execution for [seconds]. */
	data class Wait(val seconds: Double) : AutoStep()

	/** Run a lambda immediately when reached in the sequence. */
	data class Marker(val action: () -> Unit) : AutoStep()
}

/**
 * Builder for creating autonomous sequences with a fluent API.
 *
 * Mirrors the TrajectorySequenceBuilder style:
 * ```
 * val steps = AutoBuilder()
 *     .forward(24.0)
 *     .turn(90.0)
 *     .addDisplacementMarker { robot.intake.start() }
 *     .forward(12.0)
 *     .waitSeconds(1.0)
 *     .addDisplacementMarker { robot.shooter.openGate() }
 *     .build()
 * ```
 *
 * Then pass the result to [AutoRunner.run] to execute.
 */
class AutoBuilder {

	private val steps = mutableListOf<AutoStep>()

	/**
	 * Drive forward by [inches].
	 * @param inches Distance in inches (must be positive).
	 */
	fun forward(inches: Double): AutoBuilder {
		steps.add(AutoStep.Drive(inches))
		return this
	}

	/**
	 * Drive backward by [inches].
	 * @param inches Distance in inches (must be positive; direction is handled internally).
	 */
	fun back(inches: Double): AutoBuilder {
		steps.add(AutoStep.Drive(-inches))
		return this
	}

	/**
	 * Turn in place by [degrees].
	 * @param degrees Positive = left (CCW), Negative = right (CW).
	 *
	 * Example: `.turn(90.0)` turns left 90°, `.turn(-90.0)` turns right 90°.
	 */
	fun turn(degrees: Double): AutoBuilder {
		steps.add(AutoStep.Turn(degrees))
		return this
	}

	/**
	 * Pause the sequence for [seconds].
	 */
	fun waitSeconds(seconds: Double): AutoBuilder {
		steps.add(AutoStep.Wait(seconds))
		return this
	}

	/**
	 * Insert a marker that executes [action] when reached.
	 *
	 * Use this for things like starting/stopping the intake, spinning up the flywheel, etc.
	 * The action runs synchronously before the next step begins.
	 */
	fun addDisplacementMarker(action: () -> Unit): AutoBuilder {
		steps.add(AutoStep.Marker(action))
		return this
	}

	/**
	 * Build and return the sequence as an immutable list of [AutoStep]s.
	 */
	fun build(): List<AutoStep> = steps.toList()
}
