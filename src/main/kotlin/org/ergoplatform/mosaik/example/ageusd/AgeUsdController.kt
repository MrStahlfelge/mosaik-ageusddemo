package org.ergoplatform.mosaik.example.ageusd

import org.ergoplatform.mosaik.card
import org.ergoplatform.mosaik.column
import org.ergoplatform.mosaik.label
import org.ergoplatform.mosaik.model.MosaikApp
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.model.ui.layout.Padding
import org.ergoplatform.mosaik.mosaikApp
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest

@RestController
class AgeUsdController {
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
            column {
                card {

                }
            }
        }
    }
}