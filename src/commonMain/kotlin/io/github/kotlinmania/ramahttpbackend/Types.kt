package io.github.kotlinmania.ramahttpbackend

import kotlin.reflect.KClass

/**
 * Result type for Rama operations.
 */
public sealed class RamaResult<out T, out E> {
    public data class Ok<out T>(public val value: T) : RamaResult<T, Nothing>()
    public data class Err<out E>(public val error: E) : RamaResult<Nothing, E>()

    public val isOk: Boolean get() = this is Ok
    public val isErr: Boolean get() = this is Err

    public fun unwrap(): T = when (this) {
        is Ok -> value
        is Err -> throw IllegalStateException("Called unwrap on Err: $error")
    }

    public fun unwrapErr(): E = when (this) {
        is Ok -> throw IllegalStateException("Called unwrapErr on Ok: $value")
        is Err -> error
    }

    public inline fun <R> map(transform: (T) -> R): RamaResult<R, E> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

    public inline fun <F> mapErr(transform: (E) -> F): RamaResult<T, F> = when (this) {
        is Ok -> this
        is Err -> Err(transform(error))
    }

    public companion object {
        public fun <T> ok(value: T): RamaResult<T, Nothing> = Ok(value)
        public fun <E> err(error: E): RamaResult<Nothing, E> = Err(error)
    }
}

/**
 * A type map of protocol extensions.
 */
public class Extensions {
    private val entries: MutableList<Entry> = mutableListOf()

    private class Entry(
        val typeId: KClass<*>,
        val value: Any,
    )

    public fun extend(other: Extensions): Extensions {
        entries.addAll(other.entries)
        return this
    }

    public fun copy(): Extensions {
        val out = Extensions()
        out.entries.addAll(entries)
        return out
    }

    public fun clone(): Extensions = copy()

    public inline fun <reified T : Any> insert(value: T): Extensions {
        insertErased(T::class, value)
        return this
    }

    @PublishedApi
    internal fun insertErased(typeId: KClass<*>, value: Any) {
        entries.add(Entry(typeId, value))
    }

    public inline fun <reified T : Any> contains(): Boolean = containsErased(T::class)

