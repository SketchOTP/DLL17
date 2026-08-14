package com.animusmachinae.dll17.services.identity

import com.animusmachinae.dll17.core.recovery.AuthorityResponse
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityProtocol
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityTransport
import com.animusmachinae.dll17.core.recovery.TransportFailure
import com.animusmachinae.dll17.core.recovery.TransportFault
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * The network surface of the identity authority.
 *
 * ### Why the JDK's own server and not a framework
 *
 * D014 required an external check before choosing, and one was performed and
 * recorded as `PA-0003`. Ktor, Javalin, http4k and Spring Boot are all capable
 * and all maintained; the deciding fact is what this service actually is. It has
 * four endpoints, one bounded binary body shape, no routing beyond a path match,
 * no templating, no serialization layer, no dependency injection and no session
 * state. Against that, a framework contributes a dependency tree to keep current
 * on a service whose entire job is to be trustworthy about *one* number.
 *
 * `com.sun.net.httpserver` is a supported JDK API (`jdk.httpserver`), ships with
 * the runtime this project already pins, and adds no supply chain at all. The
 * cost of the decision is honest and recorded: no HTTP/2, no built-in TLS
 * termination worth using, and connection handling that expects a reverse proxy
 * in front. `OPERATIONS.md` states that requirement rather than hiding it.
 *
 * ### Serialized by design
 *
 * Every protocol call runs under one lock. The authority's whole purpose is an
 * atomic compare-and-swap on an epoch, and the cheapest correct way to keep that
 * atomic across concurrent requests is to not have concurrency inside it. This is
 * a service handling a handful of requests per organism per *lifetime*; the
 * throughput this costs is worth nothing and the reasoning it saves is worth a
 * great deal.
 */
