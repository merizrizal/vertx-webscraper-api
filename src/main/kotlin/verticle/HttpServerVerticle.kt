package verticle

import io.vertx.core.Promise
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.rxjava3.core.AbstractVerticle
import io.vertx.rxjava3.ext.web.Router
import io.vertx.rxjava3.ext.web.RoutingContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat

class HttpServerVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(HttpServerVerticle::class.java)

    override fun start(promise: Promise<Void>) {
        val router = Router.router(vertx).apply {
            get("/web-scraper/scrape").handler(this@HttpServerVerticle::scrape)
        }

        vertx
            .createHttpServer()
            .requestHandler(router)
            .rxListen(8282)
            .subscribe(
                { promise.complete() },
                { failure -> promise.fail(failure.cause) })
    }

    private fun scrape(context: RoutingContext) {
        val responses = JsonArray()

        responses.addAll(scrapeCnbc())
        responses.addAll(scrapeGeekWire())
        responses.addAll(scrapeTechStartups())

        context.response().statusCode = 200

        context.response().putHeader("Content-Type", "application/json")
        context.response().end(responses.encode())
    }

    private fun scrapeCnbc(): JsonArray {
        val responses = JsonArray()

        val document = Jsoup.connect("https://www.cnbc.com/startups/")
            .userAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/118.0")
            .get()
        val pageRowElements = document.select("div.PageBuilder-pageRow")

        for (pageRowElement in pageRowElements) {
            try {
                val moduleHeaderTitle = pageRowElement.selectFirst("h2.ModuleHeader-title")

                if (moduleHeaderTitle?.text().equals("More In Start-ups")) {
                    val elements = pageRowElement.select("div.Card-standardBreakerCard")

                    for (element in elements) {
                        val title = element.selectFirst("a.Card-title")
                        val datePost = element.selectFirst("span.Card-time")

                        val dateFormat = SimpleDateFormat("EEE, MMM d yyyy")
                        val dateString = datePost?.text()?.replace("st|nd|rd|th".toRegex(), "")
                        val date = dateFormat.parse(dateString)

                        responses.add(JsonObject().apply {
                            put("title", title?.text())
                            put("link", title?.attr("href"))
                            put("timestamp", date.time)
                        })
                    }
                }
            } catch (e: Exception) {
                logger.error(e.message)
            }
        }

        return responses
    }

    private fun scrapeGeekWire(): JsonArray {
        val responses = JsonArray()

        val document = Jsoup.connect("https://www.geekwire.com/startups/")
            .userAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/118.0")
            .get()
        val articleElements = document.select("article.type-post")

        for (articleElement in articleElements) {
            try {
                val title = articleElement.selectFirst("h2.entry-title > a")
                val datePost = articleElement.selectFirst("time.published")

                val dateFormat = SimpleDateFormat("MMM d, yyyy")
                val dateString = datePost?.text()
                val date = dateFormat.parse(dateString)

                responses.add(JsonObject().apply {
                    put("title", title?.text())
                    put("link", title?.attr("href"))
                    put("timestamp", date.time)
                })
            } catch (e: Exception) {
                logger.error(e.message)
            }
        }

        return responses
    }

    private fun scrapeTechStartups(): JsonArray {
        val responses = JsonArray()

        val document = Jsoup.connect("https://techstartups.com/")
            .userAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/118.0")
            .get()
        val postElements = document.select("div.post.type-post")

        for (postElement in postElements) {
            try {
                val title = postElement.selectFirst("h5 > a")
                val datePost = postElement.selectFirst("span.post_info_date")

                val dateFormat = SimpleDateFormat("MMM d, yyyy")
                val dateString = datePost?.text()?.replace("Posted On ", "")
                val date = dateFormat.parse(dateString)

                responses.add(JsonObject().apply {
                    put("title", title?.text())
                    put("link", title?.attr("href"))
                    put("timestamp", date.time)
                })
            } catch (e: Exception) {
                logger.error(e.message)
            }
        }

        return responses
    }
}