package com.practicestock.metatrader5.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

// 外汇品种数据类
data class ForexPair(val symbol: String, val baseCurrency: String, val contractSize: Int, val pipValue: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(appPreferences: AppPreferences, onNavigateToSettings: () -> Unit) {
    // 常见外汇品种列表，包含基础货币、合约规模和每点价值
    val forexPairs = listOf(
        ForexPair("EUR/USD", "EUR", 100000, 10.0),
        ForexPair("USD/JPY", "USD", 100000, 8.33),
        ForexPair("GBP/USD", "GBP", 100000, 10.0),
        ForexPair("USD/CHF", "USD", 100000, 10.0),
        ForexPair("AUD/USD", "AUD", 100000, 10.0),
        ForexPair("USD/CAD", "USD", 100000, 10.0)
    )

    var selectedPair by remember { mutableStateOf(forexPairs[0]) }
    var showPairDropdown by remember { mutableStateOf(false) }
    var entryPrice by remember { mutableStateOf("") }
    var exitPrice by remember { mutableStateOf("") }
    var lotSize by remember { mutableStateOf("") }
    var marginPerLot by remember { mutableStateOf("") }

    fun calculateLotSize() {
        try {
            val entry = entryPrice.toDouble()
            val capital = appPreferences.initialCapital
            val leverage = appPreferences.leverage
            val stopLossPercent = appPreferences.stopLossPercentage / 100

            // 第一步：计算1标准手所需保证金
            // 单标准手保证金 =（合约规模 × 当前汇率）÷ 杠杆
            val margin = (selectedPair.contractSize * entry) / leverage
            marginPerLot = String.format("%.2f", margin)

            // 第二步：计算止损金额
            val stopLossAmount = capital * stopLossPercent

            // 第三步：计算点数差
            val pipDifference = Math.abs(entry - exitPrice.toDouble())

            // 第四步：计算可开仓数量（以损定量）
            // 可开仓数量 = 止损金额 ÷ (点数差 × 每手价值)
            val calculatedLotSize = stopLossAmount / (pipDifference * selectedPair.pipValue)

            // 第五步：同时计算基于保证金的最大可开手数
            val maxLotByMargin = capital / margin

            // 取较小值作为最终可开仓数量
            val finalLotSize = Math.min(calculatedLotSize, maxLotByMargin)

            // 保留两位小数
            lotSize = String.format("%.2f", finalLotSize)
        } catch (e: NumberFormatException) {
            lotSize = "请输入有效的价格"
            marginPerLot = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "外汇交易计算器", modifier = Modifier.padding(bottom = 16.dp))

        // 设置按钮
        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(text = "设置")
        }

        // 外汇品种选择
        Text(text = "外汇品种", modifier = Modifier.padding(bottom = 8.dp))
        Box {
            Button(
                onClick = { showPairDropdown = !showPairDropdown },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(text = selectedPair.symbol)
            }
            DropdownMenu(
                expanded = showPairDropdown,
                onDismissRequest = { showPairDropdown = false }
            ) {
                forexPairs.forEach { pair ->
                    DropdownMenuItem(
                        onClick = {
                            selectedPair = pair
                            showPairDropdown = false
                            calculateLotSize()
                        },
                        text = { Text(text = pair.symbol) }
                    )
                }
            }
        }



        // 开仓价格
        Text(text = "开仓价格", modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = entryPrice,
            onValueChange = {
                entryPrice = it
                calculateLotSize()
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 平仓价格
        Text(text = "平仓价格", modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = exitPrice,
            onValueChange = {
                exitPrice = it
                calculateLotSize()
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 计算结果
        Text(text = "可开仓数量 (手)", modifier = Modifier.padding(bottom = 8.dp))
        Text(text = lotSize, modifier = Modifier.padding(bottom = 16.dp))

        // 1标准手保证金信息
        if (marginPerLot.isNotEmpty()) {
            Text(text = "1标准手所需保证金 (USD):", modifier = Modifier.padding(bottom = 8.dp))
            Text(text = marginPerLot, modifier = Modifier.padding(bottom = 16.dp))
        }

        // 当前设置信息
        Text(text = "当前设置:", modifier = Modifier.padding(bottom = 8.dp))
        Text(
            text = "初始资金: ${appPreferences.initialCapital} USD, 止损比例: ${appPreferences.stopLossPercentage}%, 杠杆: ${appPreferences.leverage}倍",
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
