package org.ergoplatform.mosaik.example.ageusd

import org.ergoplatform.mosaik.*
import org.ergoplatform.mosaik.jackson.MosaikSerializer
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
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import java.math.BigDecimal
import java.math.RoundingMode
import javax.servlet.http.HttpServletRequest

@RestController
@CrossOrigin
class AgeUsdController(private val ageUsdService: AgeUsdService) {
    @GetMapping("/")
    fun mainPage(): ModelAndView {
        // we always serve nobrowser error page for the main url. If the request came from a
        // Mosaik executor, it will pick up the <link rel="mosaik" ...> entry
        return ModelAndView("nobrowser.html")
    }

    private fun HttpServletRequest.getServerUrl() =
        requestURL.toString().substringBefore("sigmausd")

    @GetMapping("/sigmausd")
    fun ageUsdDashboard(request: HttpServletRequest): MosaikApp {
        val serverRequestUrl = request.getServerUrl()

        return mosaikApp(
            "AgeUSD dashboard",
            APP_VERSION,
            "Buy and sell SigUSD and SigRSV",
            serverRequestUrl + "applogo.png",
            targetCanvasDimension = MosaikManifest.CanvasDimension.COMPACT_WIDTH,
            cacheLifetime = 120,
        ) {
            val ageUsdBank = ageUsdService.getAgeUsdBank()

            val exSigUsd = navigateToApp(serverRequestUrl + "sigmausd/exchange/SigUSD")
            val exSigRsv = navigateToApp(serverRequestUrl + "sigmausd/exchange/SigRSV")
            reloadApp { id = RELOAD_ACTION_ID }

            column {
                layout(HAlignment.JUSTIFY) {
                    card(Padding.HALF_DEFAULT) {
                        layout(HAlignment.JUSTIFY, VAlignment.CENTER) {
                            column(Padding.HALF_DEFAULT) {
                                layout(HAlignment.END) {
                                    icon(IconType.REFRESH) {
                                        onClickAction(RELOAD_ACTION_ID)
                                    }
                                }

                                label("Reserve ratio", LabelStyle.HEADLINE2)
                                label("${ageUsdBank.reserveRatio}%", LabelStyle.HEADLINE1)

                                box(Padding.HALF_DEFAULT)
                            }
                        }
                    }

                    addAgeUsdCard(
                        "SigUSD",
                        ageUsdBank.sigUsdPrice,
                        2,
                        exSigUsd,
                    )

                    addAgeUsdCard(
                        "SigRSV",
                        ageUsdBank.sigRsvPrice,
                        0,
                        exSigRsv,
                    )
                }
            }
        }
    }

    private fun @MosaikDsl ViewGroup.addAgeUsdCard(
        tokenLabel: String,
        nanoerg: Long,
        tokenDecimals: Int,
        exchangeApp: Action,
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
                        withCurrency = false,
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

                button("Buy or sell") {
                    onClickAction(exchangeApp)
                }
            }
        }
    }

    @GetMapping("/sigmausd/exchange/{token}")
    fun ageUsdExchangeApp(@PathVariable token: String, request: HttpServletRequest): MosaikApp {
        val serverRequestUrl = request.getServerUrl()

        return mosaikApp(
            "$token exchange",
            APP_VERSION,
            "Buy and sell $token",
            serverRequestUrl + "applogo.png",
            targetCanvasDimension = MosaikManifest.CanvasDimension.COMPACT_WIDTH,
            cacheLifetime = 120,
        ) {

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
                column(Padding.ONE_AND_A_HALF_DEFAULT) {
                    label(
                        "Enter the $token amount to buy or sell",
                        LabelStyle.BODY1BOLD,
                        HAlignment.CENTER
                    )
                    box(Padding.HALF_DEFAULT)
                    row(packed = true) {
                        label("1 $token = ${bigDecimalPrice.toPlainString()} ERG")

                        box(Padding.DEFAULT) {
                            onClickAction =
                                addAction(navigateToApp(serverRequestUrl + "sigmausd")).id
                            icon(IconType.INFO, Icon.Size.SMALL, ForegroundColor.SECONDARY)
                        }

                    }
                    box(Padding.HALF_DEFAULT)
                    row {
                        val showInfoAction = backendRequest("calc/") {
                            // this makes the request always perform, even when an invalid amount is entered
                            postValues = BackendRequestAction.PostValueType.VALID
                        }

                        // VAlignment.BOTTOM needed for web as dropdown and input field is not of same height
                        layout(weight = 1, vAlignment = VAlignment.BOTTOM) {
                            dropDownList(
                                INPUT_EXCHANGE_TYPE, mapOf(
                                    "buy" to "Buy",
                                    "sell" to "Sell",
                                ), "buy"
                            ) {
                                onValueChangedAction = showInfoAction.id
                            }
                        }

                        box(Padding.QUARTER_DEFAULT)

                        layout(weight = 2) {
                            decimalInputField(
                                INPUT_BUYSELLAMOUNT_ID,
                                scale,
                                "$token amount"
                            ) {
                                onValueChangedAction = showInfoAction.id
                            }
                        }
                    }
                    box { id = CALCULATION_CONTENT_ID }
                }
            }

        }
    }

    @PostMapping("/sigmausd/exchange/{token}/calc")
    fun showExchangeCalculationInfo(
        @PathVariable token: String,
        @RequestBody values: Map<String, *>,
        @RequestHeader headers: Map<String, String>,
        request: HttpServletRequest,
    ) = backendResponse(APP_VERSION, changeView(mosaikView {

        val tokenAmountEntered = (values[INPUT_BUYSELLAMOUNT_ID] as? Number?)?.toLong()
        val type = values[INPUT_EXCHANGE_TYPE] as String
        val isWebExecutor = MosaikSerializer.fromContextHeadersMap(headers).walletAppName.contains(
            "web",
            ignoreCase = true
        )

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

                    addRow(exchangeInfo.ergAmount, exchangeInfo.ergAmountDescription, isWebExecutor)
                    addRow(exchangeInfo.bankFeeAmount, exchangeInfo.bankFeeDescription, isWebExecutor)
                    box(Padding.QUARTER_DEFAULT)

                    // packed is needed when no fiat value is set to center the element
                    // normally, it should always work. but a layouting bug on Compose messes,
                    // so we give Compose executors a false here
                    row(packed = isWebExecutor) {
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
        description: String,
        isWeb: Boolean
    ) {
        // VAlignment.CENTER for Web but TOP for others
        val vAlignment = if (isWeb) VAlignment.CENTER else VAlignment.TOP
        row {
            layout(vAlignment, 1) {
                ergAmount(
                    ergAmount,
                    LabelStyle.BODY1BOLD,
                    HAlignment.END
                )
            }
            box(Padding.HALF_DEFAULT)
            layout(vAlignment, 1) {
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
    val INPUT_EXCHANGE_TYPE = "exchangeType"
}

const val RELOAD_ACTION_ID = "reload_action"