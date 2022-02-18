package verticle

import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.rxjava3.core.AbstractVerticle
import io.vertx.rxjava3.ext.web.Router
import io.vertx.rxjava3.ext.web.RoutingContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class HttpServerVerticle : AbstractVerticle() {
    override fun start(promise: Promise<Void>) {
        val router = Router.router(vertx).apply {
            get("/web-scraper/crawl").handler(this@HttpServerVerticle::crawl)
        }

        vertx
            .createHttpServer()
            .requestHandler(router)
            .rxListen(8282)
            .subscribe(
                { promise.complete() },
                { failure -> promise.fail(failure.cause) })
    }

    private fun crawl(context: RoutingContext) {
        val document = Jsoup.connect("https://www.cnbc.com/startups/").get()
        val pageRowElements = document.select("div.PageBuilder-pageRow")

        val responses = JsonArray()

        for (pageRowElement in pageRowElements) {
            val moduleHeaderTitle = pageRowElement.selectFirst("h2.ModuleHeader-title")

            if (moduleHeaderTitle?.text().equals("More In Start-ups")) {
                val elements = pageRowElement.select("div.Card-standardBreakerCard")

                for (element in elements) {
                    val title = element.selectFirst("a.Card-title")
                    val datePost = element.selectFirst("span.Card-time")

                    responses.add(JsonObject().apply {
                        put("title", title?.text())
                        put("link", title?.attr("href"))
                        put("date_post", datePost?.text())
                    })
                }
            }
        }

        context.response().statusCode = 200

        context.response().putHeader("Content-Type", "application/json")
        context.response().end(responses.encode())
    }
}