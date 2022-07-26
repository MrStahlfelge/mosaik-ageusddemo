package org.ergoplatform.mosaik.example.ageusd

import org.ergoplatform.mosaik.*
import org.ergoplatform.mosaik.model.MosaikApp
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.model.actions.Action
import org.ergoplatform.mosaik.model.actions.BackendRequestAction
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.Icon
import org.ergoplatform.mosaik.model.ui.IconType
import org.ergoplatform.mosaik.model.ui.ViewGroup
import org.ergoplatform.mosaik.model.ui.layout.Column
import org.ergoplatform.mosaik.model.ui.layout.HAlignment
import org.ergoplatform.mosaik.model.ui.layout.Padding
import org.ergoplatform.mosaik.model.ui.layout.VAlignment
import org.ergoplatform.mosaik.model.ui.text.Button
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.springframework.web.bind.annotation.*
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

    private fun HttpServletRequest.getServerUrl() = requestURL.toString().substringBefore("sigmausd")

    @GetMapping("/sigmausd")
    fun sigmaUsdApp(request: HttpServletRequest): MosaikApp {
        val serverRequestUrl = request.getServerUrl()

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
                row {
                    ergAmount(
                        nanoerg,
                        LabelStyle.HEADLINE1,
                        HAlignment.END,
                        maxDecimals = 9 - tokenDecimals
                    )
                    box(Padding.HALF_DEFAULT) {
                        label("ERG/$tokenLabel", LabelStyle.BODY2BOLD)
                    }
                }

                val bigDecimalPrice = BigDecimal.valueOf(nanoerg).movePointLeft(9 - tokenDecimals)
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

    @PostMapping("/sigmausd/exchange/{token}/{type}")
    fun showExchangeDialog(
        @PathVariable token: String,
        @PathVariable type: String,
    ) = backendResponse(APP_VERSION, changeView(mosaikView {

        val ageUsdBank = ageUsdService.getAgeUsdBank()

        val scale = when (token.lowercase()) {
            "sigusd" -> 2
            else -> 0
        }
        val nanoerg = when (token.lowercase()) {
            "sigusd" -> ageUsdBank.sigUsdPrice
            else -> ageUsdBank.sigRsvPrice
        }

        val bigDecimalPrice = BigDecimal.valueOf(nanoerg).movePointLeft(9 - scale)

        card {
            layout(HAlignment.START, VAlignment.TOP) {
                box(Padding.DEFAULT) {
                    onClickAction = RELOAD_ACTION_ID
                    icon(IconType.BACK, Icon.Size.SMALL, ForegroundColor.SECONDARY)
                }
            }

            column(Padding.ONE_AND_A_HALF_DEFAULT) {
                label("Enter the $token amount to $type", LabelStyle.BODY1BOLD, HAlignment.CENTER)
                box(Padding.HALF_DEFAULT)
                label("1 $token = ${bigDecimalPrice.toPlainString()} ERG")
                box(Padding.HALF_DEFAULT)
                decimalInputField(INPUT_BUYSELLAMOUNT_ID, scale, "$token amount to $type") {
                    onValueChangedAction = backendRequest("exchange/calc/$token/$type") {
                        postValues = BackendRequestAction.PostValueType.VALID
                    }.id
                }

                box { id = CALCULATION_CONTENT_ID }
            }
        }

    }))

    @PostMapping("/sigmausd/exchange/calc/{token}/{type}")
    fun showExchangeCalculationInfo(
        @PathVariable token: String,
        @PathVariable type: String,
        @RequestBody values: Map<String, *>,
        request: HttpServletRequest,
    ) = backendResponse(APP_VERSION, changeView(mosaikView {

        val tokenAmountEntered = (values[INPUT_BUYSELLAMOUNT_ID] as? Number?)?.toLong()

        column {
            id = CALCULATION_CONTENT_ID

            tokenAmountEntered?.let {
                box(Padding.DEFAULT)

                val tokenAmountToBuy =
                    if (type.lowercase() == "buy") tokenAmountEntered else -tokenAmountEntered

                try {
                    val exchangeInfo = when (token.lowercase()) {
                        "sigusd" -> ageUsdService.calcSigmaUsdExchange(tokenAmountToBuy)
                        else -> ageUsdService.calcSigmaRsvExchange(tokenAmountToBuy)
                    }

                    addRow(exchangeInfo.ergAmount, exchangeInfo.ergAmountDescription)
                    addRow(exchangeInfo.bankFeeAmount, exchangeInfo.bankFeeDescription)
                    box(Padding.QUARTER_DEFAULT)

                    row {
                        ergAmount(
                            exchangeInfo.totalAmount,
                            LabelStyle.BODY1BOLD,
                            HAlignment.END
                        )
                        box(Padding.HALF_DEFAULT)
                        fiatAmount(exchangeInfo.totalAmount, textColor = ForegroundColor.SECONDARY)
                    }

                    box(Padding.HALF_DEFAULT)

                    layout(HAlignment.JUSTIFY) {
                        button(type.uppercase()) {
                            onClickAction(
                                backendRequest(
                                    request.getServerUrl() + "sigmausd/exchange/$tokenAmountToBuy/$token/${exchangeInfo.exchangeRate}",
                                    id = "ageUsdErgoPay" // we use a constant here so that former actions are replaced in executor
                                )
                            )
                        }
                    }

                    box(Padding.HALF_DEFAULT)

                    label("Transaction fee:")

                    row {
                        layout(weight = 1) {
                            box()
                        }
                        layout(weight = 3) {
                            dropDownList(
                                INPUT_TX_FEE_ID, mapOf(
                                    "1" to "low fee (0.001 ERG)",
                                    "5" to "medium fee (0.005 ERG)",
                                    "30" to "high fee (0.030 ERG)"
                                ), "5"
                            )
                        }
                        layout(weight = 1) {
                            box()
                        }
                    }

                } catch (t: Throwable) {
                    label(
                        t.message!!,
                        LabelStyle.BODY1BOLD,
                        HAlignment.CENTER,
                        textColor = ForegroundColor.PRIMARY
                    )
                }
            }

        }

    }))

    private fun @MosaikDsl Column.addRow(
        ergAmount: Long,
        description: String
    ) {
        row {
            layout(VAlignment.TOP, 1) {
                ergAmount(
                    ergAmount,
                    LabelStyle.BODY1BOLD,
                    HAlignment.END
                )
            }
            box(Padding.HALF_DEFAULT)
            layout(VAlignment.TOP, 1) {
                label(description, LabelStyle.BODY2)
            }
        }
    }

    @PostMapping("/sigmausd/exchange/{tokenAmountToBuy}/{token}/{exchangeRate}")
    fun getErgoPayUrl(
        @PathVariable tokenAmountToBuy: Long,
        @PathVariable exchangeRate: Long,
        @PathVariable token: String,
        @RequestBody values: Map<String, *>,
        request: HttpServletRequest
    ) = backendResponse(
        APP_VERSION, invokeErgoPay(
            getAgeUsdExchangeErgoPayUrl(
                request.getServerUrl(),
                tokenAmountToBuy,
                token.lowercase(),
                (values[INPUT_TX_FEE_ID] as? String)?.toLong() ?: 5,
                exchangeRate
            ),
        )
    )


    private fun getAgeUsdExchangeErgoPayUrl(
        serverUrl: String,
        circulatingDelta: Long,
        tokenName: String,
        minersFee: Long,
        checkRate: Long
    ): String {
        return ("ergopay://api.tokenjay.app/" + tokenName
                + "/exchange/?amount=" + circulatingDelta
                + "&address=#P2PK_ADDRESS#"
                + "&checkRate=" + checkRate
                + "&executionFee=" + (minersFee * 1000L * 1000L))
    }


    val CALCULATION_CONTENT_ID = "calculationContent"
    val INPUT_BUYSELLAMOUNT_ID = "amountToSwap"
    val INPUT_TX_FEE_ID = "minerFee"
}

const val RELOAD_ACTION_ID = "reload_action"