package org.firstinspires.ftc.teamcode.core

import com.qualcomm.robotcore.hardware.DcMotorEx

/**
 * Drive subsystem for tank drive control.
 *
 * Provides both raw power control and convenience methods for arcade-style driving with precision
 * mode.
 *
 * Usage:
 * ```
 * // Direct power
 * drive.setPower(0.5, 0.5)
 *
 * // Arcade-style (joystick input)
 * drive.arcadeDrive(forwardPower, turnPower)
 *
 * // With precision mode
 * drive.precisionMode = true
 * drive.arcadeDrive(forward, turn) // Automatically scaled
 * ```
 */
class Drive(private val leftMotor: DcMotorEx, private val rightMotor: DcMotorEx) {

	companion object {
		const val PRECISION_MULTIPLIER = 0.3
	}

  /** When true, drive power is scaled by [precisionMultiplier]. */
  var precisionMode = false

  /** Set raw power to both motors. */
  fun setPower(left: Double, right: Double) {
    leftMotor.power = left
    rightMotor.power = right
  }

  /**
   * Arcade-style tank drive.
   * @param drive Forward/backward power (-1 to 1). Positive = forward.
   * @param turn Left/right turning power (-1 to 1). Positive = turn right.
   */
  fun arcadeDrive(drive: Double, turn: Double) {
    val multiplier = if (precisionMode) PRECISION_MULTIPLIER else 1.0
    val left = (drive + turn) * multiplier
    val right = (drive - turn) * multiplier
    setPower(left, right)
  }

  /** Stop both drive motors. */
  fun stop() {
    setPower(0.0, 0.0)
  }

  /** Current left motor power. */
  val leftPower: Double
    get() = leftMotor.power

  /** Current right motor power. */
  val rightPower: Double
    get() = rightMotor.power

  /** Current left encoder position. */
  val leftPosition: Int
    get() = leftMotor.currentPosition

  /** Current right encoder position. */
  val rightPosition: Int
    get() = rightMotor.currentPosition
}
