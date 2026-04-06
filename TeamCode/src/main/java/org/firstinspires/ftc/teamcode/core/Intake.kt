package org.firstinspires.ftc.teamcode.core

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.Servo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Intake subsystem for feeding artifacts into the robot.
 *
 * Supports forward/reverse direction and a windmill (slow) mode for gentle handling of
 * already-captured artifacts.
 *
 * Usage:
 * ```
 * intake.start()           // Start intake forward
 * intake.reverse()         // Start intake in reverse
 * intake.stop()            // Stop intake
 * intake.toggle()          // Toggle on/off
 * intake.windmill = true   // Enable slow mode
 * ```
 */
class Intake(private val motor: DcMotorEx, private val feeder: Servo) {

  companion object {
    /** Power applied during windmill mode. */
    const val WINDMILL_PWR = 0.3

    /** Power applied during normal operation. */
    const val FULL_PWR = 1.0

    /**
     * Time in milliseconds to wait for the feeder servo to reach the start position before
     * sweeping.
     */
    const val FEEDER_RESET_DELAY = 500L

    const val FEEDER_SWEEP_END = 0.7
    const val FEEDER_SWEEP_START = 0.1
  }

  /** Whether the intake is currently running. */
  var active = false
    private set

  /** Current intake direction. */
  var direction: DcMotorSimple.Direction = DcMotorSimple.Direction.FORWARD
    private set

  /** When true, intake runs at reduced [windmillPower] to prevent jams. */
  var windmill = false

  /** Whether the feeder is currently mid-sweep (prevents double-triggering). */
  var feederBusy = false
    private set

  /** Start the intake running forward. */
  fun start() {
    active = true
    direction = DcMotorSimple.Direction.FORWARD
    apply()
  }

  /** Start the intake running in reverse. */
  fun reverse() {
    active = true
    direction = DcMotorSimple.Direction.REVERSE
    apply()
  }

  /** Stop the intake. */
  fun stop() {
    active = false
    apply()
  }

  /** Toggle the intake on/off in the current direction. */
  fun toggle() {
    active = !active
    apply()
  }

  /** Toggle with a specific direction. */
  fun toggle(dir: DcMotorSimple.Direction) {
    active = !active
    direction = dir
    apply()
  }

  /** Toggle windmill mode on/off. */
  fun toggleWindmill() {
    windmill = !windmill
    apply()
  }

  /**
   * Sweep the feeder servo from start → end to push the artifact into the second intake wheels.
   *
   * Moves to [feederStart] first, waits [feederResetDelayMs] for the servo to arrive, then swings
   * to [feederEnd]. Ignores repeat calls while a sweep is in progress.
   *
   * @param scope CoroutineScope to launch the delayed sweep in.
   */
  fun feed(scope: CoroutineScope) {
    if (feederBusy) return

    feederBusy = true
    feeder.position = FEEDER_SWEEP_END

    scope.launch {
      delay(FEEDER_RESET_DELAY)
      feeder.position = FEEDER_SWEEP_START
      feederBusy = false
    }
  }

  fun resetFeeder() {
    feeder.position = FEEDER_SWEEP_START
  }

  /** Apply current state to the motor. Call once per loop if changing state externally. */
  fun apply() {
    motor.direction = direction
    motor.power =
            if (active) {
              if (windmill) WINDMILL_PWR else FULL_PWR
            } else {
              0.0
            }
  }

  /** Current motor power being applied. */
  val power: Double
    get() = motor.power
}
