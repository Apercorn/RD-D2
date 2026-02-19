package org.firstinspires.ftc.teamcode.core

import com.qualcomm.robotcore.hardware.ColorSensor
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo

/**
 * Central robot hardware container.
 *
 * Initializes all hardware devices and exposes subsystem controllers. Use this single class from
 * both TeleOp and Autonomous to avoid duplicating hardware init code.
 *
 * Usage:
 * ```
 * val robot = Robot(hardwareMap)
 * robot.drive.setPower(0.5, -0.5)
 * robot.shooter.setFlywheelPower(1.0)
 * robot.shooter.openGate()
 * ```
 */
class Robot(hardwareMap: HardwareMap) {

  // ── Raw Hardware ──
  val leftDrive: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, "left_drive")
  val rightDrive: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, "right_drive")
  val intakeMotor: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, "intake")
  val flywheelMotor: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, "outtake")
  val gateLeft: Servo = hardwareMap.get(Servo::class.java, "gate_left")
  val gateRight: Servo = hardwareMap.get(Servo::class.java, "gate_right")
	val feeder: Servo = hardwareMap.get(Servo::class.java, "flywheel_feeder")
  val colorSensor: ColorSensor = hardwareMap.get(ColorSensor::class.java, "next_artifact")

  // ── Subsystems ──
  val drive = Drive(leftDrive, rightDrive)
  val intake = Intake(intakeMotor, feeder)
  val shooter = Shooter(flywheelMotor, gateLeft, gateRight, colorSensor)
	val vision = Vision(hardwareMap)

  init {
    // Drive motors
    leftDrive.direction = DcMotorSimple.Direction.REVERSE
    rightDrive.direction = DcMotorSimple.Direction.FORWARD

    leftDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    rightDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    leftDrive.mode = DcMotor.RunMode.RUN_USING_ENCODER
    rightDrive.mode = DcMotor.RunMode.RUN_USING_ENCODER

    leftDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    rightDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

    // Flywheel
    flywheelMotor.direction = DcMotorSimple.Direction.REVERSE
    flywheelMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    flywheelMotor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

    // Intake
    intakeMotor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    intakeMotor.mode = DcMotor.RunMode.RUN_USING_ENCODER

    shooter.closeGate()
		intake.resetFeeder()
  }

  /** Stop all motors. Call in OpMode stop(). */
  fun stopAll() {
    drive.stop()
    intake.stop()
    shooter.stop()
  }
}
