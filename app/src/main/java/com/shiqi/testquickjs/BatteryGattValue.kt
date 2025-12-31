package com.shiqi.testquickjs

import android.util.Log
import com.shiqi.quickjs.JSContext
import com.shiqi.quickjs.JSNumber
import kotlin.math.roundToInt

/**
 * 一個純粹的業務邏輯類，專注於從一個準備好的 JSContext 中獲取電池電量數據。
 */
class BatteryGattValue(
    private val jsContext: JSContext
) : IGattValue {

    private val TAG = "BatteryGattValue"

    private val level: Int get() {
        Log.d(TAG, "[DEBUG] get() called. Using 2-step evaluation for battery.")

        //// 步驟 1: 執行 decreaseBattery()，觸發其副作用（更新 batteryLevel 全域變數）
//        jsContext.evaluate("decreaseBattery()")
//        executePendingJobs() // 確保 console.log 被執行
//
//        // 步驟 2: 透過 globalObject 明確地獲取 'batteryLevel' 變數。
//        val result = jsContext.globalObject.getProperty("batteryLevel")
//
//        if (result is JSNumber) {
//            // JS 中的 number 預設是 double，安全地獲取並轉換
//            val doubleValue = result.getDouble()
//            val intValue = doubleValue.roundToInt()
//            Log.d(TAG, "[DEBUG] Successfully get 'batteryLevel' from globalObject. Value: $intValue")
//            return intValue
//        }
//
//        val resultType = result?.javaClass?.simpleName ?: "null"
//        Log.e(TAG, "[DEBUG] FAILED! Could not get 'batteryLevel' from globalObject. Result was '$resultType'.")
//        return -1 // 回傳錯誤碼
        jsContext.evaluate("__temp_result = getValue('2A19');")
        val result = jsContext.globalObject.getProperty("__temp_result")

        if (result is JSNumber) {
            return result.int
        }

        Log.e(TAG, "Failed to get Battery value via 'getValue(\'2A19\')'. Result was ${result?.javaClass?.simpleName}")
        return -1
    }

    private fun executePendingJobs() {
        try {
            var hasPendingJob: Boolean
            do {
                hasPendingJob = jsContext.executePendingJob()
            } while (hasPendingJob)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "[DEBUG] Could not execute pending jobs, context might be closed.", e)
        }
    }

    /**
     * 根據藍牙 GATT 規範，Battery Level (0x2A19) 是一個 uint8 的值，代表 0-100 的百分比。
     */
    override val gattValue: ByteArray
        get() = byteArrayOf(level.toByte())
}