public class IdentityAuthorityHttpServer(
    private val service: IdentityAuthorityService,
    private val storeDirectory: File,
    port: Int = 0,
    backlog: Int = 32,
    threads: Int = 8,
    /**
     * Loopback by default.
     *
     * Binding anything wider is a deployment decision that has to be made on
     * purpose: `OPERATIONS.md` requires a reverse proxy for TLS and rate
     * limiting, and a service that bound every interface by default would be
     * reachable without one the first time somebody ran it on a host with a
     * public address.
     */
    bindAddress: String = "127.0.0.1",
    private val clock: () -> Long = System::currentTimeMillis,
) {

    public companion object {
        public const val SERVER_ID: String = "IdentityAuthorityHttpServerV1"

        public const val PORT_VARIABLE: String = "DLL17_AUTHORITY_PORT"
        public const val STORE_VARIABLE: String = "DLL17_AUTHORITY_STORE"
        public const val BIND_VARIABLE: String = "DLL17_AUTHORITY_BIND"

        private val ENDPOINTS: Map<String, Int> = mapOf(
            IdentityAuthorityTransport.PATH_REGISTER to IdentityAuthorityProtocol.REGISTER_SCHEMA_ID,
            IdentityAuthorityTransport.PATH_CHALLENGE to IdentityAuthorityProtocol.CHALLENGE_SCHEMA_ID,
            IdentityAuthorityTransport.PATH_ACTIVATE to IdentityAuthorityProtocol.ACTIVATE_SCHEMA_ID,
            IdentityAuthorityTransport.PATH_HEARTBEAT to IdentityAuthorityProtocol.HEARTBEAT_SCHEMA_ID,
        )
    }

    private val server: HttpServer = HttpServer.create(InetSocketAddress(bindAddress, port), backlog)
    private val executor: ThreadPoolExecutor =
        Executors.newFixedThreadPool(threads) as ThreadPoolExecutor
    private val lock = Any()

    /**
     * Structured access log. Counts and enum names only.
     *
     * There is no organism content in a line here and there is no field that
     * could carry one: an organism id, a path, a status and a request id. It is
     * bounded so a service under attack cannot be made to exhaust memory by
     * being talked to.
     */
    private val accessLog = ArrayDeque<String>()

    public val port: Int get() = server.address.port

    public fun accessLogSnapshot(): List<String> = synchronized(lock) { accessLog.toList() }

    public fun start() {
        for ((path, schemaId) in ENDPOINTS) {
            server.createContext(path) { exchange -> handleProtocol(exchange, path, schemaId) }
        }
        server.createContext(IdentityAuthorityTransport.PATH_HEALTH) { exchange ->
            // Liveness answers "is this process running", and must not depend on
            // durable state: a health check that fails when the disk fails takes
            // the service out of rotation exactly when an operator needs to reach
            // it to find out why.
            respond(exchange, 200, "ok".toByteArray(Charsets.UTF_8), "text/plain")
        }
        server.createContext(IdentityAuthorityTransport.PATH_READY) { exchange ->
            val usable = durableStateUsable()
            respond(
                exchange,
                if (usable) 200 else 503,
                (if (usable) "ready" else "durable state unavailable").toByteArray(Charsets.UTF_8),
                "text/plain",
            )
        }
        server.executor = executor
        server.start()
    }

    public fun stop(delaySeconds: Int = 0) {
        server.stop(delaySeconds)
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    /**
     * Whether the authority could durably record an epoch advance right now.
     *
     * Checked rather than assumed, because the failure this guards against is the
     * dangerous one: an authority that grants an activation it cannot persist has
     * told two devices they hold the same organism.
     */
    private fun durableStateUsable(): Boolean =
        storeDirectory.isDirectory && storeDirectory.canWrite()

    private fun handleProtocol(exchange: HttpExchange, path: String, schemaId: Int) {
        try {
            if (exchange.requestMethod != "POST") {
                return refuse(exchange, path, TransportFailure.METHOD_NOT_ALLOWED)
            }
            val contentType = exchange.requestHeaders.getFirst("Content-Type")
            if (contentType == null || !contentType.startsWith(IdentityAuthorityTransport.MEDIA_TYPE)) {
                return refuse(exchange, path, TransportFailure.UNSUPPORTED_MEDIA_TYPE)
            }
            val requestId = exchange.requestHeaders.getFirst(IdentityAuthorityTransport.REQUEST_ID_HEADER)
            if (!IdentityAuthorityTransport.isWellFormedRequestId(requestId)) {
                return refuse(exchange, path, TransportFailure.MISSING_REQUEST_ID)
            }
            // Read one byte past the ceiling so an oversized body is refused
            // rather than truncated into something that happens to parse.
            val body = readBounded(exchange, IdentityAuthorityTransport.MAX_REQUEST_BYTES + 1)
            if (body.size > IdentityAuthorityTransport.MAX_REQUEST_BYTES) {
                return refuse(exchange, path, TransportFailure.TOO_LARGE, requestId)
            }
            if (!durableStateUsable()) {
                return refuse(exchange, path, TransportFailure.DURABLE_STATE_UNAVAILABLE, requestId)
            }

            val request = IdentityAuthorityTransport.decodeRequest(schemaId, body)
            // The server's clock, never the caller's. See the transport contract.
            val now = clock()
            val response = synchronized(lock) {
                when (schemaId) {
                    IdentityAuthorityProtocol.REGISTER_SCHEMA_ID -> service.register(
                        request.organismId, request.verificationKey, request.deviceFingerprint, now,
                    )
                    IdentityAuthorityProtocol.CHALLENGE_SCHEMA_ID ->
                        service.challenge(request.organismId, now)
                    IdentityAuthorityProtocol.ACTIVATE_SCHEMA_ID -> service.activate(
                        request.organismId, request.requestedEpoch, request.nonce,
                        request.proof, request.deviceFingerprint, now,
                    )
                    IdentityAuthorityProtocol.HEARTBEAT_SCHEMA_ID -> service.heartbeat(
                        request.organismId, request.claimedEpoch, request.deviceFingerprint, now,
                    )
                    else -> throw TransportFault(TransportFailure.NOT_FOUND, "no such endpoint")
                }
            }
            log(path, request.organismId, response.outcome.name, requestId)
            exchange.responseHeaders.add(IdentityAuthorityTransport.REQUEST_ID_HEADER, requestId)
            respond(
                exchange,
                IdentityAuthorityTransport.httpStatusFor(response.outcome),
                IdentityAuthorityTransport.encodeResponse(response),
                IdentityAuthorityTransport.MEDIA_TYPE,
            )
        } catch (fault: TransportFault) {
            refuse(exchange, path, fault.failure)
        } catch (unexpected: Exception) {
            // An unexpected server fault must never look like a protocol
            // decision. It is 503 with an empty body: a client that cannot
            // decode a response treats it as unavailable, which is correct.
            log(path, 0L, "INTERNAL_FAULT", null)
            respond(exchange, 503, ByteArray(0), "text/plain")
        }
    }

    private fun readBounded(exchange: HttpExchange, limit: Int): ByteArray {
        val buffer = ByteArray(limit)
        var read = 0
        exchange.requestBody.use { stream ->
            while (read < limit) {
                val n = stream.read(buffer, read, limit - read)
                if (n < 0) break
                read += n
            }
        }
        return buffer.copyOf(read)
    }

    private fun refuse(
        exchange: HttpExchange,
        path: String,
        failure: TransportFailure,
        requestId: String? = null,
    ) {
        log(path, 0L, failure.name, requestId)
        // No body. A transport refusal is deliberately not decodable as a
        // protocol response, so a client can never mistake one for the other.
        respond(exchange, failure.httpStatus, ByteArray(0), "text/plain")
    }

    private fun respond(exchange: HttpExchange, status: Int, body: ByteArray, contentType: String) {
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(status, if (body.isEmpty()) -1L else body.size.toLong())
        if (body.isNotEmpty()) exchange.responseBody.use { it.write(body) }
        exchange.close()
    }

    private fun log(path: String, organismId: Long, outcome: String, requestId: String?) {
        synchronized(lock) {
            if (accessLog.size >= 4096) accessLog.removeFirst()
            accessLog.addLast("$path organism=$organismId outcome=$outcome request=${requestId ?: "-"}")
        }
    }
}

/**
 * The deployable entry point.
 *
 * Configuration is read from the environment and nowhere else — no file the
 * process discovers on its own, no defaults that would let a misconfigured
 * deployment quietly come up pointing at the wrong store.
 */
public object IdentityAuthorityServiceMain {
    @JvmStatic
    public fun main(arguments: Array<String>) {
        val store = System.getenv(IdentityAuthorityHttpServer.STORE_VARIABLE)
            ?: error("${IdentityAuthorityHttpServer.STORE_VARIABLE} must be set")
        val port = System.getenv(IdentityAuthorityHttpServer.PORT_VARIABLE)?.toIntOrNull()
            ?: error("${IdentityAuthorityHttpServer.PORT_VARIABLE} must be set to a port number")
        val directory = File(store)
        val bind = System.getenv(IdentityAuthorityHttpServer.BIND_VARIABLE) ?: "127.0.0.1"
        val server = IdentityAuthorityHttpServer(
            service = IdentityAuthorityService(directory),
            storeDirectory = directory,
            port = port,
            bindAddress = bind,
        )
        server.start()
        Runtime.getRuntime().addShutdownHook(Thread { server.stop(2) })
        println("${IdentityAuthorityHttpServer.SERVER_ID} listening on $bind:${server.port} store=$store")
        if (bind != "127.0.0.1") {
            println(
                "WARNING: bound to $bind. This service has no TLS and no rate limiting of its " +
                    "own; OPERATIONS.md requires a reverse proxy in front of it.",
            )
        }
    }
}
