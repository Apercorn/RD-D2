package org.firstinspires.ftc.teamcode

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.Servo

@TeleOp(name = "Competition TeleOp", group = "Competition")
class CompetitionTeleOp : OpMode() {
  private lateinit var leftDrive: DcMotorEx
  private lateinit var rightDrive: DcMotorEx
  private lateinit var intake: DcMotorEx
  private lateinit var flywheel: DcMotorEx

  private lateinit var gateLeft: Servo
  private lateinit var gateRight: Servo

  private var intakeActive = false
  private var outtakeActive = false
  private var precisionMode = false

  private var flywheelTimer = 0.0
  private var flywheelAtRPM = false

  private var intakeDirection = DcMotorSimple.Direction.REVERSE

  // PIDF coefficients for flywheel velocity control
  private val flywheelP = 10.0
  private val flywheelI = 3.0
  private val flywheelD = 0.0
  private val flywheelF = 12.5

  // Target velocity in ticks per second (adjust based on your motor and gearing)
  private var targetFlywheelVelocity = 5000.0 // Adjust this value for desired RPM
  private val maxFlywheelVelocity = 5000.0 // Maximum velocity for long shots
  private val rpmStepSize = 250.0 // RPM step size for dpad adjustments (in ticks/sec)
  private val flywheelRPMTolerance = 50.0 // RPM tolerance for "at speed" detection

  override fun init() {
    leftDrive = hardwareMap.get(DcMotorEx::class.java, "left_drive")
    rightDrive = hardwareMap.get(DcMotorEx::class.java, "right_drive")
    intake = hardwareMap.get(DcMotorEx::class.java, "intake")
    flywheel = hardwareMap.get(DcMotorEx::class.java, "outtake")

    gateLeft = hardwareMap.get(Servo::class.java, "gate_left")
    gateRight = hardwareMap.get(Servo::class.java, "gate_right")

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

    // Configure PIDF for flywheel velocity control
    flywheel.setVelocityPIDFCoefficients(flywheelP, flywheelI, flywheelD, flywheelF)

    telemetry.addData("Status", "TeleOp Initialized")
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
      intakeDirection = DcMotorSimple.Direction.FORWARD
    }

    // Run the intake backwards
    if (gamepad1.leftTriggerWasPressed()) {
      intakeActive = !intakeActive
      intakeDirection = DcMotorSimple.Direction.REVERSE
    }

    // Outtake the artifact
    // Oneshot Mode: Accelerates the motor to the desired RPM, outtakes the artifact then powers off
    // the flywheel
    if (gamepad1.aWasPressed()) {
      outtakeActive = true
      flywheelTimer = runtime + 5.0
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

    // Toggle precision driving mode
    if (gamepad1.left_bumper) {
      precisionMode = true
    } else {
      precisionMode = false
    }

    // Flywheel RPM presets and adjustments
    if (gamepad1.dpad_up) {
      // Long shot: max RPM
      targetFlywheelVelocity = maxFlywheelVelocity
    }

    if (gamepad1.dpad_down) {
      // Short shot: half max RPM
      targetFlywheelVelocity = 2000.0
    }

    if (gamepad1.dpad_right) {
      // Increase RPM
      targetFlywheelVelocity =
              (targetFlywheelVelocity + rpmStepSize).coerceAtMost(maxFlywheelVelocity)
    }

    if (gamepad1.dpad_left) {
      // Decrease RPM
      targetFlywheelVelocity = (targetFlywheelVelocity - rpmStepSize).coerceAtLeast(0.0)
    }

    // Apply precision mode speed limiter
    val speedMultiplier = if (precisionMode) 0.4 else 1.0
    leftDrive.power = leftPower * speedMultiplier
    rightDrive.power = rightPower * speedMultiplier

    intake.direction = intakeDirection
    intake.power = if (intakeActive) 1.0 else 0.0

    // Calculate flywheel RPM values once
    val currentVelocity = flywheel.velocity
    val currentRPM = (currentVelocity * 60.0) / flywheel.motorType.ticksPerRev
    val targetRPM = (targetFlywheelVelocity * 60.0) / flywheel.motorType.ticksPerRev

    // Use velocity control for flywheel with PIDF
    if (outtakeActive) {
      flywheel.velocity = targetFlywheelVelocity
      flywheelAtRPM = kotlin.math.abs(currentRPM - targetRPM) < flywheelRPMTolerance
    } else {
      flywheel.velocity = 0.0
      flywheelAtRPM = false
    }

    // Telemetry
    telemetry.addData("Status", "Running")
    telemetry.addData("Drive L", leftPower * speedMultiplier)
    telemetry.addData("Drive R", rightPower * speedMultiplier)
    //telemetry.addData("Precision Mode", if (precisionMode) "ON (40%)" else "OFF")

    telemetry.addLine()

    telemetry.addData("Intake Active", intakeActive)
    if (intakeActive) {
      val mode =
              if (intakeDirection == DcMotorSimple.Direction.REVERSE) "Feeding..."
              else "Reverse Feeding..."
      telemetry.addData("Intake Mode", mode)
    }

    telemetry.addLine()

    telemetry.addData("Outtake Active", outtakeActive)
    if (outtakeActive) {
      telemetry.addData("Flywheel Current RPM", "%.0f".format(currentRPM))
      telemetry.addData("Flywheel Target RPM", "%.0f".format(targetRPM))
      telemetry.addData("Flywheel At Speed", flywheelAtRPM)
      if (flywheelTimer > runtime) {
        telemetry.addData("Flywheel Timer", "%.1f s".format(flywheelTimer - runtime))
      }
    } else {
      telemetry.addData("Flywheel RPM", "0")
    }

    telemetry.update()
  }
}
