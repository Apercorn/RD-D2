package org.firstinspires.ftc.teamcode.auto

import com.bylazar.configurables.annotations.Configurable
import org.firstinspires.ftc.teamcode.auto.DriveConstants

/**
 * Tunable constants for the custom autonomous framework.
 *
 * All values here can be adjusted via FTC Dashboard or the Configurable system.
 * See the tuning guide at the bottom of this file.
 */
@Configurable
object AutoConstants {

	// ════════════════════════════════════════════════
	//  ENCODER → DISTANCE CONVERSION
	// ════════════════════════════════════════════════

	/**
	 * Encoder ticks per inch of forward travel.
	 *
	 * Calculated from motor specs:
	 *   TICKS_PER_INCH = TICKS_PER_REV / (WHEEL_DIAMETER * π * GEAR_RATIO)
	 *
	 * With REV HD Hex 20:1 (560 ticks), 3.54331" radius wheels, 1/20 gear ratio:
	 *   wheelCircumference = 2 * π * 3.54331 ≈ 22.263"
	 *   TICKS_PER_INCH = 560 / (22.263 * 0.05) ≈ 503
	 *
	 * **Tuning**: Mark 48" on the floor, drive the robot, read encoder ticks,
	 * then set TICKS_PER_INCH = totalTicks / 48.0
	 */
	@JvmField
	var TICKS_PER_INCH: Double = DriveConstants.TICKS_PER_REV /
					(2.0 * Math.PI * DriveConstants.WHEEL_RADIUS * DriveConstants.GEAR_RATIO)

	// ════════════════════════════════════════════════
	//  DRIVING (FORWARD / BACK)
	// ════════════════════════════════════════════════

	/**
	 * Base motor power when driving straight (0.0–1.0).
	 * Higher = faster but harder to stop accurately.
	 *
	 * **Start at 0.4**, increase after distance accuracy is good.
	 */
	@JvmField
	var DRIVE_POWER: Double = 0.4

	/**
	 * Proportional gain for distance correction while driving.
	 *
	 * error = (targetTicks − currentTicks)
	 * correction = DRIVE_P * error  (added to base power)
	 *
	 * **Start at 0.001**, increase if the robot undershoots, decrease if it oscillates.
	 */
	@JvmField
	var DRIVE_P: Double = 0.001

	/**
	 * Heading correction P gain while driving straight.
	 *
	 * Counteracts drift by adding a small differential to left/right motors.
	 * correction = DRIVE_HEADING_P * headingError
	 *
	 * **Start at 0.5**, increase if the robot drifts off-line, decrease if it wobbles.
	 */
	@JvmField
	var DRIVE_HEADING_P: Double = 0.5

	/**
	 * How close (in inches) the robot needs to be to the target to stop.
	 *
	 * **Start at 0.5**, lower for more precision (but risk oscillation).
	 */
	@JvmField
	var DRIVE_TOLERANCE_INCHES: Double = 0.5

	/**
	 * Maximum time (seconds) to spend on any single drive step before giving up.
	 *
	 * Prevents infinite loops if the robot is stuck.
	 */
	@JvmField
	var DRIVE_TIMEOUT_SEC: Double = 5.0

	/**
	 * Minimum motor power during driving (prevents stalling at low error).
	 */
	@JvmField
	var DRIVE_MIN_POWER: Double = 0.15

	// ════════════════════════════════════════════════
	//  TURNING
	// ════════════════════════════════════════════════

	/**
	 * Proportional gain for turning.
	 *
	 * power = TURN_P * headingError
	 *
	 * **Start at 0.6**, increase if turns are sluggish, decrease if they overshoot.
	 */
	@JvmField
	var TURN_P: Double = 0.6

	/**
	 * Heading error (in degrees) within which a turn is considered complete.
	 *
	 * **Start at 2.0°**, lower for more precision (but risk oscillation).
	 */
	@JvmField
	var TURN_TOLERANCE_DEG: Double = 2.0

	/**
	 * Maximum time (seconds) to spend on any single turn before giving up.
	 */
	@JvmField
	var TURN_TIMEOUT_SEC: Double = 4.0

	/**
	 * Minimum motor power during turning (prevents stalling near target).
	 */
	@JvmField
	var TURN_MIN_POWER: Double = 0.15

	/**
	 * Maximum motor power during turning (prevents overshooting).
	 */
	@JvmField
	var TURN_MAX_POWER: Double = 0.6

	// ════════════════════════════════════════════════
	//  IMU
	// ════════════════════════════════════════════════

	/**
	 * Name of the IMU in the hardware map.
	 */
	const val IMU_NAME: String = "imu"
}
