package com.example.hondadash

data class OBDData(
    val rpm: Int = 0,
    val speed: Int = 0,
    val coolant: Int = 185,
    val throttle: Int = 0,
    val o2Voltage: Float = 0f,
    val iat: Int = 75,
    val timing: Float = 0f,
    val stft: Float = 0f,
    val ltft: Float = 0f,
    val voltage: Float = 12.6f,
    val map: Int = 101,
    val idc: Int = 0
)

object OBD2Parser {

    private val NOISE = listOf("SEARCHING", "UNABLE TO CONNECT", "NO DATA", "BUFFER FULL", "BUS INIT", "?", "STOPPED")

    /**
     * Clean raw ELM327 response: strip echo, prompts, whitespace, known noise lines.
     * Returns just the hex data portion (e.g. "410C1AF8").
     */
    fun cleanResponse(raw: String): String {
        return raw
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { line ->
                line.isNotEmpty() &&
                line != ">" &&
                NOISE.none { noise -> line.uppercase().contains(noise) }
            }
            .joinToString("")
            .replace(">", "")
            .replace(" ", "")
            .trim()
    }

    /** Check if the response indicates an error or no data. */
    fun isError(raw: String): Boolean {
        val upper = raw.uppercase()
        return NOISE.any { upper.contains(it) }
    }

    // ============ MODE 01 PID PARSERS ============

    /** RPM: PID 0C — ((A*256)+B)/4 */
    fun parseRPM(raw: String): Int? {
        val hex = extractDataBytes(cleanResponse(raw), "410C", 2) ?: return null
        val a = hex[0]
        val b = hex[1]
        return ((a * 256) + b) / 4
    }

    /** Vehicle Speed: PID 0D — A (km/h, converted to MPH) */
    fun parseSpeed(raw: String): Int? {
        val hex = extractDataBytes(cleanResponse(raw), "410D", 1) ?: return null
        return (hex[0] * 0.621371).toInt()
    }

    /** Coolant Temp: PID 05 — A-40 (°C, converted to °F) */
    fun parseCoolant(raw: String): Int? {
        val hex = extractDataBytes(cleanResponse(raw), "4105", 1) ?: return null
        val celsius = hex[0] - 40
        return (celsius * 9 / 5) + 32
    }

    /** Throttle Position: PID 11 — A*100/255 */
    fun parseThrottle(raw: String): Int? {
        val hex = extractDataBytes(cleanResponse(raw), "4111", 1) ?: return null
        return hex[0] * 100 / 255
    }

    /** O2 Sensor Voltage: PID 14 — A/200 */
    fun parseO2Voltage(raw: String): Float? {
        val hex = extractDataBytes(cleanResponse(raw), "4114", 1) ?: return null
        return hex[0] / 200f
    }

    /** Intake Air Temp: PID 0F — A-40 (°C, converted to °F) */
    fun parseIAT(raw: String): Int? {
        val hex = extractDataBytes(cleanResponse(raw), "410F", 1) ?: return null
        val celsius = hex[0] - 40
        return (celsius * 9 / 5) + 32
    }

    /** Timing Advance: PID 0E — A/2 - 64 */
    fun parseTiming(raw: String): Float? {
        val hex = extractDataBytes(cleanResponse(raw), "410E", 1) ?: return null
        return hex[0] / 2f - 64f
    }

    /** Short Term Fuel Trim: PID 06 — (A-128)*100/128 */
    fun parseSTFT(raw: String): Float? {
        val hex = extractDataBytes(cleanResponse(raw), "4106", 1) ?: return null
        return (hex[0] - 128) * 100f / 128f
    }

    /** Long Term Fuel Trim: PID 07 — (A-128)*100/128 */
    fun parseLTFT(raw: String): Float? {
        val hex = extractDataBytes(cleanResponse(raw), "4107", 1) ?: return null
        return (hex[0] - 128) * 100f / 128f
    }

    /** MAP (Manifold Absolute Pressure): PID 0B — A (kPa) */
    fun parseMAP(raw: String): Int? {
        val hex = extractDataBytes(cleanResponse(raw), "410B", 1) ?: return null
        return hex[0]
    }

    /** Battery Voltage from ATRV command — parse float like "12.4V" */
    fun parseVoltage(raw: String): Float? {
        val cleaned = raw.replace("\r", "").replace("\n", "").replace(">", "").trim()
        val match = Regex("""(\d+\.?\d*)""").find(cleaned)
        return match?.groupValues?.get(1)?.toFloatOrNull()
    }

    // ============ MODE 03 DTC PARSER ============

    /**
     * Parse Mode 03 (stored DTCs) response.
     * Response format: "43 XX YY XX YY ..." where each XX YY pair is one DTC.
     * First 2 bits of XX = letter (00=P, 01=C, 10=B, 11=U)
     * Next 2 bits of XX = second digit
     * YY = last two hex digits
     */
    fun parseDTCs(raw: String): List<String> {
        val cleaned = cleanResponse(raw)
        if (cleaned.length < 4 || !cleaned.startsWith("43")) return emptyList()

        // Strip the "43" header
        val data = cleaned.substring(2)
        val codes = mutableListOf<String>()

        var i = 0
        while (i + 4 <= data.length) {
            val byte1 = data.substring(i, i + 2).toIntOrNull(16) ?: break
            val byte2 = data.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            i += 4

            // Skip 0000 padding
            if (byte1 == 0 && byte2 == 0) continue

            val letterBits = (byte1 shr 6) and 0x03
            val letter = when (letterBits) {
                0 -> 'P'
                1 -> 'C'
                2 -> 'B'
                3 -> 'U'
                else -> 'P'
            }
            val digit2 = (byte1 shr 4) and 0x03
            val digit34 = byte1 and 0x0F
            val digit56 = byte2

            val code = "$letter${digit2}${digit34.toString(16).uppercase()}${digit56.toString(16).uppercase().padStart(2, '0')}"
            codes.add(code)
        }

        return codes
    }

    // ============ HELPERS ============

    /**
     * Extract N data bytes after the expected header from cleaned hex string.
     * E.g., extractDataBytes("410C1AF8", "410C", 2) → [0x1A, 0xF8] = [26, 248]
     */
    private fun extractDataBytes(cleaned: String, header: String, count: Int): List<Int>? {
        val headerUpper = header.uppercase()
        val idx = cleaned.uppercase().indexOf(headerUpper)
        if (idx < 0) return null

        val dataStart = idx + headerUpper.length
        val needed = count * 2
        if (dataStart + needed > cleaned.length) return null

        val bytes = mutableListOf<Int>()
        for (b in 0 until count) {
            val hexByte = cleaned.substring(dataStart + b * 2, dataStart + b * 2 + 2)
            val value = hexByte.toIntOrNull(16) ?: return null
            bytes.add(value)
        }
        return bytes
    }
}
