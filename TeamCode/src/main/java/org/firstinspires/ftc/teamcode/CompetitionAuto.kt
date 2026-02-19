package org.firstinspires.ftc.teamcode

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.bylazar.configurables.annotations.Configurable
import com.bylazar.telemetry.JoinedTelemetry
import com.bylazar.telemetry.PanelsTelemetry
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import org.firstinspires.ftc.teamcode.core.Robot
import org.firstinspires.ftc.teamcode.drive.SampleTankDrive

/*
 * Competition Autonomous OpMode
 *
 * Uses the shared Robot class for hardware + subsystems so that
 * hardware init is never duplicated between TeleOp and Auto.
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
@Configurable
@Autonomous(name = "Competition Auto", group = "Competition")
class CompetitionAuto : LinearOpMode() {

  override fun runOpMode() {
    // Initialize shared robot hardware + subsystems
    val robot = Robot(hardwareMap)
    val tm = JoinedTelemetry(PanelsTelemetry.ftcTelemetry, telemetry)

    // Initialize Road Runner drive (uses its own motor references)
    val drive = SampleTankDrive(hardwareMap)

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

    val trajectory1 =
            drive.trajectoryBuilder(startPose)
                    .forward(24.0) // Drive forward 24 inches
                    .build()

    tm.addData("Status", "Initialized")
    tm.addData("Start Pose", startPose.toString())
    tm.addLine()
    tm.addLine("Make sure DriveConstants are tuned!")
    tm.addLine("See: learnroadrunner.com")
    tm.update()

    // Wait for the driver to press START
    waitForStart()

    if (isStopRequested) return

    /*
     * ============================
     * EXECUTE AUTONOMOUS SEQUENCE
     * ============================
     * Follow trajectories and perform actions using subsystems.
     *
     * Example:
     *
     * // Step 1: Drive to scoring position
     * drive.followTrajectory(trajectory1)
     *
     * // Step 2: Score the sample using subsystems
     * robot.intake.start()
     * sleep(500)
     * robot.intake.stop()
     *
     * // Step 3: Shoot
     * robot.shooter.setFlywheelPower(1.0)
     * sleep(1000)
     * robot.shooter.openGate()
     * sleep(500)
     * robot.shooter.closeGate()
     * robot.shooter.stopFlywheel()
     *
     * // Step 4: Park
     * drive.followTrajectory(trajectory2)
     */

    // Execute the trajectory
    drive.followTrajectory(trajectory1)

    // Stop all subsystems
    robot.stopAll()

    tm.addData("Status", "Autonomous Complete")
    tm.update()
  }
}
