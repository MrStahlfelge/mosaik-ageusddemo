package org.ergoplatform.mosaik.example.ageusd

import org.springframework.stereotype.Service

@Service
class AgeUsdService {

    // this is a dummy service
    fun getAgeUsdBank(): AgeUsdBank {
        Thread.sleep(1000)
        return AgeUsdBank(340, 4184100, 623497)
    }


    data class AgeUsdBank(
        val reserveRatio: Int,
        val sigUsdPrice: Long,
        val sigRsvPrice: Long,
    )
}