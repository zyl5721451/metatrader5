package com.practicestock.metatrader5.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.practicestock.metatrader5.data.AppPreferences
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

// 定义计算逻辑类型
enum class CalcCategory {
    FOREX_DIRECT,   // 外汇直盘 (USD在后: EURUSD) -> 盈亏就是USD
    FOREX_INVERSE,  // 外汇反向 (USD在前: USDJPY) -> 需除以平仓价
    FOREX_CROSS,    // 外汇交叉 (AUDCAD) -> 需除以 USD/CAD 汇率
    CFD_USD,        // 美元计价CFD (黄金, 美股指, 大宗) -> 盈亏就是USD
    CFD_NON_USD     // 非美计价CFD (GER40-欧, UK100-英) -> 需乘以 欧元/美元 或 英镑/美元 汇率
}

data class FinancialInstrument(
    val symbol: String,
    val defaultContractSize: Double, // 改为Double，因为加密货币可能是0.1
    val category: CalcCategory,
    val currency: String = "USD" // 计价货币名称，用于提示
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(appPreferences: AppPreferences, onNavigateToSettings: () -> Unit) {

    // -------------------------------------------------------------
    // ⚠️ 数据源定义：请务必核对 defaultContractSize
    // -------------------------------------------------------------
    val instruments = listOf(
        // --- 1. 外汇 (Forex) ---
        FinancialInstrument("EUR/USD", 100000.0, CalcCategory.FOREX_DIRECT),
        FinancialInstrument("GBP/USD", 100000.0, CalcCategory.FOREX_DIRECT),
        FinancialInstrument("AUD/USD", 100000.0, CalcCategory.FOREX_DIRECT),
        FinancialInstrument("NZD/USD", 100000.0, CalcCategory.FOREX_DIRECT),
        FinancialInstrument("USD/CAD", 100000.0, CalcCategory.FOREX_INVERSE),
        FinancialInstrument("USD/CHF", 100000.0, CalcCategory.FOREX_INVERSE),
        FinancialInstrument("USD/CNH", 100000.0, CalcCategory.FOREX_INVERSE),
        FinancialInstrument("USD/JPY", 100000.0, CalcCategory.FOREX_INVERSE),
        FinancialInstrument("USD/SGD", 100000.0, CalcCategory.FOREX_INVERSE),

        // --- 2. 金属 (Metals) ---
        // XAU: 100盎司, XAG: 5000盎司
        FinancialInstrument("XAU/USD", 100.0, CalcCategory.CFD_USD),
        FinancialInstrument("XAGUSD", 5000.0, CalcCategory.CFD_USD),
        // XCU(铜): 常见25000磅或10000, 需核对
        FinancialInstrument("XCU/USD", 25000.0, CalcCategory.CFD_USD),
        // XPT(铂金): 通常50盎司
        FinancialInstrument("XPT/USD", 50.0, CalcCategory.CFD_USD),
        // XPD(钯金): 通常100盎司
        FinancialInstrument("XPD/USD", 100.0, CalcCategory.CFD_USD),

        // --- 3. 指数 (Indices) ---
        // ⚠️ 注意：合约大小各平台差异极大 (1, 10, 100都有可能)
        FinancialInstrument("US500.cash", 1.0, CalcCategory.CFD_USD),  // S&P500
        FinancialInstrument("US30.cash", 1.0, CalcCategory.CFD_USD),   // Dow Jones
        FinancialInstrument("US100.cash", 1.0, CalcCategory.CFD_USD),  // Nasdaq
        // 非美指数
        FinancialInstrument("GER40.cash", 1.0, CalcCategory.CFD_NON_USD, "EUR"), // DAX (欧元)
        FinancialInstrument("UK100.cash", 1.0, CalcCategory.CFD_NON_USD, "GBP"), // FTSE (英镑)

        // --- 4. 能源 (Energy) ---
        FinancialInstrument("Heatoil.c", 42000.0, CalcCategory.CFD_USD), // 热燃油 (加仑)
        FinancialInstrument("NatGas.cash", 10000.0, CalcCategory.CFD_USD), // 天然气 (mmBtu)

        // --- 5. 农产品 (Agriculture) ---
        // ⚠️ 注意：价格通常为美分，计算时需注意小数点。此处假设MT5报价与美元换算一致
        // 若MT5显示 560.00，实际合约可能是 5000 蒲式耳。
        FinancialInstrument("Coffee.c", 37500.0, CalcCategory.CFD_USD), // 咖啡 (磅)
        FinancialInstrument("Wheat.c", 5000.0, CalcCategory.CFD_USD),   // 小麦 (蒲式耳)
        FinancialInstrument("Corn.c", 5000.0, CalcCategory.CFD_USD),    // 玉米 (蒲式耳)
        FinancialInstrument("Soybean.c", 5000.0, CalcCategory.CFD_USD), // 大豆 (蒲式耳)
        FinancialInstrument("Sugar.c", 112000.0, CalcCategory.CFD_USD), // 糖 (磅)
        FinancialInstrument("Cotton.c", 50000.0, CalcCategory.CFD_USD), // 棉花 (磅)
        FinancialInstrument("Cocoa.c", 10.0, CalcCategory.CFD_USD),     // 可可 (公吨) - 常见是10

        // --- 6. 加密货币 (Crypto) ---
        FinancialInstrument("BTC/USD", 1.0, CalcCategory.CFD_USD),
        FinancialInstrument("ETH/USD", 1.0, CalcCategory.CFD_USD),
        FinancialInstrument("SOL/USD", 1.0, CalcCategory.CFD_USD) // Solana通常也是1或10
    )

    // 状态管理
    var selectedInstrument by remember { mutableStateOf(instruments[3]) } // 默认黄金
    var showDropdown by remember { mutableStateOf(false) }

    // 输入字段 (使用String防止输入过程中的跳变)
    var entryPriceStr by remember { mutableStateOf("") }
    var stopLossPriceStr by remember { mutableStateOf("") }
    // ⚠️ 新增：合约大小允许修改
    var contractSizeStr by remember { mutableStateOf(selectedInstrument.defaultContractSize.toString()) }
    // ⚠️ 新增：汇率输入 (用于 GER40, UK100, AUDCAD)
    var exchangeRateStr by remember { mutableStateOf("") }

    var resultText by remember { mutableStateOf("") }
    var lotSizeResult by remember { mutableStateOf("0.00") }
    var marginInfo by remember { mutableStateOf("") }

    // 当切换品种时，重置合约大小为默认值
    fun onInstrumentSelected(instrument: FinancialInstrument) {
        selectedInstrument = instrument
        contractSizeStr = if (instrument.defaultContractSize % 1.0 == 0.0) {
            instrument.defaultContractSize.toInt().toString()
        } else {
            instrument.defaultContractSize.toString()
        }
        showDropdown = false
        // 清空汇率输入，除非是同一个货币体系
        exchangeRateStr = ""
    }

    fun calculate() {
        try {
            val entry = entryPriceStr.toDoubleOrNull()
            val stopLoss = stopLossPriceStr.toDoubleOrNull()
            val contractSize = contractSizeStr.toDoubleOrNull()
            val exchangeRate = exchangeRateStr.toDoubleOrNull() // 某些品种需要

            if (entry == null || stopLoss == null || contractSize == null) {
                resultText = "请输入完整的价格和合约大小"
                return
            }

            val capital = appPreferences.initialCapital
            val leverage = appPreferences.leverage.toDouble()
            val riskPercent = appPreferences.stopLossPercentage / 100.0

            // 1. 风险金额 (USD)
            val maxRiskAmount = capital * riskPercent

            // 2. 价格差 (绝对值)
            val priceDiff = abs(entry - stopLoss)
            if (priceDiff == 0.0) {
                resultText = "开仓价不能等于止损价"
                return
            }

            // 3. 计算 "每手亏损金额 (折算为USD)"
            // ---------------------------------------------------------
            // 核心计算逻辑
            // ---------------------------------------------------------
            val lossPerLotUSD = when (selectedInstrument.category) {
                CalcCategory.FOREX_DIRECT, CalcCategory.CFD_USD -> {
                    // 计价也是USD，直接算 (EURUSD, XAUUSD, US500, BTCUSD)
                    contractSize * priceDiff
                }
                CalcCategory.FOREX_INVERSE -> {
                    // USD在前 (USDJPY)，除以止损价(平仓价)
                    (contractSize * priceDiff) / stopLoss
                }
                CalcCategory.FOREX_CROSS -> {
                    // 交叉盘 (AUDCAD)，需要除以 USD/CAD 汇率
                    // 这里的 exchangeRate 应输入 USDCAD 的价格
                    if (exchangeRate == null || exchangeRate == 0.0) {
                        resultText = "请输入 ${selectedInstrument.currency} 对 USD 的汇率 (例如 USDCAD)"
                        return
                    }
                    (contractSize * priceDiff) / exchangeRate
                }
                CalcCategory.CFD_NON_USD -> {
                    // 非美计价CFD (GER40-EUR, UK100-GBP)
                    // 需乘以 EURUSD 或 GBPUSD 汇率
                    if (exchangeRate == null || exchangeRate == 0.0) {
                        resultText = "请输入 ${selectedInstrument.currency}USD 的汇率"
                        return
                    }
                    (contractSize * priceDiff) * exchangeRate
                }
            }

            // 4. 风控手数
            val riskLots = maxRiskAmount / lossPerLotUSD

            // 5. 保证金手数 (资金限制)
            // 简化计算：无论是何种货币，先估算1手的合约名义价值(Notional Value)折算成USD
            val contractValueUSD = when (selectedInstrument.category) {
                CalcCategory.FOREX_DIRECT -> contractSize * entry // EURUSD -> EUR * Price
                CalcCategory.FOREX_INVERSE -> contractSize // USDJPY -> 100k USD
                CalcCategory.CFD_USD -> contractSize * entry // Gold -> 100 * Price
                CalcCategory.FOREX_CROSS -> {
                    // AUDCAD -> 100k AUD -> 100k * AUDUSD Price
                    // 这里为了简化，如果没有AUDUSD汇率，近似使用 EntryPrice (假设交叉盘汇率接近1，或者需要更复杂的输入)
                    // 为保证严谨，交叉盘的保证金计算比较复杂，这里用保守估算：
                    contractSize * entry // 假设 Base Currency Value
                }
                CalcCategory.CFD_NON_USD -> {
                    // GER40 (EUR) -> Value * EURUSD Rate
                    (contractSize * entry) * (exchangeRate ?: 1.0)
                }
            }

            val marginPerLot = contractValueUSD / leverage
            val maxMarginLots = capital / marginPerLot

            marginInfo = "1手保证金: $${String.format("%.2f", marginPerLot)}"

            // 6. 最终结果
            val finalLots = min(riskLots, maxMarginLots)

            // 结果格式化 (部分差价合约可能最小0.1，这里统一保留2位小数)
            lotSizeResult = String.format("%.2f", floor(finalLots * 100) / 100.0)

            resultText = if (maxMarginLots < riskLots) {
                "受杠杆限制 (最大可开 ${String.format("%.2f", maxMarginLots)} 手)"
            } else {
                "基于风控计算"
            }

        } catch (e: Exception) {
            resultText = "计算错误: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("交易仓位计算器", style = MaterialTheme.typography.headlineSmall)

        OutlinedButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()
        ) {
            Text("设置: $${appPreferences.initialCapital} | 止损${appPreferences.stopLossPercentage}% | 杠杆${appPreferences.leverage}")
        }

        // 1. 品种选择
        Text("选择品种", style = MaterialTheme.typography.labelMedium)
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Button(onClick = { showDropdown = !showDropdown }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedInstrument.symbol)
            }
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                modifier = Modifier.height(300.dp) // 限制高度，因为列表很长
            ) {
                instruments.forEach { instrument ->
                    DropdownMenuItem(
                        text = { Text(instrument.symbol) },
                        onClick = { onInstrumentSelected(instrument) }
                    )
                }
            }
        }

        // 2. 合约大小 (允许修改)
        Text("合约大小 (Contract Size) - 请核对MT5!", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = contractSizeStr,
            onValueChange = { contractSizeStr = it; calculate() },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            label = { Text("例如: 黄金100, EURUSD 100000") }
        )

        // 3. 价格输入
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = entryPriceStr,
                onValueChange = { entryPriceStr = it; calculate() },
                label = { Text("开仓价") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = stopLossPriceStr,
                onValueChange = { stopLossPriceStr = it; calculate() },
                label = { Text("止损价") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        // 4. 动态汇率输入 (仅在需要时显示)
        if (selectedInstrument.category == CalcCategory.CFD_NON_USD) {
            Text("汇率换算 (${selectedInstrument.currency} -> USD)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top=8.dp))
            OutlinedTextField(
                value = exchangeRateStr,
                onValueChange = { exchangeRateStr = it; calculate() },
                label = { Text("请输入 ${selectedInstrument.currency}USD 当前价格") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        } else if (selectedInstrument.category == CalcCategory.FOREX_CROSS) {
            Text("汇率换算 (Quote -> USD)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top=8.dp))
            OutlinedTextField(
                value = exchangeRateStr,
                onValueChange = { exchangeRateStr = it; calculate() },
                label = { Text("请输入 USD${selectedInstrument.currency} 价格") }, // 如 USDCAD
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 结果显示
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("建议手数", style = MaterialTheme.typography.titleMedium)
                Text(lotSizeResult, style = MaterialTheme.typography.displayMedium)
                if (marginInfo.isNotEmpty()) Text(marginInfo, style = MaterialTheme.typography.bodySmall)
                if (resultText.isNotEmpty()) Text(resultText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("注意：农产品价格若为美分，请确保开仓价与止损价单位一致。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}