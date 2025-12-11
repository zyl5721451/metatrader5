package com.practicestock.metatrader5.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.practicestock.metatrader5.data.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(appPreferences: AppPreferences, onNavigateBack: () -> Unit) {
    var initialCapital by remember { mutableStateOf(appPreferences.initialCapital.toString()) }
    var stopLossPercentage by remember { mutableStateOf(appPreferences.stopLossPercentage.toString()) }
    var leverage by remember { mutableStateOf(appPreferences.leverage.toString()) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "设置", modifier = Modifier.padding(bottom = 16.dp))

        Text(text = "初始资金量 (USD)", modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = initialCapital,
            onValueChange = { initialCapital = it },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(text = "止损比例 (%)", modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = stopLossPercentage,
            onValueChange = { stopLossPercentage = it },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(text = "杠杆倍数", modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = leverage,
            onValueChange = { leverage = it },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                try {
                    val capital = initialCapital.toDouble()
                    val percentage = stopLossPercentage.toDouble()
                    val leverageValue = leverage.toInt()
                    appPreferences.initialCapital = capital
                    appPreferences.stopLossPercentage = percentage
                    appPreferences.leverage = leverageValue
                    message = "设置已保存"
                } catch (e: NumberFormatException) {
                    message = "请输入有效的数字"
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(text = "保存设置")
        }

        Text(text = message, modifier = Modifier.padding(bottom = 16.dp))

        Button(onClick = onNavigateBack) {
            Text(text = "返回")
        }
    }
}
