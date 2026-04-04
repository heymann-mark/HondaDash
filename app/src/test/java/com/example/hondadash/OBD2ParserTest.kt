package com.example.hondadash

import org.junit.Test
import org.junit.Assert.*

class OBD2ParserTest {

    // ================================================================
    //  cleanResponse
    // ================================================================

    @Test
    fun cleanResponse_stripsWhitespace() {
        assertEquals("410C1AF8", OBD2Parser.cleanResponse("41 0C 1A F8"))
    }

    @Test
    fun cleanResponse_stripsPrompt() {
        assertEquals("410C1AF8", OBD2Parser.cleanResponse("410C1AF8\r\n>"))
    }

    @Test
    fun cleanResponse_stripsEchoAndNoise() {
        assertEquals("410C1AF8", OBD2Parser.cleanResponse("SEARCHING...\r\n410C1AF8\r\n>"))
    }

    @Test
    fun cleanResponse_handlesMultilineWithNoise() {
        // Echo "010C" is not filtered (it's valid hex), so result includes it
        val result = OBD2Parser.cleanResponse("010C\r\nSEARCHING...\r\n41 0C 1A F8\r\n>")
        assertTrue(result.contains("410C1AF8"))
    }

    @Test
    fun cleanResponse_stripsBufferFull() {
        assertEquals("410D40", OBD2Parser.cleanResponse("BUFFER FULL\r\n410D40\r\n>"))
    }

    @Test
    fun cleanResponse_emptyForNoise() {
        assertEquals("", OBD2Parser.cleanResponse("SEARCHING...\r\nUNABLE TO CONNECT\r\n>"))
    }

    // ================================================================
    //  isError
    // ================================================================

    @Test
    fun isError_searchingIsError() {
        assertTrue(OBD2Parser.isError("SEARCHING..."))
    }

    @Test
    fun isError_noDataIsError() {
        assertTrue(OBD2Parser.isError("NO DATA"))
    }

    @Test
    fun isError_validResponseIsNotError() {
        assertFalse(OBD2Parser.isError("410C1AF8"))
    }

    @Test
    fun isError_unableToConnect() {
        assertTrue(OBD2Parser.isError("UNABLE TO CONNECT"))
    }

    // ================================================================
    //  parseRPM
    // ================================================================

    @Test
    fun parseRPM_idle() {
        // 0x0D48 = 3400, / 4 = 850 RPM (idle)
        val rpm = OBD2Parser.parseRPM("410C0D48")
        assertEquals(850, rpm)
    }

    @Test
    fun parseRPM_highRevs() {
        // 0x1F40 = 8000, / 4 = 2000... let me calc correctly
        // For 7200 RPM: 7200 * 4 = 28800 = 0x7080
        val rpm = OBD2Parser.parseRPM("410C7080")
        assertEquals(7200, rpm)
    }

    @Test
    fun parseRPM_zero() {
        val rpm = OBD2Parser.parseRPM("410C0000")
        assertEquals(0, rpm)
    }

    @Test
    fun parseRPM_redline() {
        // 8000 RPM: 8000 * 4 = 32000 = 0x7D00
        val rpm = OBD2Parser.parseRPM("410C7D00")
        assertEquals(8000, rpm)
    }

    @Test
    fun parseRPM_withSpaces() {
        val rpm = OBD2Parser.parseRPM("41 0C 0D 48")
        assertEquals(850, rpm)
    }

    @Test
    fun parseRPM_withEchoAndPrompt() {
        val rpm = OBD2Parser.parseRPM("010C\r\n41 0C 0D 48\r\n>")
        assertEquals(850, rpm)
    }

    @Test
    fun parseRPM_noData() {
        assertNull(OBD2Parser.parseRPM("NO DATA"))
    }

    @Test
    fun parseRPM_garbage() {
        assertNull(OBD2Parser.parseRPM("XYZABC"))
    }

    // ================================================================
    //  parseSpeed
    // ================================================================

    @Test
    fun parseSpeed_zero() {
        val speed = OBD2Parser.parseSpeed("410D00")
        assertEquals(0, speed)
    }

    @Test
    fun parseSpeed_highway() {
        // 113 km/h * 0.621371 ≈ 70 MPH
        val speed = OBD2Parser.parseSpeed("410D71")
        assertEquals(70, speed)
    }

    @Test
    fun parseSpeed_city() {
        // 48 km/h * 0.621371 ≈ 29 MPH
        val speed = OBD2Parser.parseSpeed("410D30")
        assertEquals(29, speed)
    }

    @Test
    fun parseSpeed_noData() {
        assertNull(OBD2Parser.parseSpeed("NO DATA"))
    }

    // ================================================================
    //  parseCoolant
    // ================================================================

