package io.ktor.samples.httpbin

import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.nio.ByteBuffer
import java.nio.charset.MalformedInputException
import kotlin.coroutines.coroutineContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.text.Charsets
import kotlin.text.String

@Serializable
data class CommonResponse(
    @Serializable(with = SmartArraySerializer::class)
    val args: Map<String, List<String>>,
    val headers: Map<String, String>,
    val origin: String,
    val url: String
)

@Serializable
data class BodyResponse(
    @Serializable(with = SmartArraySerializer::class)
    val args: Map<String, List<String>>,
    val data: String,
    val files: Map<String, String>,
    @Serializable(with = SmartArraySerializer::class)
    val form: Map<String, List<String>>,
    val headers: Map<String, String>,
    @Serializable
    @Contextual
    val json: Any, // Kompendium throws an error when the type is JsonElement
    val origin: String,
    val url: String,
)

@ExperimentalSerializationApi
object DynamicLookupSerializer: KSerializer<Any> {
    override val descriptor: SerialDescriptor = ContextualSerializer(Any::class, null, emptyArray()).descriptor

    @OptIn(InternalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Any) {
        val actualSerializer = encoder.serializersModule.getContextual(value::class) ?: value::class.serializer()
        @Suppress("UNCHECKED_CAST")
        encoder.encodeSerializableValue(actualSerializer as KSerializer<Any>, value)
    }

    override fun deserialize(decoder: Decoder): Any {
        error("Unsupported")
    }
}

object SmartArraySerializer: JsonTransformingSerializer<Map<String, List<String>>>(MapSerializer(String.serializer(), ListSerializer(String.serializer()))) {
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
    return CommonResponse(
        args = call.request.queryParameters.asMap(),
        headers = call.request.headers.asMap(),
        origin = origin.remoteAddress,
        url = fullUrl()
    )
}

@OptIn(ExperimentalSerializationApi::class, InternalAPI::class)
suspend fun ApplicationRequest.asBodyResponse(): BodyResponse {
    val data = call.receive<ByteArray>()
    val contentCharset = contentType().charset() ?: Charsets.UTF_8

    val text = if (contentType().match("multipart/form-data") || contentType().match("application/x-www-form-urlencoded")) {
        ""
    } else {
        data.asText(contentCharset)
    }

    val files = mutableMapOf<String, String>()
    val form = when {
        contentType().match("multipart/form-data") -> {
            val values = mutableMapOf<String, MutableList<String>>()
            val multipart = CIOMultipartDataBase(
                coroutineContext,
                ByteReadChannel(data),
                contentType().toString(),
                contentLength()
            )

            do {
                val part = multipart.readPart()

                when (part) {
                    is PartData.FormItem -> {
                        val name = part.name ?: continue
                        val list = values.getOrElse(name) { mutableListOf() }
                        list.add(part.value)
                        values[name] = list
                    }

                    is PartData.FileItem -> {
                        val name = part.name ?: continue
                        val charset = part.contentType?.charset() ?: Charsets.UTF_8
                        val mimeType = part.contentType?.toString() ?: "application/octet-stream"
                        files[name] = part.provider().readBytes().asText(charset, mimeType)
                    }

                    is PartData.BinaryItem -> {
                        val name = part.name ?: continue
                        val charset = part.contentType?.charset() ?: Charsets.UTF_8
                        val mimeType = part.contentType?.toString() ?: "application/octet-stream"
                        files[name] = part.provider().readBytes().asText(charset, mimeType)
                    }

                    else -> {}
                }

            } while (part != null)

            values
        }

        contentType().match("application/x-www-form-urlencoded") -> {
            String(data).parseUrlEncodedParameters(contentCharset).toMap()
        }

        else -> mapOf()
    }

    val json = try {
        val result = Json.parseToJsonElement(String(data))

        when {
            result is JsonPrimitive && !result.isString && result.content.toIntOrNull() != null -> result.content.toInt()
            result is JsonPrimitive && !result.isString -> JsonPrimitive(null)
            result is JsonPrimitive && result.isString -> result.content

            else -> result
        }
    } catch (cause: SerializationException) {
        JsonPrimitive(null)
    }

    return BodyResponse(
        args = call.request.queryParameters.asMap(),
        headers = call.request.headers.asMap(),
        origin = origin.remoteAddress,
        url = fullUrl(),
        data = text,
        files = files,
        form = form,
        json = json
    )
}

 @OptIn(ExperimentalEncodingApi::class)
 private suspend fun ByteArray.asText(charset: Charset, mimeType: String = "application/octet-stream"): String {
    return withContext(Dispatchers.IO) {
        try {
            charset.newDecoder().decode(ByteBuffer.wrap(this@asText)).toString()
        } catch (cause: MalformedInputException) {
            "data:$mimeType;base64,${Base64.encode(this@asText)}"
        }
    }
}

private fun ApplicationRequest.fullUrl(): String {
    val port = origin.serverPort
    return "${origin.scheme}://${origin.serverHost}${if (port != 80) ":$port" else ""}$uri"
}

private fun Headers.asMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    for ((name, values) in entries()) {
        map[name] = values.joinToString(separator = ",")
    }
    return map.toSortedMap()
}

private fun Parameters.asMap(): Map<String, List<String>> {
    val map = mutableMapOf<String, List<String>>()
    for ((name, values) in entries()) {
        map[name] = values
    }
    return map
}