/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.moyinoluwa.camerabarcodedetection

import android.graphics.Canvas
import android.graphics.Color.BLUE
import android.graphics.Color.MAGENTA
import android.graphics.Paint
import com.google.android.gms.vision.barcode.Barcode
import com.moyinoluwa.camerabarcodedetection.ui.camera.GraphicOverlay

/**
 * Graphic instance for rendering barcode position, orientation, and landmarks within an associated
 * graphic overlay view.
 */

private const val BOX_STROKE_WIDTH = 5.0f
private const val ID_TEXT_SIZE = 40.0f

class BarcodeGraphic(overlay: GraphicOverlay) : GraphicOverlay.Graphic(overlay) {

    private val idPaint = Paint().apply {
        color = BLUE
        textSize = ID_TEXT_SIZE
    }

    private val boxPaint = Paint().apply {
        color = MAGENTA
        style = Paint.Style.STROKE
        strokeWidth = BOX_STROKE_WIDTH
    }

    @Volatile private var barcode: Barcode? = null
    private var barcodeId: Int = 0

    fun setId(id: Int) {
        barcodeId = id
    }

    /**
     * Updates the barcode instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    fun updateBarcode(barcode: Barcode) {
        this.barcode = barcode
        postInvalidate()
    }

    /**
     * Draws the barcode annotations for position on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        val barcode = barcode ?: return

        // Draws a bounding box around the barcode.
        val barcodeBoundingBox = barcode.boundingBox

        val x = translateX(barcodeBoundingBox.left.toFloat() + barcodeBoundingBox.width() / 2)
        val y = translateY(barcodeBoundingBox.top.toFloat() + barcodeBoundingBox.height() / 2)

        val xOffset = scaleX(barcodeBoundingBox.width() / 2.0f)
        val yOffset = scaleY(barcodeBoundingBox.height() / 2.0f)
        val left = x - xOffset
        val top = y - yOffset
        val right = x + xOffset
        val bottom = y + yOffset

        canvas.drawRect(left, top, right, bottom, boxPaint)

        // Draws the data on the screen
        val barcodeData = barcode.rawValue
        canvas.drawText(barcodeData, left, top, idPaint)
    }
}