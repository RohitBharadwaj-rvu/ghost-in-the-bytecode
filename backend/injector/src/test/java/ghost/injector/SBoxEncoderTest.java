package ghost.injector;

import ghost.validator.BytecodeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for S-Box Smearing functionality.
 * These tests focus on the injector module only.
 */
class SBoxEncoderTest {

    private SBoxEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new SBoxEncoder();
    }

    // ==================== Encoding Tests ====================

    @Test
    @DisplayName("SBoxEncode: Implicit XOR signature validates correctly")
    void testSBoxEncode_ImplicitSignature() {
        byte[] payload = "Test data".getBytes(StandardCharsets.UTF_8);
        int[] sbox = encoder.encode(payload);

        // Verify XOR signature: sbox[0] ^ sbox[last] == payloadLength
        int computedLength = sbox[0] ^ sbox[sbox.length - 1];
        assertEquals(payload.length, computedLength,
                "XOR signature should encode payload length");
    }

    @Test
    @DisplayName("SBoxDecode: Round-trip encode→decode preserves payload exactly")
    void testSBoxDecode_RoundTrip() {
        byte[] payload = "Hello S-Box World!".getBytes(StandardCharsets.UTF_8);
        int[] sbox = encoder.encode(payload);
        byte[] decoded = encoder.decode(sbox);

        assertNotNull(decoded, "Decoded payload should not be null");
        assertArrayEquals(payload, decoded, "Decoded payload should match original");
    }

    @Test
    @DisplayName("SBoxEncode: Payload ≤500 bytes uses 128-entry table")
    void testSBoxEncode_Size128() {
        byte[] smallPayload = new byte[100];
        Arrays.fill(smallPayload, (byte) 0x42);

        int[] sbox = encoder.encode(smallPayload);
        assertEquals(SBoxEncoder.SIZE_128, sbox.length,
                "Small payload should use 128-entry table");
    }

    @Test
    @DisplayName("SBoxEncode: Payload 501-752 bytes uses 192-entry table")
    void testSBoxEncode_Size192() {
        byte[] mediumPayload = new byte[600];
        Arrays.fill(mediumPayload, (byte) 0x42);

        int[] sbox = encoder.encode(mediumPayload);
        assertEquals(SBoxEncoder.SIZE_192, sbox.length,
                "Medium payload should use 192-entry table");
    }

    @Test
    @DisplayName("SBoxEncode: Payload >752 bytes uses 256-entry table")
    void testSBoxEncode_Size256() {
        byte[] largePayload = new byte[800];
        Arrays.fill(largePayload, (byte) 0x42);

        int[] sbox = encoder.encode(largePayload);
        assertEquals(SBoxEncoder.SIZE_256, sbox.length,
                "Large payload should use 256-entry table");
    }

    @Test
    @DisplayName("SBoxEncode: Two encodes of same payload produce different noise")
    void testSBoxEncode_RandomNoiseDiffers() {
        byte[] payload = "Same payload".getBytes(StandardCharsets.UTF_8);

        int[] sbox1 = encoder.encode(payload);
        int[] sbox2 = encoder.encode(payload);

        // sbox[0] should differ (random validation component)
        assertNotEquals(sbox1[0], sbox2[0],
                "Random components should differ between encodes");
    }

    @Test
    @DisplayName("SBoxEncode: All byte values (0x00-0xFF) are preserved exactly")
    void testSBoxEncode_AllByteValues() {
        byte[] payload = new byte[256];
        for (int i = 0; i < 256; i++) {
            payload[i] = (byte) i;
        }

        int[] sbox = encoder.encode(payload);
        byte[] decoded = encoder.decode(sbox);

        assertArrayEquals(payload, decoded, "All byte values should be preserved");
    }

    @Test
    @DisplayName("SBoxDecode: Corrupted XOR validation fails extraction")
    void testSBoxDecode_CorruptedXor_Fails() {
        byte[] payload = "Valid payload".getBytes(StandardCharsets.UTF_8);
        int[] sbox = encoder.encode(payload);

        // Corrupt the last element to break XOR validation
        sbox[sbox.length - 1] ^= 0x12345678;

        byte[] decoded = encoder.decode(sbox);
        assertNull(decoded, "Corrupted S-Box should fail decoding");
    }

    @Test
    @DisplayName("SBoxDecode: Corrupted CRC fails extraction")
    void testSBoxDecode_CorruptedCrc_Fails() {
        byte[] payload = "Valid payload".getBytes(StandardCharsets.UTF_8);
        int[] sbox = encoder.encode(payload);

        // Corrupt the CRC field (index 1)
        sbox[1] ^= 0xFFFF;

        byte[] decoded = encoder.decode(sbox);
        assertNull(decoded, "Corrupted CRC should fail decoding");
    }

    @Test
    @DisplayName("SBoxDecode: Invalid table size returns null")
    void testSBoxDecode_InvalidSize_Fails() {
        int[] badSize = new int[100]; // Not 128, 192, or 256
        byte[] decoded = encoder.decode(badSize);
        assertNull(decoded, "Invalid table size should return null");
    }

    // ==================== Stress Tests ====================

    @Test
    @DisplayName("Stress: 100 roundtrip iterations with random payloads")
    void testSBoxRoundtrip_100Iterations() {
        java.util.Random random = new java.util.Random(42);

        for (int i = 0; i < 100; i++) {
            int size = random.nextInt(800) + 1; // 1-800 bytes
            byte[] payload = new byte[size];
            random.nextBytes(payload);

            int[] sbox = encoder.encode(payload);
            byte[] decoded = encoder.decode(sbox);

            assertNotNull(decoded, "Iteration " + i + ": decode should not return null");
            assertArrayEquals(payload, decoded, "Iteration " + i + ": payload mismatch");
        }
    }
}
