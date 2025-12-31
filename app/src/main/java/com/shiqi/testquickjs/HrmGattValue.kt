package com.shiqi.testquickjs

import android.util.Log
import com.shiqi.quickjs.JSContext
import com.shiqi.quickjs.JSNumber
import kotlin.math.roundToInt

/**
 * 一個純粹的業務邏輯類，專注於從一個準備好的 JSContext 中獲取心率數據。
 * 它不再持有任何 Android Context。
 */
class HrmGattValue(
    private val jsContext: JSContext
) : IGattValue {

    private val TAG = "HrmGattValue"

    // init 區塊已被移除，因為腳本的加載和執行責任已上移。

    private val bpm: Int get() {
        Log.d(TAG, "[DEBUG] get() called. Using 2-step evaluation.")

//        // 步驟 1: 執行 next()，僅為了觸發其副作用 (更新 bpm 全域變數和 console.log)
//        // 我們忽略它的回傳值，因為我們知道它會被 console.log 污染成 null。
//        jsContext.evaluate("next()")
//        executePendingJobs() // 確保 console.log 被執行
//
//        // 步驟 2: 透過 globalObject 明確地獲取 'bpm' 變數。
//        // 這是在 Java 層訪問 JS 全域變數的唯一正確方法。
//        val result = jsContext.globalObject.getProperty("bpm")
//
//        if (result is JSNumber) {
//            val doubleValue = result.getDouble()
//            val bpmValue = doubleValue.roundToInt()
//            Log.d(TAG, "[DEBUG] Successfully get 'bpm' from globalObject. DoubleValue: $doubleValue, RoundedInt: $bpmValue")
//            return bpmValue
//        }
//
//        val resultType = result?.javaClass?.simpleName ?: "null"
//        Log.e(TAG, "[DEBUG] FAILED! Could not get 'bpm' from globalObject. Result was '$resultType'.")
//        return -1
        jsContext.evaluate("__temp_result = getValue('2A37');")

        // 步驟 2: 安全地從 globalObject 讀取臨時變數
        val result = jsContext.globalObject.getProperty("__temp_result")

        if (result is JSNumber) {
            return result.int
        }

        Log.e(TAG, "Failed to get HRM value via 'getValue(\'2A37\')'. Result was ${result?.javaClass?.simpleName}")
        return -1 // 如果獲取失敗，回傳 -1
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

    override val gattValue: ByteArray
        get() = byteArrayOf(
            0x00,      // Flags: uint8 HR
            bpm.toByte()
        )
}
