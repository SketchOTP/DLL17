package com.animusmachinae.dll17.research.aliveness.agentic

/**
 * A minimal JSON writer and reader.
 *
 * This module depends on nothing, deliberately: a reviewer must not be able to
 * import the organism it adjudicates, and the cheapest way to guarantee that is
 * to have no dependency graph at all. That constraint is worth more than the
 * convenience of a JSON library, so the small amount of JSON these two provider
 * APIs need is written here.
 *
 * Scope is deliberately narrow. This is enough JSON to build a request and read
 * a response, not a general-purpose library, and it is not exported as one.
 */

/** A JSON value. Closed hierarchy: nothing outside this file can add a case. */
public sealed interface JsonValue {
    public object Null : JsonValue
    public class Bool(public val value: Boolean) : JsonValue
    public class Num(public val literal: String) : JsonValue
    public class Str(public val value: String) : JsonValue
    public class Arr(public val items: List<JsonValue>) : JsonValue
    public class Obj(public val entries: List<Pair<String, JsonValue>>) : JsonValue {
        public operator fun get(key: String): JsonValue? =
            entries.firstOrNull { it.first == key }?.second

        /** Every key appearing anywhere beneath this object, at any depth. */
        public fun keysDeep(): Set<String> {
            val out = LinkedHashSet<String>()
            fun walk(v: JsonValue) {
                when (v) {
                    is Obj -> for ((k, child) in v.entries) { out += k; walk(child) }
                    is Arr -> for (item in v.items) walk(item)
                    else -> Unit
                }
            }
            walk(this)
            return out
        }
    }
}

public fun jsonString(text: String): String = buildString {
    append('"')
    for (ch in text) {
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            '' -> append("\\f")
            else ->
                if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
        }
    }
    append('"')
}

/**
 * Serializes deterministically: entries render in the order given, never in hash
 * order. A request whose byte form varied between runs could not be hashed into
 * provenance and compared later.
 */
public fun JsonValue.render(): String = when (this) {
    is JsonValue.Null -> "null"
    is JsonValue.Bool -> value.toString()
    is JsonValue.Num -> literal
    is JsonValue.Str -> jsonString(value)
    is JsonValue.Arr -> items.joinToString(",", "[", "]") { it.render() }
    is JsonValue.Obj ->
        entries.joinToString(",", "{", "}") { (k, v) -> jsonString(k) + ":" + v.render() }
}

/** Convenience builders, so request construction reads like the request. */
public fun jObj(vararg entries: Pair<String, JsonValue>): JsonValue.Obj =
    JsonValue.Obj(entries.toList())

public fun jArr(vararg items: JsonValue): JsonValue.Arr = JsonValue.Arr(items.toList())

public fun jStr(value: String): JsonValue.Str = JsonValue.Str(value)

public fun jNum(value: Number): JsonValue.Num = JsonValue.Num(value.toString())

/** Thrown when a provider response is not the shape the parser expects. */
public class JsonParseException(message: String) : RuntimeException(message)

/**
 * Parses JSON text.
 *
 * Strict rather than forgiving: a response this parser cannot read must produce
 * an error the harness can fail closed on, never a silently empty value that a
 * caller might read as a ruling.
 */
public object Json {

    public fun parse(text: String): JsonValue {
        val p = Parser(text)
        p.skipWhitespace()
        val value = p.readValue()
        p.skipWhitespace()
        if (!p.atEnd()) throw JsonParseException("trailing content at offset ${p.offset}")
        return value
    }

    private class Parser(private val text: String) {
        var offset: Int = 0

        fun atEnd(): Boolean = offset >= text.length

        fun skipWhitespace() {
            while (offset < text.length && text[offset].isWhitespace()) offset++
        }

        private fun expect(ch: Char) {
            if (atEnd() || text[offset] != ch) {
                throw JsonParseException("expected '$ch' at offset $offset")
            }
            offset++
        }

        fun readValue(): JsonValue {
            if (atEnd()) throw JsonParseException("unexpected end of input")
            return when (val ch = text[offset]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> JsonValue.Str(readString())
                't' -> { readLiteral("true"); JsonValue.Bool(true) }
                'f' -> { readLiteral("false"); JsonValue.Bool(false) }
                'n' -> { readLiteral("null"); JsonValue.Null }
                else ->
                    if (ch == '-' || ch.isDigit()) {
                        readNumber()
                    } else {
                        throw JsonParseException("unexpected '$ch' at offset $offset")
                    }
            }
        }

        private fun readLiteral(literal: String) {
            if (!text.startsWith(literal, offset)) {
                throw JsonParseException("expected '$literal' at offset $offset")
            }
            offset += literal.length
        }

        private fun readNumber(): JsonValue.Num {
            val start = offset
            if (offset < text.length && text[offset] == '-') offset++
            while (offset < text.length && (text[offset].isDigit() || text[offset] in ".eE+-")) {
                offset++
            }
            val literal = text.substring(start, offset)
            if (literal.isEmpty()) throw JsonParseException("empty number at offset $start")
            return JsonValue.Num(literal)
        }

        private fun readString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw JsonParseException("unterminated string")
                when (val ch = text[offset++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (atEnd()) throw JsonParseException("unterminated escape")
                        when (val esc = text[offset++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (offset + 4 > text.length) {
                                    throw JsonParseException("truncated \\u escape")
                                }
                                val hex = text.substring(offset, offset + 4)
                                offset += 4
                                sb.append(
                                    hex.toIntOrNull(16)?.toChar()
                                        ?: throw JsonParseException("bad \\u escape '$hex'"),
                                )
                            }
                            else -> throw JsonParseException("bad escape '\\$esc'")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
        }

        private fun readArray(): JsonValue.Arr {
            expect('[')
            val items = mutableListOf<JsonValue>()
            skipWhitespace()
            if (!atEnd() && text[offset] == ']') { offset++; return JsonValue.Arr(items) }
            while (true) {
                skipWhitespace()
                items += readValue()
                skipWhitespace()
                if (atEnd()) throw JsonParseException("unterminated array")
                when (text[offset]) {
                    ',' -> offset++
                    ']' -> { offset++; return JsonValue.Arr(items) }
                    else -> throw JsonParseException("expected ',' or ']' at offset $offset")
                }
            }
        }

        private fun readObject(): JsonValue.Obj {
            expect('{')
            val entries = mutableListOf<Pair<String, JsonValue>>()
            skipWhitespace()
            if (!atEnd() && text[offset] == '}') { offset++; return JsonValue.Obj(entries) }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                entries += key to readValue()
                skipWhitespace()
                if (atEnd()) throw JsonParseException("unterminated object")
                when (text[offset]) {
                    ',' -> offset++
                    '}' -> { offset++; return JsonValue.Obj(entries) }
                    else -> throw JsonParseException("expected ',' or '}' at offset $offset")
                }
            }
        }
    }
}
