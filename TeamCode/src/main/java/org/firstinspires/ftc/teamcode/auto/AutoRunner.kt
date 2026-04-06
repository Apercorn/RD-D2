package org.firstinspires.ftc.teamcode.auto

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.IMU
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.teamcode.auto.DriveConstants
import kotlin.math.abs
import kotlin.math.sign

/**
 * Executes an autonomous sequence built by [AutoBuilder].
 *
 * Uses the IMU for heading-based turns and heading correction during straight driving,
 * and drive-wheel encoders for distance measurement.
 *
 * Usage:
 * ```
 * val runner = AutoRunner(hardwareMap, telemetry)
 * val steps = AutoBuilder()
 *     .forward(24.0)
 *     .turn(90.0)
 *     .build()
 *
 * // In your opmode loop:
 * runner.run(steps) { isStopRequested }
 * ```
 *
 * @param hardwareMap   The FTC HardwareMap.
 * @param telemetry     Telemetry for live tuning feedback.
 */
class AutoRunner(
	hardwareMap: HardwareMap,
	private val telemetry: Telemetry? = null
) {

	// ── Hardware ──
	private val leftMotor: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, "left_drive")
	private val rightMotor: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, "right_drive")
	private val imu: IMU = hardwareMap.get(IMU::class.java, AutoConstants.IMU_NAME)

	private val timer = ElapsedTime()

	init {
		// Motor directions (match Robot.kt)
		leftMotor.direction = DriveConstants.LEFT_DIRECTION
		rightMotor.direction = DriveConstants.RIGHT_DIRECTION

		// Use encoders for distance
		leftMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
		rightMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
		leftMotor.mode = DcMotor.RunMode.RUN_USING_ENCODER
		rightMotor.mode = DcMotor.RunMode.RUN_USING_ENCODER

		leftMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
		rightMotor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

		// IMU init
		imu.initialize(
			IMU.Parameters(
				RevHubOrientationOnRobot(
					DriveConstants.LOGO_FACING_DIR,
					DriveConstants.USB_FACING_DIR
				)
			)
		)
		imu.resetYaw()
	}

	// ─────────────────────────────────────────────
	//  PUBLIC API
	// ─────────────────────────────────────────────

	/**
	 * Execute a full autonomous sequence.
	 *
	 * @param steps         The list of [AutoStep]s from [AutoBuilder.build].
	 * @param isStopRequested  Lambda that returns true when the opmode is stopping.
	 *                        Typically `{ isStopRequested }` from your LinearOpMode.
	 */
	fun run(steps: List<AutoStep>, isStopRequested: () -> Boolean) {
		for ((index, step) in steps.withIndex()) {
			if (isStopRequested()) {
				stop()
				return
			}

			telemetry?.let {
				it.addData("Auto Step", "${index + 1} / ${steps.size}")
				it.addData("Step Type", step::class.simpleName)
				it.update()
			}

			when (step) {
				is AutoStep.Drive -> executeDrive(step.inches, isStopRequested)
				is AutoStep.Turn -> executeTurn(step.degrees, isStopRequested)
				is AutoStep.Wait -> executeWait(step.seconds, isStopRequested)
				is AutoStep.Marker -> step.action()
			}
		}

		stop()

		telemetry?.let {
			it.addData("Auto", "Sequence Complete")
			it.update()
		}
	}

	// ─────────────────────────────────────────────
	//  DRIVE (FORWARD / BACK)
	// ─────────────────────────────────────────────

	/**
	 * Drive [inches] using encoder ticks with IMU heading correction.
	 * Positive = forward, negative = backward.
	 */
	private fun executeDrive(inches: Double, isStopRequested: () -> Boolean) {
		val targetTicks = (abs(inches) * AutoConstants.TICKS_PER_INCH).toInt()
		val direction = inches.sign  // +1.0 forward, -1.0 backward

		// Reset encoders
		leftMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
		rightMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
		leftMotor.mode = DcMotor.RunMode.RUN_USING_ENCODER
		rightMotor.mode = DcMotor.RunMode.RUN_USING_ENCODER

		// Capture starting heading to correct drift
		val startHeading = getHeading()
		timer.reset()

		while (!isStopRequested()) {
			val avgTicks = (abs(leftMotor.currentPosition) + abs(rightMotor.currentPosition)) / 2
			val remainingTicks = targetTicks - avgTicks
			val remainingInches = remainingTicks / AutoConstants.TICKS_PER_INCH

			// Done?
			if (remainingInches <= AutoConstants.DRIVE_TOLERANCE_INCHES) break

			// Timeout?
			if (timer.seconds() > AutoConstants.DRIVE_TIMEOUT_SEC) {
				telemetry?.let {
					it.addData("⚠ Drive", "TIMEOUT at %.1f inches remaining".format(remainingInches))
					it.update()
				}
				break
			}

			// P-controller for distance
			var power = AutoConstants.DRIVE_POWER + AutoConstants.DRIVE_P * remainingTicks
			power = power.coerceIn(AutoConstants.DRIVE_MIN_POWER, 1.0)
			power *= direction

			// Heading correction — keeps the robot driving straight
			val headingError = angleDelta(startHeading, getHeading())
			val correction = AutoConstants.DRIVE_HEADING_P * headingError

			val leftPower = (power + correction).coerceIn(-1.0, 1.0)
			val rightPower = (power - correction).coerceIn(-1.0, 1.0)

			leftMotor.power = leftPower
			rightMotor.power = rightPower

			telemetry?.let {
				it.addData("Drive Target", "%.1f in".format(inches))
				it.addData("Remaining", "%.1f in".format(remainingInches))
				it.addData("Heading Error", "%.1f°".format(Math.toDegrees(headingError)))
				it.addData("Power L/R", "%.2f / %.2f".format(leftPower, rightPower))
				it.update()
			}
		}

		stop()
		// Small settle delay
		sleep(100)
	}

	// ─────────────────────────────────────────────
	//  TURN
	// ─────────────────────────────────────────────

	/**
	 * Turn in place by [degrees].
	 * Positive = left (CCW), Negative = right (CW).
	 */
	private fun executeTurn(degrees: Double, isStopRequested: () -> Boolean) {
		val targetRad = Math.toRadians(degrees)
		val startHeading = getHeading()
		val targetHeading = normalizeAngle(startHeading + targetRad)
		val toleranceRad = Math.toRadians(AutoConstants.TURN_TOLERANCE_DEG)

		timer.reset()

		while (!isStopRequested()) {
			val currentHeading = getHeading()
			val error = angleDelta(targetHeading, currentHeading)

			// Done?
			if (abs(error) <= toleranceRad) break

			// Timeout?
			if (timer.seconds() > AutoConstants.TURN_TIMEOUT_SEC) {
				telemetry?.let {
					it.addData("⚠ Turn", "TIMEOUT at %.1f° remaining".format(Math.toDegrees(error)))
					it.update()
				}
				break
			}

			// P-controller for turn
			var power = AutoConstants.TURN_P * error
			// Apply min/max power (keep the sign)
			val absPower = abs(power).coerceIn(AutoConstants.TURN_MIN_POWER, AutoConstants.TURN_MAX_POWER)
			power = absPower * power.sign

			// Positive error → need to turn left → left motor backward, right motor forward
			leftMotor.power = -power
			rightMotor.power = power

			telemetry?.let {
				it.addData("Turn Target", "%.1f°".format(degrees))
				it.addData("Error", "%.1f°".format(Math.toDegrees(error)))
				it.addData("Power", "%.2f".format(power))
				it.update()
			}
		}

		stop()
		// Small settle delay
		sleep(200)
	}

	// ─────────────────────────────────────────────
	//  WAIT
	// ─────────────────────────────────────────────

	private fun executeWait(seconds: Double, isStopRequested: () -> Boolean) {
		timer.reset()
		while (timer.seconds() < seconds && !isStopRequested()) {
			telemetry?.let {
				it.addData("Waiting", "%.1f / %.1f sec".format(timer.seconds(), seconds))
				it.update()
			}
			sleep(50)
		}
	}

	// ─────────────────────────────────────────────
	//  HELPERS
	// ─────────────────────────────────────────────

	/** Stop both motors. */
	private fun stop() {
		leftMotor.power = 0.0
		rightMotor.power = 0.0
	}

	/** Get current IMU heading in radians. */
	private fun getHeading(): Double {
		return imu.robotYawPitchRollAngles.getYaw(AngleUnit.RADIANS)
	}

	/**
	 * Calculate the shortest signed angle from [current] to [target].
	 * Both in radians. Result is in [-π, π].
	 */
	private fun angleDelta(target: Double, current: Double): Double {
		return normalizeAngle(target - current)
	}

	/** Normalize an angle to [-π, π]. */
	private fun normalizeAngle(angle: Double): Double {
		var a = angle
		while (a > Math.PI) a -= 2 * Math.PI
		while (a < -Math.PI) a += 2 * Math.PI
		return a
	}

	/** Thread sleep wrapper. */
	private fun sleep(ms: Long) {
		try {
			Thread.sleep(ms)
		} catch (_: InterruptedException) {
			Thread.currentThread().interrupt()
		}
	}
}