    @Test
    fun parseCoolant_normalOperating() {
        // 90°C = byte value 130 (90+40), 90°C = 194°F
        val temp = OBD2Parser.parseCoolant("410582")
        assertEquals(194, temp)
    }

    @Test
    fun parseCoolant_cold() {
        // 20°C = byte 60 (20+40), 20°C = 68°F
        val temp = OBD2Parser.parseCoolant("41053C")
        assertEquals(68, temp)
    }

    @Test
    fun parseCoolant_overheating() {
        // 110°C = byte 150 (0x96), 110°C = 230°F
        val temp = OBD2Parser.parseCoolant("410596")
        assertEquals(230, temp)
    }

    // ================================================================
    //  parseThrottle
    // ================================================================

    @Test
    fun parseThrottle_closed() {
        val throttle = OBD2Parser.parseThrottle("411100")
        assertEquals(0, throttle)
    }

    @Test
    fun parseThrottle_wideOpen() {
        val throttle = OBD2Parser.parseThrottle("4111FF")
        assertEquals(100, throttle)
    }

    @Test
    fun parseThrottle_half() {
        // 128/255 * 100 ≈ 50
        val throttle = OBD2Parser.parseThrottle("411180")
        assertEquals(50, throttle)
    }

    // ================================================================
    //  parseIAT
    // ================================================================

    @Test
    fun parseIAT_normal() {
        // 30°C = byte 70 (0x46), 30°C = 86°F
        val temp = OBD2Parser.parseIAT("410F46")
        assertEquals(86, temp)
    }

    @Test
    fun parseIAT_hot() {
        // 60°C = byte 100 (0x64), 60°C = 140°F
        val temp = OBD2Parser.parseIAT("410F64")
        assertEquals(140, temp)
    }

    // ================================================================
    //  parseTiming
    // ================================================================

    @Test
    fun parseTiming_advance() {
        // 30° advance: (A/2)-64, A = (30+64)*2 = 188 = 0xBC
        val timing = OBD2Parser.parseTiming("410EBC")
        assertEquals(30f, timing)
    }

    @Test
    fun parseTiming_retard() {
        // -5° retard: A = (-5+64)*2 = 118 = 0x76
        val timing = OBD2Parser.parseTiming("410E76")
        assertEquals(-5f, timing)
    }

    @Test
    fun parseTiming_zero() {
        // 0°: A = 64*2 = 128 = 0x80
        val timing = OBD2Parser.parseTiming("410E80")
        assertEquals(0f, timing)
    }

    // ================================================================
    //  parseSTFT
    // ================================================================

    @Test
    fun parseSTFT_zero() {
        // 0%: A = 128 = 0x80
        val stft = OBD2Parser.parseSTFT("410680")
        assertNotNull(stft)
        assertEquals(0f, stft!!, 0.1f)
    }

    @Test
    fun parseSTFT_positive() {
        // +10%: (A-128)*100/128 = 10, A = 128 + 10*128/100 = 140.8 ≈ 141 = 0x8D
        val stft = OBD2Parser.parseSTFT("41068D")
        assertNotNull(stft)
        assertTrue(stft!! > 5f && stft < 15f)
    }

    @Test
    fun parseSTFT_negative() {
        // -10%: A = 128 - 10*128/100 = 115.2 ≈ 115 = 0x73
        val stft = OBD2Parser.parseSTFT("410673")
        assertNotNull(stft)
        assertTrue(stft!! < -5f && stft > -15f)
    }

    // ================================================================
    //  parseLTFT
    // ================================================================

    @Test
    fun parseLTFT_zero() {
        val ltft = OBD2Parser.parseLTFT("410780")
        assertNotNull(ltft)
        assertEquals(0f, ltft!!, 0.1f)
    }

    // ================================================================
    //  parseMAP
    // ================================================================

    @Test
    fun parseMAP_idle() {
        // ~35 kPa at idle = 0x23
        val map = OBD2Parser.parseMAP("410B23")
        assertEquals(35, map)
    }

    @Test
    fun parseMAP_wot() {
        // ~101 kPa at WOT (NA) = 0x65
        val map = OBD2Parser.parseMAP("410B65")
        assertEquals(101, map)
    }

    @Test
    fun parseMAP_atmosphere() {
        // ~101 kPa = atmospheric
        val map = OBD2Parser.parseMAP("410B65")
        assertEquals(101, map)
    }

    // ================================================================
    //  parseVoltage
    // ================================================================

    @Test
    fun parseVoltage_normal() {
        val v = OBD2Parser.parseVoltage("14.2V\r\n>")
        assertNotNull(v)
        assertEquals(14.2f, v!!, 0.01f)
    }

    @Test
    fun parseVoltage_low() {
        val v = OBD2Parser.parseVoltage("12.1V\r\n>")
        assertNotNull(v)
        assertEquals(12.1f, v!!, 0.01f)
    }

