package org.firstinspires.ftc.teamcode.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Size
import com.bylazar.camerastream.PanelsCameraStream
import com.qualcomm.robotcore.hardware.HardwareMap
import java.util.concurrent.atomic.AtomicReference
import org.firstinspires.ftc.robotcore.external.function.Consumer
import org.firstinspires.ftc.robotcore.external.function.Continuation
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.robotcore.external.stream.CameraStreamSource
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.VisionProcessor
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import org.opencv.android.Utils
import org.opencv.core.Mat

/**
 * Vision subsystem for AprilTag detection with Panels camera stream.
 *
 * Encapsulates VisionPortal, AprilTagProcessor, and the Panels FrameProcessor so TeleOp and Auto
 * can share the same vision logic.
 *
 * Usage:
 * ```
 * val vision = VisionSubsystem(hardwareMap)
 * vision.startStreaming()        // Start Panels camera stream
 *
 * val tags = vision.detections   // List<AprilTagDetection>
 * val tag = vision.getTag(5)     // Get specific tag by ID
 *
 * // Auto-alignment: returns (steer, range) or null
 * val align = vision.alignToTag(5)
 * if (align != null) {
 *     drive.setPower(-align.steer, align.steer) // Turn toward tag
 * }
 *
 * vision.close()                 // Clean up when done
 * ```
 */
class Vision(
	hardwareMap: HardwareMap,
	cameraName: String = "Camera",
	resolution: Size = Size(640, 480),
	decimation: Float = 2.0f
) {

	/** Frame processor for Panels dashboard camera stream. */
	private val frameProcessor = FrameProcessor()

	/** AprilTag processor for tag detection. */
	val aprilTag: AprilTagProcessor =
		AprilTagProcessor.Builder().build().also { it.setDecimation(decimation) }

	/** FTC Vision portal. */
	val visionPortal: VisionPortal =
		VisionPortal.Builder()
			.setCamera(hardwareMap.get(WebcamName::class.java, cameraName))
			.setCameraResolution(resolution)
			.addProcessor(aprilTag)
			.addProcessor(frameProcessor)
			.build()

	private var streaming = false

	// ── Panels Camera Stream ──

	/** Start streaming frames to the Panels dashboard. */
	fun startStreaming() {
		PanelsCameraStream.startStream(frameProcessor)
		streaming = true
	}

	/** Stop the Panels stream (saves CPU). */
	fun stopStreaming() {
		PanelsCameraStream.stopStream()
		streaming = false
	}

	/** Whether the Panels stream is active. */
	val isStreaming: Boolean
		get() = streaming

	/** Pause the VisionPortal camera (saves more CPU). */
	fun pauseCamera() = visionPortal.stopStreaming()

	/** Resume the VisionPortal camera. */
	fun resumeCamera() = visionPortal.resumeStreaming()

	// ── AprilTag Detections ──

	/** All currently-detected AprilTags. */
	val detections: List<AprilTagDetection>
		get() = aprilTag.detections

	/** Number of detected tags. */
	val tagCount: Int
		get() = detections.size

	/** Get a specific tag detection by ID, or null if not visible. */
	fun getTag(id: Int): AprilTagDetection? = detections.firstOrNull { it.id == id }

	/** Check whether a specific tag is currently visible. */
	fun isTagVisible(id: Int): Boolean = getTag(id) != null

	// ── Auto-Alignment ──

	/**
	 * Alignment result containing steering correction and range info.
	 *
	 * @property steer Normalized steering value (-1..1). Negative = turn left, positive = turn right.
	 * @property range Distance to the tag in inches.
	 * @property bearing Horizontal angle to the tag in degrees.
	 * @property yaw Tag rotation relative to camera in degrees.
	 */
	data class AlignmentResult(
		val steer: Double,
		val range: Double,
		val bearing: Double,
		val yaw: Double
	)

	/**
	 * Proportional gain for bearing correction in alignment. Higher values = more aggressive turning
	 * toward the tag.
	 */
	var bearingGain = 0.02

	/** Proportional gain for yaw correction in alignment. Helps the robot face the tag head-on. */
	var yawGain = 0.01

	/**
	 * Calculate steering correction to align with a target AprilTag.
	 *
	 * Uses proportional control based on bearing (horizontal offset) and yaw (rotation offset) to
	 * generate a steering value.
	 *
	 * @param tagId The AprilTag ID to align with.
	 * @return AlignmentResult with steer value, or null if the tag is not visible.
	 */
	fun alignToTag(tagId: Int): AlignmentResult? {
		val tag = getTag(tagId) ?: return null
		val pose = tag.ftcPose ?: return null

		val steer = (pose.bearing * bearingGain) + (pose.yaw * yawGain)

		return AlignmentResult(
			steer = steer.coerceIn(-1.0, 1.0),
			range = pose.range,
			bearing = pose.bearing,
			yaw = pose.yaw
		)
	}

	/**
	 * Calculate drive power to approach a tag at a desired range.
	 *
	 * @param tagId The AprilTag ID to approach.
	 * @param desiredRange Target distance from the tag in inches.
	 * @param rangeGain Proportional gain for range correction.
	 * @return Drive power (-1..1), or null if the tag is not visible.
	 */
	fun approachTag(tagId: Int, desiredRange: Double, rangeGain: Double = 0.03): Double? {
		val tag = getTag(tagId) ?: return null
		val pose = tag.ftcPose ?: return null

		val rangeError = pose.range - desiredRange
		return (rangeError * rangeGain).coerceIn(-1.0, 1.0)
	}

	// ── Cleanup ──

	/** Close the VisionPortal and stop streaming. Call in OpMode stop(). */
	fun close() {
		if (streaming) stopStreaming()
		visionPortal.close()
	}

	// ── Frame Processor (inner class) ──

	/** Captures camera frames for streaming to Panels dashboard. */
	private class FrameProcessor : VisionProcessor, CameraStreamSource {
		private val lastFrame = AtomicReference(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565))

		override fun init(width: Int, height: Int, calibration: CameraCalibration?) {
			lastFrame.set(Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565))
		}

		override fun processFrame(frame: Mat, captureTimeNanos: Long): Any? {
			val b = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.RGB_565)
			Utils.matToBitmap(frame, b)
			lastFrame.set(b)
			return null
		}

		override fun onDrawFrame(
			canvas: Canvas,
			onscreenWidth: Int,
			onscreenHeight: Int,
			scaleBmpPxToCanvasPx: Float,
			scaleCanvasDensity: Float,
			userContext: Any?
		) {
		}

		override fun getFrameBitmap(continuation: Continuation<out Consumer<Bitmap>>) {
			continuation.dispatch { it.accept(lastFrame.get()) }
		}
	}
}
