package com.fuxy.cookeasy.entity

import android.graphics.Bitmap
import android.net.Uri

data class EditStep(
    var stepNumber: Int? = null,
    var description: String? = null,
    var imageUri: Uri? = null,
    var imageBitmap: Bitmap? = null
)