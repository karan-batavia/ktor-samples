package io.ktor.samples.httpbin

import io.bkbn.kompendium.core.metadata.*
import io.bkbn.kompendium.core.plugin.NotarizedRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.methods() {
    route("/delete") {
        install(NotarizedRoute()) {
            delete = DeleteInfo.builder {
                summary("The request's DELETE parameters.")
                description("")
                response {
                    responseCode(HttpStatusCode.OK)
                    responseType<CommonResponse>()
                    description("The request's DELETE parameters.")
                }
            }
        }
        delete {
            call.respond(call.request.asCommonResponse())
        }
    }

    route("/get") {
        install(NotarizedRoute()) {
            get = GetInfo.builder {
                summary("The request's query parameters.")
                description("")
                response {
                    responseCode(HttpStatusCode.OK)
                    responseType<CommonResponse>()
                    description("The request’s query parameters.")
                }
            }
        }
        get {
            call.respond(call.request.asCommonResponse())
        }
    }

    route("/patch") {
        install(NotarizedRoute()) {
            patch = PatchInfo.builder {
                summary("The request's PATCH parameters.")
                description("")
                response {
                    responseCode(HttpStatusCode.OK)
                    responseType<CommonResponse>()
                    description("The request's PATCH parameters.")
                }
            }
        }
        patch {
            call.respond(call.request.asCommonResponse())
        }
    }

    route("/post") {
        install(NotarizedRoute()) {
            post = PostInfo.builder {
                summary("The request's POST parameters.")
                description("")
                response {
                    responseCode(HttpStatusCode.OK)
                    responseType<CommonResponse>()
                    description("The request's POST parameters.")
                }
            }
        }

        post {
            call.respond(call.request.asCommonResponse())
        }
    }

    route("/put") {
        install(NotarizedRoute()) {
            put = PutInfo.builder {
                summary("The request's POST parameters.")
                description("")
                response {
                    responseCode(HttpStatusCode.OK)
                    responseType<CommonResponse>()
                    description("The request's POST parameters.")
                }
            }
        }
        put {
            call.respond(call.request.asCommonResponse())
        }
    }
}