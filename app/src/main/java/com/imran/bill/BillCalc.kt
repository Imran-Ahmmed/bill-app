package com.imran.bill

import java.math.BigDecimal
import java.math.MathContext

data class BillInput(
    val presentDate: String,
    val pastDate: String,
    val recharge: Int,
    val cutting: Int,
    val presentTk: Int,
    val pastTk: Int,
    val aiyanPresent: Long,
    val aiyanPast: Long,
    val mahimPresent: Long,
    val mahimPast: Long,
    val mahimPaid: Int? = null   // ঐচ্ছিক: পাশের ঘর আগে যত টাকা দিয়েছে
)

data class BillResult(
    val input: BillInput,
    val totalTk: Int,
    val taholeTk: Int,
    val katingBade: Float,
    val aiyanUnit: Long,
    val mahimUnit: Long,
    val totalUnit: Float,
    val perUnit: Float,
    val aiyanBill: Float,
    val mahimBill: Float,
    val cuttingHalf: Int
)

/** তোমার C++ প্রোগ্রামের হুবহু হিসাব */
fun calculate(i: BillInput): BillResult {
    val totalTk = i.recharge + i.pastTk
    val taholeTk = totalTk - i.presentTk
    val katingBade = (taholeTk - i.cutting).toFloat()

    val aiyanUnit = i.aiyanPresent - i.aiyanPast
    val mahimUnit = i.mahimPresent - i.mahimPast
    val totalUnit = (aiyanUnit + mahimUnit).toFloat()

    val perUnit = katingBade / totalUnit
    val aiyanBill = aiyanUnit * perUnit
    val mahimBill = mahimUnit * perUnit
    val cuttingHalf = i.cutting / 2   // C++ এর মতোই integer division

    return BillResult(
        i, totalTk, taholeTk, katingBade,
        aiyanUnit, mahimUnit, totalUnit,
        perUnit, aiyanBill, mahimBill, cuttingHalf
    )
}

/** C++ cout এর মতো ৬ অঙ্ক পর্যন্ত দেখায় (যেমন 6.9898, 803.826) */
fun fmt(v: Float): String =
    BigDecimal(v.toDouble()).round(MathContext(6)).stripTrailingZeros().toPlainString()
