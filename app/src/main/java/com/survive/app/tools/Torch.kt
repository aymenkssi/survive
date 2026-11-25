package com.survive.app.tools

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/** Contrôle du flash de l'appareil photo, sans permission caméra (API 23+). */
class Torch(context: Context) {
    private val manager = context.getSystemService(CameraManager::class.java)
    private val cameraId: String? = runCatching {
        manager?.cameraIdList?.firstOrNull {
            manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    val available: Boolean get() = cameraId != null

    fun set(on: Boolean) {
        val id = cameraId ?: return
        runCatching { manager?.setTorchMode(id, on) }
    }
}
