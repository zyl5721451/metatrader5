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
    // 扩展外汇品种列表，包含用户要求的所有品种，更新每点价值
    val forexPairs = listOf(
        ForexPair("EUR/USD", "EUR", 100000, 1.0),    // 每点价值1.0USD/手
        ForexPair("GBP/USD", "GBP", 100000, 1.0),    // 每点价值1.0USD/手
        ForexPair("XAGUSD", "XAG", 5000, 5.0),      // 白银，每点价值5.0USD/手
        ForexPair("XAU/USD", "XAU", 100, 1.0),       // 黄金，每点价值1.0USD/手
        ForexPair("AUD/USD", "AUD", 100000, 1.0),    // 每点价值1.0USD/手
        ForexPair("NZD/USD", "NZD", 100000, 1.0),    // 每点价值1.0USD/手
        ForexPair("USD/CAD", "USD", 100000, 0.73),   // 每点价值0.73USD/手（CAD换算USD）
        ForexPair("USD/CHF", "USD", 100000, 1.26),   // 每点价值1.26USD/手（CHF换算USD）
        ForexPair("USD/CNH", "USD", 100000, 0.14),   // 每点价值0.14USD/手（CNH换算USD）
        ForexPair("USD/JPY", "USD", 100000, 0.64),   // 每点价值0.64USD/手（JPY换算USD）
        ForexPair("USD/SGD", "USD", 100000, 0.77),   // 每点价值0.77USD/手（SGD换算USD）
        ForexPair("AUD/CAD", "AUD", 100000, 0.73)    // 每点价值0.73USD/手（CAD换算USD）
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
            val exit = exitPrice.toDouble()
            val capital = appPreferences.initialCapital
            val leverage = appPreferences.leverage
            val stopLossPercent = appPreferences.stopLossPercentage / 100

            // 第一步：计算止损金额
            val stopLossAmount = capital * stopLossPercent

            // 第二步：计算价格差（止损价差）
            val priceDifference = Math.abs(entry - exit)

            // 第三步：根据货币对类型确定计算方式
            val calculatedLotSize = when {
                // 1. USD为基准货币的货币对（如USD/CNH、USD/CAD等）
                selectedPair.symbol.startsWith("USD/") -> {
                    // USD为基准货币：每手亏损金额 = 止损价差 × 合约规模 × 手数
                    // 手数 = 单笔最大亏损 ÷ (止损价差 × 合约规模)
                    stopLossAmount / (priceDifference * selectedPair.contractSize)
                }
                // 2. 贵金属品种（黄金、白银）
                selectedPair.symbol == "XAU/USD" || selectedPair.symbol == "XAGUSD" -> {
                    // 贵金属：每手亏损金额 = 止损价差 × 合约规模 × 手数
                    // 手数 = 单笔最大亏损 ÷ (止损价差 × 合约规模)
                    stopLossAmount / (priceDifference * selectedPair.contractSize)
                }
                // 3. 其他主要货币对（如EUR/USD、GBP/USD等）
                else -> {
                    // 其他货币对：使用点数计算
                    // 确定点值单位（1点的价格变动）
                    val pipUnit = if (selectedPair.symbol.endsWith("JPY")) {
                        // 日元类货币对，1点=0.001（3位小数）
                        0.001
                    } else {
                        // 其他主要货币对，1点=0.00001（5位小数）
                        0.00001
                    }
                    
                    // 计算实际点数差
                    val actualPipDifference = priceDifference / pipUnit

                    // 可开仓数量 = 止损金额 ÷ (实际点数差 × 每点价值)
                    stopLossAmount / (actualPipDifference * selectedPair.pipValue)
                }
            }

            // 第四步：计算每手所需保证金和基于保证金的最大可开手数
            val marginPerLotValue = if (selectedPair.symbol.startsWith("USD/")) {
                // USD为基准货币：每手保证金 = 合约规模 ÷ 杠杆
                selectedPair.contractSize.toDouble() / leverage.toDouble()
            } else {
                // 其他货币对：每手保证金 =（合约规模 × 当前汇率）÷ 杠杆
                (selectedPair.contractSize.toDouble() * entry) / leverage.toDouble()
            }
            marginPerLot = String.format("%.2f", marginPerLotValue)
            
            // 计算基于保证金的最大可开手数
            val maxLotByMargin = capital / marginPerLotValue

            // 取较小值作为最终可开仓数量
            var finalLotSize = Math.min(calculatedLotSize, maxLotByMargin)

            // 调整取整规则：0.01手递增（四舍五入到两位小数）
            finalLotSize = Math.round(finalLotSize * 100.0) / 100.0

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

        // 交易时间说明
        Text(text = "交易时间说明:", modifier = Modifier.padding(bottom = 8.dp))
        Text(
            text = "- 日线开始: 你的时间早上6:00 (对方时间0:00)\n" +
                  "- 4小时K线开始: 6:00、10:00、14:00、18:00、22:00、次日2:00\n" +
                  "  (每4小时循环一次，完美衔接日线)",
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
