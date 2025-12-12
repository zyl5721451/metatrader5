package com.practicestock.metatrader5.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    var initialCapital: Double
        get() = sharedPreferences.getFloat(KEY_INITIAL_CAPITAL, DEFAULT_INITIAL_CAPITAL.toFloat()).toDouble()
        set(value) = sharedPreferences.edit().putFloat(KEY_INITIAL_CAPITAL, value.toFloat()).apply()

    var stopLossPercentage: Double
        get() = sharedPreferences.getFloat(KEY_STOP_LOSS_PERCENTAGE, DEFAULT_STOP_LOSS_PERCENTAGE.toFloat()).toDouble()
        set(value) = sharedPreferences.edit().putFloat(KEY_STOP_LOSS_PERCENTAGE, value.toFloat()).apply()

    var leverage: Int
        get() = sharedPreferences.getInt(KEY_LEVERAGE, DEFAULT_LEVERAGE)
        set(value) = sharedPreferences.edit().putInt(KEY_LEVERAGE, value).apply()

    companion object {
        private const val PREFERENCES_NAME = "forex_calculator_preferences"
        private const val KEY_INITIAL_CAPITAL = "initial_capital"
        private const val KEY_STOP_LOSS_PERCENTAGE = "stop_loss_percentage"
        private const val KEY_LEVERAGE = "leverage"
        const val DEFAULT_INITIAL_CAPITAL = 10000.0
        const val DEFAULT_STOP_LOSS_PERCENTAGE = 1.0
        const val DEFAULT_LEVERAGE = 20
    }
}
