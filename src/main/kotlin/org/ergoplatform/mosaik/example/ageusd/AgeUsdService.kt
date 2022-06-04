package org.ergoplatform.mosaik.example.ageusd

import org.springframework.stereotype.Service
import java.lang.Math.abs

@Service
class AgeUsdService {

    // this is a dummy service


    fun getAgeUsdBank(): AgeUsdBank {
        Thread.sleep(1000)
        return AgeUsdBank(340, 4184100, 623497)
    }

    /**
     * calculates Sigma USD exchange
     * @param sigmaUsdAmount change to circulating amount (positive: buy, negative: sell)
     */
    fun calcSigmaUsdExchange(sigmaUsdAmount: Long): AgeUsdExchangeInfo {
        val ageUsdBank = getAgeUsdBank()

        if (sigmaUsdAmount < 0) {
            val ergAmount = ageUsdBank.sigUsdPrice * sigmaUsdAmount
            val feeAmount = kotlin.math.abs((ergAmount * 2) / 100)

            return AgeUsdExchangeInfo(
                ageUsdBank.sigUsdPrice,
                ergAmount,
                "${formatSigmaUsdAmount(sigmaUsdAmount)} x ${formatErgAmount(ageUsdBank.sigUsdPrice)} ERG",
                feeAmount,
                "2% AgeUSD bank fee",
                ergAmount + feeAmount
            )
        } else {
            throw IllegalArgumentException(
                "Cannot exchange amount of ${
                    formatSigmaUsdAmount(
                        sigmaUsdAmount
                    )
                } SigmaUSD due to reserve conditions."
            )
        }
    }

    fun calcSigmaRsvExchange(sigmaRsvAmount: Long): AgeUsdExchangeInfo {
        val ageUsdBank = getAgeUsdBank()

        if (sigmaRsvAmount > 0) {
            val ergAmount = ageUsdBank.sigRsvPrice * sigmaRsvAmount
            val feeAmount = kotlin.math.abs((ergAmount * 2) / 100)

            return AgeUsdExchangeInfo(
                ageUsdBank.sigRsvPrice,
                ergAmount,
                "$sigmaRsvAmount x ${formatErgAmount(ageUsdBank.sigUsdPrice)} ERG",
                feeAmount,
                "2% AgeUSD bank fee",
                ergAmount + feeAmount
            )
        } else {
            throw IllegalArgumentException(
                "Cannot exchange amount of $sigmaRsvAmount SigRSV due to reserve conditions."
            )
        }
    }

    fun formatSigmaUsdAmount(rawAmount: Long): String =
        rawAmount.toBigDecimal().movePointLeft(2).toPlainString()

    fun formatErgAmount(rawAmount: Long): String =
        rawAmount.toBigDecimal().movePointLeft(9).toPlainString().trimEnd('0').trimEnd('.')

    data class AgeUsdBank(
        val reserveRatio: Int,
        val sigUsdPrice: Long,
        val sigRsvPrice: Long,
    )

    data class AgeUsdExchangeInfo(
        val exchangeRate: Long,
        val ergAmount: Long,
        val ergAmountDescription: String,
        val bankFeeAmount: Long,
        val bankFeeDescription: String,
        val totalAmount: Long
    )
}