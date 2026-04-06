package org.firstinspires.ftc.teamcode

import com.bylazar.configurables.annotations.Configurable
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.firstinspires.ftc.teamcode.auto.AutoBuilder
import org.firstinspires.ftc.teamcode.auto.AutoRunner
import org.firstinspires.ftc.teamcode.core.Robot

@Configurable
@Autonomous(name = "Competition Auto", group = "Competition")
class CompetitionAuto : LinearOpMode() {
	private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

	override fun runOpMode() {
		val robot = Robot(hardwareMap)
		val runner = AutoRunner(hardwareMap, telemetry)

		val blueAlliance = AutoBuilder()
			.turn(25.0)
			.addDisplacementMarker {
				robot.intake.start()
				robot.shooter.longshot()
			}
			.waitSeconds(3.0)
			.addDisplacementMarker {
				robot.shooter.shoot(coroutineScope)
			}
			.waitSeconds(5.0)
			.addDisplacementMarker {
				robot.shooter.windmill()
			}
			.turn(-25.0)
			.forward(2.8)
			.turn(90.0)
			// pickup spike artifact
			.addDisplacementMarker { robot.intake.start() }
			.forward(4.5)
			.back(4.0)
			.turn(-90.0)
			//.addDisplacementMarker { robot.shooter.longshot() }
			//.forward(4.0)
			//.addDisplacementMarker {
			//	robot.shooter.shoot(coroutineScope)
			//}
			//.waitSeconds(5.0) // wait for all artifacts to launch
			//.addDisplacementMarker {
			//	robot.shooter.windmill()
			//}
			//.back(4.0)
			//.turn(120.0)
			.build()

		val redAlliance = AutoBuilder()
			.turn(-20.0)
			.addDisplacementMarker {
				robot.intake.start()
				robot.shooter.longshot()
			}
			.waitSeconds(3.0)
			.addDisplacementMarker {
				robot.shooter.shoot(coroutineScope)
			}
			.waitSeconds(5.0)
			.addDisplacementMarker {
				robot.shooter.windmill()
			}
			.turn(-20.0)
			.forward(2.0)
			.build()

		telemetry.addData("Status", "Initialized — ready to run")
		telemetry.update()

		waitForStart()

		if (isStopRequested) return

		// Run the full sequence
		runner.run(redAlliance) { isStopRequested }

		// Cleanup
		robot.stopAll()

		telemetry.addData("Status", "Autonomous Complete")
		telemetry.update()
	}
}