    @Test
    fun parseVoltage_noUnit() {
        val v = OBD2Parser.parseVoltage("13.8\r\n>")
        assertNotNull(v)
        assertEquals(13.8f, v!!, 0.01f)
    }

    // ================================================================
    //  parseO2Voltage
    // ================================================================

    @Test
    fun parseO2Voltage_lean() {
        // 0.1V = 20 = 0x14
        val v = OBD2Parser.parseO2Voltage("411414")
        assertNotNull(v)
        assertEquals(0.1f, v!!, 0.01f)
    }

    @Test
    fun parseO2Voltage_rich() {
        // 0.8V = 160 = 0xA0
        val v = OBD2Parser.parseO2Voltage("4114A0")
        assertNotNull(v)
        assertEquals(0.8f, v!!, 0.01f)
    }

    // ================================================================
    //  parseDTCs
    // ================================================================

    @Test
    fun parseDTCs_noCodes() {
        val codes = OBD2Parser.parseDTCs("430000")
        assertTrue(codes.isEmpty())
    }

    @Test
    fun parseDTCs_singleCode() {
        // P0300 = 0x0300: first byte 0x03, second byte 0x00
        val codes = OBD2Parser.parseDTCs("430300")
        assertEquals(1, codes.size)
        assertEquals("P0300", codes[0])
    }

    @Test
    fun parseDTCs_multipleCodes() {
        // P0300 + P0420
        val codes = OBD2Parser.parseDTCs("4303000420")
        assertEquals(2, codes.size)
        assertEquals("P0300", codes[0])
        assertEquals("P0420", codes[1])
    }

    @Test
    fun parseDTCs_vtecCode() {
        // P2646: P = 00, 2 = 10 (second digit), 6 = 0110, 46 = 0x46
        // First byte: 00|10|0110 = 0x26, Second byte: 0x46
        val codes = OBD2Parser.parseDTCs("432646")
        assertEquals(1, codes.size)
        assertEquals("P2646", codes[0])
    }

    @Test
    fun parseDTCs_withSpaces() {
        val codes = OBD2Parser.parseDTCs("43 03 00 04 20")
        assertEquals(2, codes.size)
    }

    @Test
    fun parseDTCs_noDataResponse() {
        val codes = OBD2Parser.parseDTCs("NO DATA")
        assertTrue(codes.isEmpty())
    }

    @Test
    fun parseDTCs_bodyControlCode() {
        // B1234: B = 10, first byte: 10|01|0010 = 0x92, second byte: 0x34
        val codes = OBD2Parser.parseDTCs("439234")
        assertEquals(1, codes.size)
        assertTrue(codes[0].startsWith("B"))
    }

    @Test
    fun parseDTCs_chassisCode() {
        // C0300: C = 01, first byte: 01|00|0011 = 0x43, second byte: 0x00
        val codes = OBD2Parser.parseDTCs("434300")
        assertEquals(1, codes.size)
        assertTrue(codes[0].startsWith("C"))
    }

    // ================================================================
    //  OBDData
    // ================================================================

    @Test
    fun obdData_defaultValues() {
        val data = OBDData()
        assertEquals(0, data.rpm)
        assertEquals(0, data.speed)
        assertEquals(185, data.coolant)
        assertEquals(75, data.iat)
        assertEquals(12.6f, data.voltage)
        assertEquals(101, data.map)
    }

    @Test
    fun obdData_customValues() {
        val data = OBDData(rpm = 7200, speed = 85, coolant = 205, throttle = 95)
        assertEquals(7200, data.rpm)
        assertEquals(85, data.speed)
        assertEquals(205, data.coolant)
        assertEquals(95, data.throttle)
    }

    // ================================================================
    //  Edge Cases
    // ================================================================

    @Test
    fun parseRPM_maxValue() {
        // 0xFFFF = 65535, /4 = 16383 RPM (unrealistic but valid)
        val rpm = OBD2Parser.parseRPM("410CFFFF")
        assertEquals(16383, rpm)
    }

    @Test
    fun parseCoolant_minValue() {
        // 0°C byte = 40 (0x28), 0°C = 32°F
        val temp = OBD2Parser.parseCoolant("410528")
        assertEquals(32, temp)
    }

    @Test
    fun parseMAP_maxValue() {
        // 255 kPa = 0xFF
        val map = OBD2Parser.parseMAP("410BFF")
        assertEquals(255, map)
    }

    @Test
    fun cleanResponse_handlesEmpty() {
        assertEquals("", OBD2Parser.cleanResponse(""))
    }

    @Test
    fun cleanResponse_handlesOnlyPrompt() {
        assertEquals("", OBD2Parser.cleanResponse(">"))
    }
}
