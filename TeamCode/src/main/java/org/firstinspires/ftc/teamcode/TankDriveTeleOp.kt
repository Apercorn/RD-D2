package org.firstinspires.ftc.teamcode

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple

@TeleOp(name = "Kotlin TeleOp", group = "Kotlin")
class TankDriveTeleOp : OpMode() {
    private lateinit var leftDrive: DcMotorEx
    private lateinit var rightDrive: DcMotorEx

    override fun init() {
        leftDrive = hardwareMap.get(DcMotorEx::class.java, "left_drive")
        rightDrive = hardwareMap.get(DcMotorEx::class.java, "right_drive")

        leftDrive.direction = DcMotorSimple.Direction.REVERSE
        rightDrive.direction = DcMotorSimple.Direction.FORWARD

        telemetry.addData("Status", "Initialized")
        telemetry.update()
    }

    override fun loop() {
        val drive = -gamepad1.left_stick_y.toDouble()
        val turn = gamepad1.right_stick_x.toDouble()

        val leftPower = drive + turn
        val rightPower = drive - turn

        leftDrive.power = leftPower
        rightDrive.power = rightPower

        telemetry.addData("Status", "Running")
        telemetry.addData("Motors", "left (%.2f), right (%.2f)", leftPower, rightPower)
        telemetry.update()
    }
}