    @PublishedApi
    internal fun containsErased(typeId: KClass<*>): Boolean {
        for (i in entries.indices.reversed()) {
            if (entries[i].typeId == typeId) return true
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    public inline fun <reified T : Any> get(): T? = getErased(T::class) as? T

    @PublishedApi
    internal fun getErased(typeId: KClass<*>): Any? {
        for (i in entries.indices.reversed()) {
            val entry = entries[i]
            if (entry.typeId == typeId) return entry.value
        }
        return null
    }

    public inline fun <reified T : Any> remove(): T? = removeErased(T::class) as? T

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun removeErased(typeId: KClass<*>): Any? {
        for (i in entries.indices.reversed()) {
            if (entries[i].typeId == typeId) {
                return entries.removeAt(i).value
            }
        }
        return null
    }

    public companion object {
        public fun new(): Extensions = Extensions()
    }
}

/**
 * Network protocol definition.
 */
public sealed class NetProtocol(public val scheme: String) : Comparable<NetProtocol> {
    public open fun isHttp(): Boolean = false
    public open fun isWs(): Boolean = false
    public open fun isSocks5(): Boolean = false
    public open fun isSecure(): Boolean = false
    public open fun defaultPort(): Int? = null

    override fun toString(): String = scheme
    override fun compareTo(other: NetProtocol): Int = scheme.compareTo(other.scheme, ignoreCase = true)

    public data object Http : NetProtocol("http") {
        override fun isHttp(): Boolean = true
        override fun defaultPort(): Int = 80
    }

    public data object Https : NetProtocol("https") {
        override fun isHttp(): Boolean = true
        override fun isSecure(): Boolean = true
        override fun defaultPort(): Int = 443
    }

    public data object Ws : NetProtocol("ws") {
        override fun isWs(): Boolean = true
        override fun defaultPort(): Int = 80
    }

    public data object Wss : NetProtocol("wss") {
        override fun isWs(): Boolean = true
        override fun isSecure(): Boolean = true
        override fun defaultPort(): Int = 443
    }

    public data object Socks5 : NetProtocol("socks5") {
        override fun isSocks5(): Boolean = true
        override fun defaultPort(): Int = 1080
    }

    public data object Socks5h : NetProtocol("socks5h") {
        override fun isSocks5(): Boolean = true
        override fun defaultPort(): Int = 1080
    }

    public companion object {
        public const val HTTP_DEFAULT_PORT: Int = 80
        public const val HTTPS_DEFAULT_PORT: Int = 443

        public fun parse(scheme: String): NetProtocol? = when (scheme.lowercase()) {
            "http" -> Http
            "https" -> Https
            "ws" -> Ws
            "wss" -> Wss
            "socks5" -> Socks5
            "socks5h" -> Socks5h
            else -> null
        }
    }
}

/**
 * Proxy authentication credentials.
 */
public sealed class ProxyCredential {
    public data class Basic(val username: String, val password: String? = null) : ProxyCredential() {
        public fun headerValue(): String {
            val raw = if (password != null) "$username:$password" else username
            return "Basic " + raw.encodeToByteArray().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
        }
    }

    public data class Bearer(val token: String) : ProxyCredential() {
        public fun headerValue(): String = "Bearer $token"
    }
}

/**
 * Host name with mandatory port number.
 */
public data class HostWithPort(val host: String, val port: Int) {
    override fun toString(): String = "$host:$port"

    public companion object {
        public fun exampleDomainHttp(): HostWithPort = HostWithPort("example.com", 80)
        public fun exampleDomainHttps(): HostWithPort = HostWithPort("example.com", 443)
    }
}

/**
 * Host name with optional port number.
 */
public data class HostWithOptPort(val host: String, val port: Int? = null) {
    override fun toString(): String = if (port != null) "$host:$port" else host

    public companion object {
        public fun parse(s: String): HostWithOptPort {
            val colonIdx = s.indexOf(':')
            return if (colonIdx != -1) {
                val host = s.substring(0, colonIdx)
                val port = s.substring(colonIdx + 1).toIntOrNull()
                HostWithOptPort(host, port)
            } else {
                HostWithOptPort(s, null)
            }
        }
    }
}

/**
 * Proxy address specification.
 */
public data class ProxyAddress(
    val address: HostWithPort,
    val protocol: NetProtocol? = null,
    val credential: ProxyCredential? = null,
) {
    override fun toString(): String {
        val sb = StringBuilder()
        if (protocol != null) {
            sb.append(protocol.scheme).append("://")
        }
        if (credential != null && credential is ProxyCredential.Basic) {
            if (credential.password != null) {
                sb.append(credential.username).append(':').append(credential.password).append('@')
            } else {
                sb.append(credential.username).append('@')
            }
        }
        sb.append(address.toString())
        return sb.toString()
    }

    public companion object {
        public fun parse(s: String): ProxyAddress {
            var input = s.trim()
            var proto: NetProtocol? = null
            val protoIdx = input.indexOf("://")
            if (protoIdx != -1) {
                val scheme = input.substring(0, protoIdx)
                proto = NetProtocol.parse(scheme)
                input = input.substring(protoIdx + 3)
            }

            var credential: ProxyCredential? = null
            val atIdx = input.lastIndexOf('@')
            if (atIdx != -1) {
                val userPass = input.substring(0, atIdx)
                input = input.substring(atIdx + 1)
                val credColonIdx = userPass.indexOf(':')
                credential = if (credColonIdx != -1) {
                    ProxyCredential.Basic(userPass.substring(0, credColonIdx), userPass.substring(credColonIdx + 1))
                } else {
                    ProxyCredential.Basic(userPass, null)
                }
            }

            val colonIdx = input.indexOf(':')
            val host: String
            val port: Int
            if (colonIdx != -1) {
                host = input.substring(0, colonIdx)
                port = input.substring(colonIdx + 1).toIntOrNull() ?: proto?.defaultPort() ?: 80
            } else {
                host = input
                port = proto?.defaultPort() ?: 80
            }

            return ProxyAddress(
                address = HostWithPort(host, port),
                protocol = proto,
                credential = credential,
            )
        }
    }
}

/**
 * Context of a request for transport and proxy handling.
 */
public data class RequestContext(
    val authority: HostWithOptPort,
    val protocol: NetProtocol,
) {
    public fun authorityHasDefaultPort(): Boolean {
        val def = protocol.defaultPort()
        return authority.port == null || (def != null && authority.port == def)
    }

    public companion object {
        public fun tryFrom(req: Request): RequestContext? {
            val ctx = req.extensions.get<RequestContext>()
            if (ctx != null) return ctx

            val hostStr = req.uri.host
            val port = req.uri.port
            val scheme = req.uri.scheme
            val proto = if (scheme != null) NetProtocol.parse(scheme) ?: NetProtocol.Http else NetProtocol.Http

            val authority = if (hostStr != null) {
                HostWithOptPort(hostStr, port)
            } else {
                val hostHeader = req.headers.get("host")
                if (hostHeader != null) {
                    HostWithOptPort.parse(hostHeader)
                } else {
                    return null
                }
            }

            return RequestContext(authority, proto)
        }
    }
}

/**
 * HTTP Method representation.
 */
public enum class Method(public val text: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    CONNECT("CONNECT"),
    PATCH("PATCH"),
    TRACE("TRACE");

    public fun asStr(): String = text
    override fun toString(): String = text

    public companion object {
        public fun fromString(s: String): Method =
            entries.firstOrNull { it.text.equals(s, ignoreCase = true) } ?: GET
    }
}

/**
 * HTTP Version representation.
 */
public enum class Version(public val text: String) {
    HTTP_09("HTTP/0.9"),
    HTTP_10("HTTP/1.0"),
    HTTP_11("HTTP/1.1"),
    HTTP_2("HTTP/2.0"),
    HTTP_3("HTTP/3.0");

    override fun toString(): String = text

    public companion object {
        public fun fromString(s: String): Version =
            entries.firstOrNull { it.text.equals(s, ignoreCase = true) } ?: HTTP_11
    }
}

/**
 * HTTP Status Code.
 */
public data class StatusCode(public val code: Int, public val reason: String = "") {
    override fun toString(): String = if (reason.isNotEmpty()) "$code $reason" else "$code"

    public companion object {
        public val OK: StatusCode = StatusCode(200, "OK")
        public val PROXY_AUTHENTICATION_REQUIRED: StatusCode = StatusCode(407, "Proxy Authentication Required")
        public val SERVICE_UNAVAILABLE: StatusCode = StatusCode(503, "Service Unavailable")
    }
}

/**
 * URI representation.
 */
public data class Uri(
    public val scheme: String? = null,
    public val authority: String? = null,
    public val pathAndQuery: String = "/",
) {
    public val host: String?
        get() = authority?.substringBefore(':')

    public val port: Int?
        get() {
            val p = authority?.substringAfter(':', "")
            return if (!p.isNullOrEmpty()) p.toIntOrNull() else null
        }

    public val path: String
        get() = pathAndQuery.substringBefore('?')

    public val query: String?
        get() = if (pathAndQuery.contains('?')) pathAndQuery.substringAfter('?') else null

    override fun toString(): String {
        val sb = StringBuilder()
        if (scheme != null) {
            sb.append(scheme).append("://")
        }
        if (authority != null) {
            sb.append(authority)
        }
        if (pathAndQuery.isNotEmpty()) {
            if (authority != null && !pathAndQuery.startsWith('/')) {
                sb.append('/')
            }
            sb.append(pathAndQuery)
        }
        return sb.toString()
    }

    public fun intoParts(): UriParts = UriParts(scheme, authority, pathAndQuery)

    public companion object {
        public fun fromString(s: String): Uri {
            var rest = s
            var scheme: String? = null
            val protoIdx = rest.indexOf("://")
            if (protoIdx != -1) {
                scheme = rest.substring(0, protoIdx)
                rest = rest.substring(protoIdx + 3)
            }

            var authority: String? = null
            var pathAndQuery = "/"
            val slashIdx = rest.indexOf('/')
            if (slashIdx != -1) {
                if (protoIdx != -1) {
                    authority = rest.substring(0, slashIdx)
                    pathAndQuery = rest.substring(slashIdx)
                } else {
                    pathAndQuery = rest
                }
            } else {
                if (protoIdx != -1 || rest.contains(':')) {
                    authority = rest
                    pathAndQuery = "/"
                } else if (rest.startsWith('/')) {
                    pathAndQuery = rest
                } else {
                    authority = rest
                    pathAndQuery = "/"
                }
            }

            return Uri(scheme, authority, pathAndQuery)
        }

        public fun fromParts(parts: UriParts): Uri =
            Uri(parts.scheme, parts.authority, parts.pathAndQuery ?: "/")

        public fun builder(): UriBuilder = UriBuilder()
    }
}

public data class UriParts(
    public var scheme: String? = null,
    public var authority: String? = null,
    public var pathAndQuery: String? = null,
)

public class UriBuilder {
    private var scheme: String? = null
    private var authority: String? = null
    private var pathAndQuery: String = "/"

    public fun scheme(scheme: String): UriBuilder {
        this.scheme = scheme
        return this
    }

    public fun authority(authority: String): UriBuilder {
        this.authority = authority
        return this
    }

    public fun pathAndQuery(pathAndQuery: String): UriBuilder {
        this.pathAndQuery = pathAndQuery
        return this
    }

    public fun build(): RamaResult<Uri, String> =
        RamaResult.ok(Uri(scheme, authority, pathAndQuery))
}

/**
 * Case-insensitive HTTP Header Map.
 */
public class HeaderMap {
    private val headers: MutableMap<String, String> = mutableMapOf()

    public fun insert(name: String, value: String): HeaderMap {
        headers[name.lowercase()] = value
        return this
    }

    public fun append(name: String, value: String): HeaderMap {
        val key = name.lowercase()
        val existing = headers[key]
        if (existing != null) {
            headers[key] = "$existing, $value"
        } else {
            headers[key] = value
        }
        return this
    }

    public fun get(name: String): String? = headers[name.lowercase()]

    public fun containsKey(name: String): Boolean = headers.containsKey(name.lowercase())

    public fun remove(name: String): String? = headers.remove(name.lowercase())

    public fun clone(): HeaderMap {
        val out = HeaderMap()
        out.headers.putAll(headers)
        return out
    }

    public fun toMap(): Map<String, String> = headers.toMap()

    override fun toString(): String = headers.toString()
}

/**
 * HTTP Body payload.
 */
public class Body(private val bytes: ByteArray) {
    public val size: Int get() = bytes.size
    public fun toByteArray(): ByteArray = bytes.copyOf()
    public fun asString(): String = bytes.decodeToString()

    override fun equals(other: Any?): Boolean =
        other is Body && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    public companion object {
        public fun empty(): Body = Body(ByteArray(0))
        public fun from(s: String): Body = Body(s.encodeToByteArray())
        public fun from(bytes: ByteArray): Body = Body(bytes.copyOf())
        public fun new(bytes: ByteArray): Body = Body(bytes.copyOf())
    }
}

/**
 * Request Parts.
 */
public class RequestParts(
    public var method: Method = Method.GET,
    public var uri: Uri = Uri.fromString("/"),
    public var version: Version = Version.HTTP_11,
    public var headers: HeaderMap = HeaderMap(),
    public var extensions: Extensions = Extensions(),
)

/**
 * HTTP Request representation.
 */
public class Request(
    public var method: Method = Method.GET,
    public var uri: Uri = Uri.fromString("/"),
    public var version: Version = Version.HTTP_11,
    public val headers: HeaderMap = HeaderMap(),
    public val extensions: Extensions = Extensions(),
    public var body: Body = Body.empty(),
) {
    public fun headersMut(): HeaderMap = headers
    public fun extensionsMut(): Extensions = extensions

    public fun intoParts(): Pair<RequestParts, Body> {
        val parts = RequestParts(method, uri, version, headers.clone(), extensions.clone())
        return Pair(parts, body)
    }

    public companion object {
        public fun builder(): RequestBuilder = RequestBuilder()

        public fun fromParts(parts: RequestParts, body: Body): Request =
            Request(
                method = parts.method,
                uri = parts.uri,
                version = parts.version,
                headers = parts.headers,
                extensions = parts.extensions,
                body = body,
            )

        public fun new(body: Body): Request =
            Request(
                method = Method.GET,
                uri = Uri.fromString("/"),
                version = Version.HTTP_11,
                headers = HeaderMap(),
                extensions = Extensions(),
                body = body,
            )
    }
}

public class RequestBuilder {
    private var method: Method = Method.GET
    private var uri: Uri = Uri.fromString("/")
    private var version: Version = Version.HTTP_11
    private val headers: HeaderMap = HeaderMap()
    private val extensions: Extensions = Extensions()

    public fun method(method: Method): RequestBuilder {
        this.method = method
        return this
    }

    public fun method(method: String): RequestBuilder {
        this.method = Method.fromString(method)
        return this
    }

    public fun uri(uri: Uri): RequestBuilder {
        this.uri = uri
        return this
    }

    public fun uri(uri: String): RequestBuilder {
        this.uri = Uri.fromString(uri)
        return this
    }

    public fun version(version: Version): RequestBuilder {
        this.version = version
        return this
    }

    public fun header(name: String, value: String): RequestBuilder {
        this.headers.insert(name, value)
        return this
    }

    public fun extension(value: Any): RequestBuilder {
        this.extensions.insert(value)
        return this
    }

    public fun body(body: Body): RamaResult<Request, String> =
        RamaResult.ok(Request(method, uri, version, headers, extensions, body))

    public fun body(unit: Unit): RamaResult<Request, String> =
        body(Body.empty())
}

/**
 * Response Parts.
 */
public class ResponseParts(
    public var status: StatusCode = StatusCode.OK,
    public var version: Version = Version.HTTP_11,
    public var headers: HeaderMap = HeaderMap(),
    public var extensions: Extensions = Extensions(),
)

/**
 * HTTP Response representation.
 */
public class Response(
    public var status: StatusCode = StatusCode.OK,
    public var version: Version = Version.HTTP_11,
    public val headers: HeaderMap = HeaderMap(),
    public val extensions: Extensions = Extensions(),
    public var body: Body = Body.empty(),
) {
    public fun headersMut(): HeaderMap = headers
    public fun extensionsMut(): Extensions = extensions

    public fun intoParts(): Pair<ResponseParts, Body> {
        val parts = ResponseParts(status, version, headers.clone(), extensions.clone())
        return Pair(parts, body)
    }

    public companion object {
        public fun new(body: Body): Response =
            Response(
                status = StatusCode.OK,
                version = Version.HTTP_11,
                headers = HeaderMap(),
                extensions = Extensions(),
                body = body,
            )
    }
}

/**
 * Service interface.
 */
public interface Service<in Input, out Output : Any, out Error : Any> {
    public suspend fun serve(input: Input): RamaResult<Output, Error>
    public fun boxed(): BoxService<Input, Output, Error> = BoxService(this)
}

public class BoxService<in Input, out Output : Any, out Error : Any>(
    private val inner: Service<Input, Output, Error>,
) : Service<Input, Output, Error> {
    override suspend fun serve(input: Input): RamaResult<Output, Error> = inner.serve(input)
    override fun boxed(): BoxService<Input, Output, Error> = this
}

/**
 * Middleware Layer interface.
 */
public interface Layer<in S, out OutService> {
    public fun layer(inner: S): OutService
    public fun intoLayer(inner: S): OutService = layer(inner)
}

/**
 * Request matcher.
 */
public interface Matcher<in T> {
    public fun matches(ext: Extensions?, input: T): Boolean
}

/**
 * Async stream / connection abstraction.
 */
public interface Stream {
    public val extensions: Extensions
}

/**
 * Upgraded HTTP connection.
 */
public class Upgraded(
    public val stream: Stream,
    private val ext: Extensions = Extensions(),
) : Stream {
    override val extensions: Extensions get() = ext
    public fun extensionsMut(): Extensions = ext
}

/**
 * Established client connection wrapper.
 */
public data class EstablishedClientConnection<out C, out I>(
    public val conn: C,
    public val input: I,
)

/**
 * Executor abstraction.
 */
public class Executor {
    public fun guard(): ShutdownGuard? = null

    public companion object {
        public val default: Executor = Executor()
        public fun new(): Executor = Executor()
    }
}

/**
 * Shutdown guard.
 */
public class ShutdownGuard {
    public fun cancelled(): Boolean = false
}
