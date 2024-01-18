package io.ktor.samples.httpbin

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*


fun main() {
    embeddedServer(Netty, host = "localhost", port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            methods()
        }
    }.start(wait = true)
}
