package org.firstinspires.ftc.teamcode

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple

@TeleOp(name = "Competition TeleOp", group = "Competition")
class CompetitionTeleOp : OpMode() {
  private lateinit var leftDrive: DcMotorEx
  private lateinit var rightDrive: DcMotorEx
  private lateinit var intake: DcMotorEx
  private lateinit var flywheel: DcMotorEx

  private var intakeActive = false
  private var outtakeActive = false

  private var flywheelTimer = 0.0
  private var flywheelAtRPM = false

  private var intakeDirection = DcMotorSimple.Direction.REVERSE

  override fun init() {
    leftDrive = hardwareMap.get(DcMotorEx::class.java, "left_drive")
    rightDrive = hardwareMap.get(DcMotorEx::class.java, "right_drive")
    intake = hardwareMap.get(DcMotorEx::class.java, "intake")
    flywheel = hardwareMap.get(DcMotorEx::class.java, "outtake")

    // Set drive motor directions
    leftDrive.direction = DcMotorSimple.Direction.REVERSE
    rightDrive.direction = DcMotorSimple.Direction.FORWARD

    // Set the flywheel outtake direction
    flywheel.direction = DcMotorSimple.Direction.REVERSE

    // Reset the encoder count to 0
    leftDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    rightDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    flywheel.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    intake.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER

    // Enable the motor encoder
    leftDrive.mode = DcMotor.RunMode.RUN_USING_ENCODER
    rightDrive.mode = DcMotor.RunMode.RUN_USING_ENCODER
    flywheel.mode = DcMotor.RunMode.RUN_USING_ENCODER
    intake.mode = DcMotor.RunMode.RUN_USING_ENCODER

    telemetry.addData("Status", "Initialized")
    telemetry.update()
  }

  override fun loop() {
    val drive = -gamepad1.left_stick_y.toDouble()
    val turn = gamepad1.right_stick_x.toDouble()

    val leftPower = drive + turn
    val rightPower = drive - turn

    // Feed the artifact into the robot
    if (gamepad1.rightTriggerWasPressed()) {
      intakeActive = !intakeActive
      intakeDirection = DcMotorSimple.Direction.REVERSE
    }

    // Run the intake backwards
    if (gamepad1.leftTriggerWasPressed()) {
      intakeActive = !intakeActive
      intakeDirection = DcMotorSimple.Direction.FORWARD
    }

    // Outtake the artifact
    // Oneshot Mode: Accelerates the motor to the desired RPM, outtakes the artifact then powers off
    // the flywheel
    if (gamepad1.aWasPressed()) {
      outtakeActive = true
      flywheelTimer = runtime + 7.0
    }

    // Outtake the artifact
    // Toggle Mode: Toggles on and off the flywheel motor
    if (gamepad1.bWasPressed()) {
      outtakeActive = !outtakeActive
    }

    if (outtakeActive && runtime >= flywheelTimer && flywheelTimer != 0.0) {
      outtakeActive = false
      flywheelTimer = 0.0
    }

    leftDrive.power = leftPower
    rightDrive.power = rightPower

    intake.direction = intakeDirection
    intake.power = if (intakeActive) 1.0 else 0.0
    flywheel.power = if (outtakeActive) 0.6 else 0.0

    telemetry.addData("Status", "Running")
    telemetry.addData("Drive", "L: %.2f, R: %.2f", leftPower, rightPower)

    telemetry.addLine()

    telemetry.addData("Intake Active", "%b", intakeActive)

    if (intakeActive) {
      telemetry.addData(
              "Intake Mode",
              "%s",
              if (intakeDirection == DcMotorSimple.Direction.REVERSE) "Feeding..."
              else "Reverse Feeding..."
      )
    }

    telemetry.addLine()

    telemetry.addData("Outtake Active", "%b", outtakeActive)
    telemetry.addData("Flywheel RPM", "%f", 0.0)

    if (outtakeActive && flywheelTimer > runtime) {
      telemetry.addData("Flywheel Timer", "%.1f s", flywheelTimer - runtime)
    }

    telemetry.update()
  }
}
