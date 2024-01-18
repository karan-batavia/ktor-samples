package io.ktor.samples.httpbin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.methods() {
    delete("/delete") {
        call.respond(call.request.asCommonResponse())
    }

    get("/get") {
        call.respond(call.request.asCommonResponse())
    }

    patch("/patch") {
        call.respond(call.request.asCommonResponse())
    }

    post("/post") {
        call.respond(call.request.asCommonResponse())
    }

    put("/put") {
        call.respond(call.request.asCommonResponse())
    }
}