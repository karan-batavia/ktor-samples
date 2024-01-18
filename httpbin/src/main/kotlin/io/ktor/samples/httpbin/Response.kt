package io.ktor.samples.httpbin

import io.ktor.server.plugins.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

@Serializable
data class CommonResponse(
    @Serializable(with = ArgsSerializer::class)
    val args: Map<String, List<String>>,
    val headers: Map<String, String>,
    val origin: String,
    val url: String
)

object ArgsSerializer: JsonTransformingSerializer<Map<String, List<String>>>(MapSerializer(String.serializer(), ListSerializer(String.serializer()))) {
    override fun transformSerialize(element: JsonElement): JsonElement {
        require(element is JsonObject)

        val result = mutableMapOf<String, JsonElement>()
        for ((key, el) in element) {
            require(el is JsonArray)

            if (el.isEmpty()) {
                result[key] = JsonPrimitive("")
            } else if (el.size == 1) {
                require(el[0] is JsonPrimitive)
                result[key] = el[0]
            } else {
                result[key] = el
            }
        }

        return JsonObject(result)
    }
}

fun ApplicationRequest.asCommonResponse(): CommonResponse {
    val port = origin.serverPort
    val url = "${origin.scheme}://${origin.serverHost}${if (port != 80) ":$port" else ""}$uri"

    val headers = mutableMapOf<String, String>()
    for ((name, values) in call.request.headers.entries()) {
        headers[name] = values.joinToString(separator = ",")
    }

    val args = mutableMapOf<String, List<String>>()
    for ((name, values) in call.request.queryParameters.entries()) {
        args[name] = values
    }

    return CommonResponse(args, headers.toSortedMap(), origin.remoteAddress, url)
}