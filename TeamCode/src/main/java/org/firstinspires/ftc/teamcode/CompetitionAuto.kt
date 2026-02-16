package org.firstinspires.ftc.teamcode

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import org.firstinspires.ftc.teamcode.drive.DriveConstants

/*
 * Competition Autonomous OpMode
 *
 * This is a starter template. Build your trajectories based on your
 * game-specific strategy once DriveConstants are tuned.
 *
 * Coordinate system (field-centric):
 *   +X = toward the audience (forward from red alliance wall)
 *   +Y = toward the red alliance driver station (left from robot perspective)
 *   Heading 0 = facing +X direction
 *
 * Tuning order:
 *   1. Set motor/wheel constants in DriveConstants.kt
 *   2. Run feedforward tuner → set kV, kA, kStatic
 *   3. Run TrackWidthTuner → refine TRACK_WIDTH
 *   4. Run TurnTest → tune HEADING_PID
 *   5. Run FollowerPIDTuner → tune TRANSLATIONAL_PID
 *   6. Run StraightTest / SplineTest to verify
 *   7. Build your competition trajectories below
 */
@Autonomous(name = "Competition Auto", group = "Competition")
class CompetitionAuto : LinearOpMode() {
  private lateinit var leftDrive: DcMotorEx
  private lateinit var rightDrive: DcMotorEx
  private lateinit var intake: DcMotorEx
  private lateinit var flywheel: DcMotorEx
  private lateinit var gateLeft: Servo
  private lateinit var gateRight: Servo

  override fun runOpMode() {
    // Initialize hardware
    leftDrive = hardwareMap.get(DcMotorEx::class.java, "left_drive")
    rightDrive = hardwareMap.get(DcMotorEx::class.java, "right_drive")
    intake = hardwareMap.get(DcMotorEx::class.java, "intake")
    flywheel = hardwareMap.get(DcMotorEx::class.java, "outtake")
    gateLeft = hardwareMap.get(Servo::class.java, "gate_left")
    gateRight = hardwareMap.get(Servo::class.java, "gate_right")

    leftDrive.direction = DriveConstants.LEFT_DIRECTION
    rightDrive.direction = DriveConstants.RIGHT_DIRECTION

    leftDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    rightDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER

    leftDrive.mode = DcMotor.RunMode.RUN_USING_ENCODER
    rightDrive.mode = DcMotor.RunMode.RUN_USING_ENCODER

    leftDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
    rightDrive.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE

    // Define starting position on the field
    // TODO: Set this to your actual starting position
    val startPose = Pose2d(0.0, 0.0, Math.toRadians(0.0))

    /*
     * ============================
     * BUILD TRAJECTORIES
     * ============================
     * Build all trajectories during init so they're ready immediately
     * when the OpMode starts.
     *
     * Uncomment and modify these once your DriveConstants are tuned.
     *
     * Example trajectory sequence:
     *
     * val trajectory1 = drive.trajectoryBuilder(startPose)
     *     .forward(24.0)                    // Drive forward 24 inches
     *     .build()
     *
     * val trajectory2 = drive.trajectoryBuilder(trajectory1.end())
     *     .strafeRight(12.0)                // Strafe right 12 inches
     *     .build()
     *
     * val trajectory3 = drive.trajectoryBuilder(trajectory2.end())
     *     .splineTo(Vector2d(36.0, 36.0), Math.toRadians(90.0))  // Spline to position
     *     .build()
     *
     * Available trajectory methods:
     *   .forward(distance)         - Drive forward
     *   .back(distance)            - Drive backward
     *   .strafeLeft(distance)      - Strafe left
     *   .strafeRight(distance)     - Strafe right
     *   .splineTo(pos, heading)    - Smooth curve to position
     *   .lineTo(pos)               - Straight line to position
     *   .lineToLinearHeading(pos, heading) - Line with heading interpolation
     */

    telemetry.addData("Status", "Initialized")
    telemetry.addData("Start Pose", startPose.toString())
    telemetry.addLine()
    telemetry.addLine("⚠ Make sure DriveConstants are tuned!")
    telemetry.addLine("See: learnroadrunner.com")
    telemetry.update()

    // Wait for the driver to press START
    waitForStart()

    if (isStopRequested) return

    /*
     * ============================
     * EXECUTE AUTONOMOUS SEQUENCE
     * ============================
     * Follow trajectories and perform actions.
     *
     * Example:
     *
     * // Step 1: Drive to scoring position
     * drive.followTrajectory(trajectory1)
     *
     * // Step 2: Score the sample
     * intake.power = -1.0
     * sleep(500)
     * intake.power = 0.0
     *
     * // Step 3: Drive to next position
     * drive.followTrajectory(trajectory2)
     *
     * // Step 4: Park
     * drive.followTrajectory(trajectory3)
     */

    // TODO: Add your autonomous sequence here

    telemetry.addData("Status", "Autonomous Complete")
    telemetry.update()
  }
}
