package org.firstinspires.ftc.teamcode.auto

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.control.PIDCoefficients
import com.bylazar.configurables.annotations.Configurable
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.PIDFCoefficients

// Mass: 16 lb or 7.26 kg

/*
 * Constants shared between multiple drive types.
 */
@Configurable
object DriveConstants {

	/*
	 * ============================
	 * MOTOR CONSTANTS
	 * ============================
	 * These values are from your motor's spec sheet.
	 *
	 * Common motors:
	 *   goBILDA 5202/5203 312 RPM: TICKS_PER_REV = 537.7,  MAX_RPM = 312
	 *   goBILDA 5202/5203 435 RPM: TICKS_PER_REV = 384.5,  MAX_RPM = 435
	 *   REV HD Hex 40:1:            TICKS_PER_REV = 1120.0, MAX_RPM = 150
	 *   REV HD Hex 20:1:            TICKS_PER_REV = 560.0,  MAX_RPM = 300
	 *   NeveRest Orbital 20:        TICKS_PER_REV = 537.6,  MAX_RPM = 340
	 */
	const val TICKS_PER_REV = 560.0
	const val MAX_RPM = 300.0

	/*
	 * Set RUN_USING_ENCODER to true if using the built-in motor velocity PID.
	 * Set to false if using feedforward only (kV, kA, kStatic).
	 * If true, update MOTOR_VELO_PID below.
	 */
	@JvmField
	var RUN_USING_ENCODER = true

	/*
	 * If RUN_USING_ENCODER is true, set these PID values for the motor velocity controller.
	 * (Not the same as HEADING/TRANSLATIONAL PID below.)
	 */
	@JvmField
	var MOTOR_VELO_PID =
		PIDFCoefficients(0.0, 0.0, 0.0, getMotorVelocityF(MAX_RPM / 60.0 * TICKS_PER_REV))

	/*
	 * ============================
	 * PHYSICAL DIMENSIONS
	 * ============================
	 * Measure these from your actual robot!
	 */

	/*
	 * WHEEL_RADIUS: The radius of your drive wheels in inches.
	 *   96mm goBILDA Mecanum = 1.8898 inches
	 *   75mm wheels = 1.4764 inches
	 * TODO: Measure your wheel radius
	 */
	@JvmField
	var WHEEL_RADIUS = 3.54331

	/*
	 * GEAR_RATIO: External gear ratio (output/input).
	 *   If your motor connects directly to the wheel: 1.0
	 *   If you have a 2:1 gear reduction: 0.5
	 */
	@JvmField
	var GEAR_RATIO = 1.0 / 20.0

	/*
	 * TRACK_WIDTH: Distance between the centers of the left and right wheels, in inches.
	 *   Use a ruler/tape to measure this on your robot.
	 *   This will be refined during TrackWidthTuner.
	 */
	@JvmField
	var TRACK_WIDTH = 15.0

	/*
	 * ============================
	 * FEEDFORWARD PARAMETERS
	 * ============================
	 * These are determined by running the feedforward tuning routine.
	 * See: https://learnroadrunner.com/feedforward-tuning.html
	 *
	 *   kV = 1 / maxVelocity (ticks/sec converted to inches/sec)
	 *   kA = acceleration constant (usually small)
	 *   kStatic = minimum power to overcome static friction
	 *
	 * TODO: Run ManualFeedforwardTuner or AutomaticFeedforwardTuner
	 */
	@JvmField
	var kV = 0.019

	@JvmField
	var kA = 0.003

	@JvmField
	var kStatic = 0.05

	/*
	 * ============================
	 * DRIVE CONSTRAINTS
	 * ============================
	 * Max velocity, acceleration, and angular velocity/acceleration.
	 *
	 * MAX_VEL: maximum velocity in inches/sec. A safe starting value is
	 *          ~75% of your theoretical max: rpmToVelocity(MAX_RPM) * 0.75
	 * MAX_ACCEL: maximum acceleration in inches/sec^2 (start equal to MAX_VEL)
	 * MAX_ANG_VEL: maximum angular velocity in rad/sec (Math.toRadians(180) is safe)
	 * MAX_ANG_ACCEL: maximum angular acceleration in rad/sec^2
	 */
	@JvmField
	var MAX_VEL = 30.0

	@JvmField
	var MAX_ACCEL = 30.0

	@JvmField
	var MAX_ANG_VEL = Math.toRadians(180.0)

	@JvmField
	var MAX_ANG_ACCEL = Math.toRadians(180.0)

	/*
	 * ============================
	 * PID COEFFICIENTS
	 * ============================
	 * These are tuned AFTER feedforward and track width.
	 *
	 * HEADING_PID: Corrects heading error during trajectory following.
	 *              Tune with TurnTest. See: https://learnroadrunner.com/heading-pid-tuning.html
	 *
	 * TRANSLATIONAL_PID: Corrects cross-track (lateral) error.
	 *                    Tune with FollowerPIDTuner. See: https://learnroadrunner.com/follower-pid-tuning.html
	 *
	 * TODO: Tune these after feedforward constants are set
	 */
	@JvmField
	var HEADING_PID = PIDCoefficients(8.0, 0.0, 0.5)

	@JvmField
	var TRANSLATIONAL_PID = PIDCoefficients(8.0, 0.0, 0.5)

	/*
	 * ============================
	 * MOTOR DIRECTIONS
	 * ============================
	 * Adjust if your motors run in unexpected directions.
	 * For most drivetrains, left motors are REVERSE and right motors are FORWARD.
	 */
	/*
	 * ============================
	 * IMU ORIENTATION
	 * ============================
	 * Adjust these based on how the Control Hub is mounted on your robot.
	 * See: https://ftc-docs.firstinspires.org/en/latest/programming_resources/imu/imu.html
	 */
	@JvmField
	var LOGO_FACING_DIR = RevHubOrientationOnRobot.LogoFacingDirection.LEFT

	@JvmField
	var USB_FACING_DIR = RevHubOrientationOnRobot.UsbFacingDirection.BACKWARD

	@JvmField
	var LEFT_DIRECTION = DcMotorSimple.Direction.REVERSE

	@JvmField
	var RIGHT_DIRECTION = DcMotorSimple.Direction.FORWARD

	// Helper function: converts RPM to inches/sec based on wheel geometry
	@JvmStatic
	fun rpmToVelocity(rpm: Double): Double {
		return rpm * GEAR_RATIO * 2 * Math.PI * WHEEL_RADIUS / 60.0
	}

	// Helper function: converts encoder ticks to inches
	@JvmStatic
	fun encoderTicksToInches(ticks: Double): Double {
		return WHEEL_RADIUS * 2 * Math.PI * GEAR_RATIO * ticks / TICKS_PER_REV
	}

	// Helper function: gets the feedforward F value for the motor velocity PID
	@JvmStatic
	fun getMotorVelocityF(ticksPerSecond: Double): Double {
		return 32767 / ticksPerSecond
	}
}
