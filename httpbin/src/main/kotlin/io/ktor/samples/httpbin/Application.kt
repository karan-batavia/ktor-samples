package io.ktor.samples.httpbin

import io.bkbn.kompendium.core.attribute.KompendiumAttributes
import io.bkbn.kompendium.core.plugin.NotarizedApplication
import io.bkbn.kompendium.oas.OpenApiSpec
import io.bkbn.kompendium.oas.info.Info
import io.bkbn.kompendium.oas.serialization.KompendiumSerializersModule
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalSerializationApi::class)
val CustomJsonEncoder = Json {
    serializersModule = KompendiumSerializersModule.module
    encodeDefaults = true
    explicitNulls = false
}

fun main() {
    embeddedServer(Netty, host = "localhost", port = 8080) {
        install(NotarizedApplication()) {
            spec = OpenApiSpec(info = Info("HttpBin Application", "1.0.0"), openapi = "3.0.3")

            openApiJson = {
                route("/openapi.json") {
                    get {
                        call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        val json = CustomJsonEncoder.encodeToString(this@route.application.attributes[KompendiumAttributes.openApiSpec])
                        call.respondText { json }
                    }
                }
            }
        }
        install(ContentNegotiation) {
            json()
        }
        routing {
            swaggerUI()
            methods()
        }
    }.start(wait = true)
}