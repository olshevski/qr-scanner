package dev.olshevski.qrscanner

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.util.Size
import android.util.SizeF
import androidx.annotation.WorkerThread
import androidx.camera.core.ImageProxy
import androidx.core.graphics.toRect
import androidx.core.graphics.toRectF

private const val TAG = "CropUtils"

/**
 * @param barcodeRect barcode area within the [previewSize]
 * @param previewSize the dimensions of the preview area
 * @param image the taken hi-res photo
 *
 * @return cropped QR code bitmap, or  `null` when the cropping process failed for some reason
 */
@WorkerThread
fun cropQRCodeFromImage(
    barcodeRect: Rect,
    previewSize: Size,
    image: ImageProxy
): Bitmap? = try {
    val imageRotation = image.imageInfo.rotationDegrees
    val mappedBarcodeRect = mapBarcodeRectangleToImage(
        barcodeRect = barcodeRect,
        previewSize = previewSize,
        imageSize = image.size,
        imageCropRect = image.cropRect,
        imageRotation = imageRotation
    )

    val matrix = Matrix()
    matrix.setRotate(imageRotation.toFloat())

    Bitmap.createBitmap(
        image.toBitmap(),
        mappedBarcodeRect.left,
        mappedBarcodeRect.top,
        mappedBarcodeRect.width(),
        mappedBarcodeRect.height(),
        matrix,
        false
    )
} catch (e: Exception) {
    Log.e(TAG, "failed to crop QR code", e)
    null
}

/**
 * Preview and image have different dimensions and rotation. This function properly re-maps the
 * barcode area [barcodeRect] to the whole image. This way we can later cut the barcode from
 * the hi-res image.
 *
 * The returned rectangle is guaranteed to be within [imageSize] dimensions.
 *
 * @param barcodeRect barcode area within the [previewSize]
 * @param previewSize the dimensions of the preview area
 * @param imageSize whole image size
 * @param imageCropRect crop area (showing the same image as the preview area) of the whole image.
 * Dimensions are equal or smaller than [imageSize].
 * @param imageRotation the rotation between the image and the preview
 */
// TODO this may be covered with unit tests
private fun mapBarcodeRectangleToImage(
    barcodeRect: Rect,
    previewSize: Size,
    imageSize: Size,
    imageCropRect: Rect,
    imageRotation: Int
): Rect {
    val imageCropRectF = imageCropRect.toRectF()
    val previewSizeF = previewSize.toSizeF()
    val barcodeRectF = barcodeRect.toRectF()

    // recalculate barcode rectangle to the image coordinates
    val matrix: Matrix = Matrix()

    // rotate to the image orientation around the preview center
    matrix.setRotate(-imageRotation.toFloat(), previewSizeF.width / 2, previewSizeF.height / 2)

    if (imageRotation % 180 == 90) { // coordinates flip
        // align the preview area left-top corner back to (0, 0)
        matrix.postTranslate(
            (previewSizeF.height - previewSizeF.width) / 2,
            (previewSizeF.width - previewSizeF.height) / 2
        )

        // scale the preview area, flipping the dimensions
        matrix.postScale(
            imageCropRectF.width() / previewSizeF.height,
            imageCropRectF.height() / previewSizeF.width,
        )
    } else {
        // scale the preview area, dimensions do not flip in this case
        matrix.postScale(
            imageCropRectF.width() / previewSizeF.width,
            imageCropRectF.height() / previewSizeF.height,
        )
    }

    // adjust position to the cropped area
    matrix.postTranslate(
        imageCropRectF.left,
        imageCropRectF.top
    )

    val mappedBarcodeRectF = RectF()
    matrix.mapRect(mappedBarcodeRectF, barcodeRectF)
    return mappedBarcodeRectF.toRect() intersectWith imageSize.toRect()
}

private fun Size.toRect() = Rect(0, 0, width, height)

private fun Size.toSizeF() = SizeF(width.toFloat(), height.toFloat())

private val ImageProxy.size get() = Size(width, height)

/**
 * Returns a new [Rect] that is an intersection of the receiver Rect and [other].
 */
private infix fun Rect.intersectWith(other: Rect): Rect {
    val result = Rect()
    // noinspection CheckResult
    result.setIntersect(this, other)
    return result
}