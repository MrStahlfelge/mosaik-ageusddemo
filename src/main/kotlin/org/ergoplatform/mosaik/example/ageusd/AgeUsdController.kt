package org.ergoplatform.mosaik.example.ageusd

import org.ergoplatform.mosaik.*
import org.ergoplatform.mosaik.model.MosaikApp
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.model.actions.Action
import org.ergoplatform.mosaik.model.ui.ViewGroup
import org.ergoplatform.mosaik.model.ui.layout.HAlignment
import org.ergoplatform.mosaik.model.ui.layout.Padding
import org.ergoplatform.mosaik.model.ui.layout.VAlignment
import org.ergoplatform.mosaik.model.ui.text.Button
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import java.math.BigDecimal
import java.math.RoundingMode
import javax.servlet.http.HttpServletRequest

@RestController
class AgeUsdController(private val ageUsdService: AgeUsdService) {
    @GetMapping("/")
    fun mainPage(): ModelAndView {
        // we always serve nobrowser error page for the main url. If the request came from a
        // Mosaik executor, it will pick up the <link rel="mosaik" ...> entry
        return ModelAndView("nobrowser.html")
    }

    @GetMapping("/sigmausd")
    fun sigmaUsdApp(request: HttpServletRequest): MosaikApp {
        val baseUrl = request.requestURL.toString()
        val serverRequestUrl = baseUrl.substring(0, baseUrl.indexOf("sigmausd"))

        return mosaikApp(
            "AgeUSD",
            APP_VERSION,
            "Buy and sell SigUSD and SigRSV",
            serverRequestUrl + "applogo.png",
            targetCanvasDimension = MosaikManifest.CanvasDimension.COMPACT_WIDTH,
            cacheLifetime = 120,
        ) {
            val ageUsdBank = ageUsdService.getAgeUsdBank()

            val buySigUsd = backendRequest("exchange/SigUSD/buy")
            val buySigRsv = backendRequest("exchange/SigRSV/buy")
            val sellSigUsd = backendRequest("exchange/SigUSD/sell")
            val sellSigRsv = backendRequest("exchange/SigRSV/sell")
            reloadApp { id = RELOAD_ACTION_ID }

            column {
                layout(HAlignment.JUSTIFY) {
                    card(Padding.HALF_DEFAULT) {
                        column(Padding.ONE_AND_A_HALF_DEFAULT) {
                            label("Reserve ratio", LabelStyle.HEADLINE2)
                            label("${ageUsdBank.reserveRatio}%", LabelStyle.HEADLINE1)
                        }
                    }

                    addAgeUsdCard(
                        "SigUSD",
                        ageUsdBank.sigUsdPrice,
                        2,
                        buySigUsd,
                        sellSigUsd,
                    )

                    addAgeUsdCard(
                        "SigRSV",
                        ageUsdBank.sigRsvPrice,
                        0,
                        buySigRsv,
                        sellSigRsv,
                    )
                }
            }
        }
    }

    private fun @MosaikDsl ViewGroup.addAgeUsdCard(
        tokenLabel: String,
        nanoerg: Long,
        tokenDecimals: Int,
        buyAction: Action,
        sellAction: Action,
    ) {
        card(Padding.HALF_DEFAULT) {
            column(Padding.DEFAULT) {
                box(Padding.QUARTER_DEFAULT)
                label(tokenLabel, LabelStyle.HEADLINE1)
                box(Padding.HALF_DEFAULT)
                val bigDecimalPrice = BigDecimal.valueOf(nanoerg).movePointLeft(9 - tokenDecimals)
                row {
                    // TODO ErgAmountLabel
                    label(
                        bigDecimalPrice.toPlainString(),
                        LabelStyle.HEADLINE1,
                        HAlignment.END
                    )
                    box(Padding.HALF_DEFAULT) {
                        label("ERG/$tokenLabel", LabelStyle.BODY2BOLD)
                    }
                }

                val price = BigDecimal.valueOf(1.0 / bigDecimalPrice.toDouble())
                    .setScale(tokenDecimals, RoundingMode.HALF_UP).toPlainString()
                label("1 ERG ≈ $price $tokenLabel", LabelStyle.BODY1BOLD)
                box(Padding.HALF_DEFAULT)

                row {
                    layout(VAlignment.CENTER, weight = 1) {
                        box {
                            layout(HAlignment.END, VAlignment.CENTER) {
                                button("Buy") {
                                    onClickAction(buyAction)
                                }
                            }
                        }
                    }
                    box(Padding.HALF_DEFAULT)
                    layout(VAlignment.CENTER, weight = 1) {
                        box {
                            layout(HAlignment.START, VAlignment.CENTER) {
                                button("Sell", Button.ButtonStyle.SECONDARY) {
                                    onClickAction(sellAction)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

const val RELOAD_ACTION_ID = "reload_action"
const val CALCULATION_CONTENT_ID = "calculationContent"