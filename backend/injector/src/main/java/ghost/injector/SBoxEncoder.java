package ghost.injector;

import java.security.SecureRandom;
import java.util.zip.CRC32;

/**
 * Encodes arbitrary payload bytes into an int[] that mimics a cryptographic
 * S-Box table.
 * 
 * v2 Features:
 * - Implicit signature: sbox[0] ^ sbox[last] == payloadLength (no plaintext
 * magic)
 * - Variable table sizes: 128/192/256 based on payload size
 * - Cryptographically random noise padding
 * - CRC32 checksum for extraction validation
 * 
 * Encoding layout:
 * Index 0: Random int (validation component 1)
 * Index 1: CRC32 of payload (for extraction validation)
 * Index 2-N: Payload bytes packed into ints (4 bytes per int, big-endian)
 * Index N-last: Random noise
 * Last index: Random int where (sbox[0] ^ sbox[last]) == payloadLength
 */
public class SBoxEncoder {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Table sizes that mimic real crypto implementations */
    public static final int SIZE_128 = 128; // AES T-tables
    public static final int SIZE_192 = 192; // Common crypto
    public static final int SIZE_256 = 256; // Full S-Box

    /**
     * Maximum payload bytes per table size (reserve 2 ints for header, 1 for
     * validation)
     */
    private static final int MAX_PAYLOAD_SIZE_128 = (SIZE_128 - 3) * 4; // 500 bytes
    private static final int MAX_PAYLOAD_SIZE_192 = (SIZE_192 - 3) * 4; // 756 bytes

    /**
     * Encodes a payload into a fake S-Box table.
     * 
     * @param payload the raw payload bytes
     * @return int[] mimicking a crypto lookup table
     * @throws IllegalArgumentException if payload exceeds max capacity (~1012
     *                                  bytes)
     */
    public int[] encode(byte[] payload) {
        if (payload == null) {
            payload = new byte[0];
        }

        int tableSize = selectTableSize(payload.length);
        int maxPayload = (tableSize - 3) * 4;

        if (payload.length > maxPayload) {
            throw new IllegalArgumentException(
                    "Payload too large: " + payload.length + " bytes (max " + maxPayload + " for " + tableSize
                            + "-entry table)");
        }

        int[] sbox = new int[tableSize];

        // Index 0: Random validation component
        sbox[0] = RANDOM.nextInt();

        // Index 1: CRC32 checksum of payload
        CRC32 crc = new CRC32();
        crc.update(payload);
        sbox[1] = (int) crc.getValue();

        // Index 2 onwards: Payload bytes packed into ints (big-endian)
        int payloadInts = (payload.length + 3) / 4; // Round up
        for (int i = 0; i < payloadInts; i++) {
            int value = 0;
            for (int b = 0; b < 4; b++) {
                int byteIndex = i * 4 + b;
                if (byteIndex < payload.length) {
                    value |= (payload[byteIndex] & 0xFF) << (24 - b * 8);
                }
            }
            sbox[2 + i] = value;
        }

        // Fill remaining slots with random noise (except last)
        for (int i = 2 + payloadInts; i < tableSize - 1; i++) {
            sbox[i] = RANDOM.nextInt();
        }

        // Last index: Computed so that sbox[0] ^ sbox[last] == payloadLength
        sbox[tableSize - 1] = sbox[0] ^ payload.length;

        return sbox;
    }

    /**
     * Decodes a payload from an S-Box table.
     * 
     * @param sbox the encoded S-Box table
     * @return decoded payload bytes, or null if validation fails
     */
    public byte[] decode(int[] sbox) {
        if (sbox == null || !isValidTableSize(sbox.length)) {
            return null;
        }

        // Extract length from implicit signature
        int payloadLength = sbox[0] ^ sbox[sbox.length - 1];

        // Sanity check: length must be non-negative and fit in the table
        int maxPayload = (sbox.length - 3) * 4;
        if (payloadLength < 0 || payloadLength > maxPayload) {
            return null;
        }

        // Extract payload bytes
        byte[] payload = new byte[payloadLength];
        int payloadInts = (payloadLength + 3) / 4;

        for (int i = 0; i < payloadInts; i++) {
            int value = sbox[2 + i];
            for (int b = 0; b < 4; b++) {
                int byteIndex = i * 4 + b;
                if (byteIndex < payloadLength) {
                    payload[byteIndex] = (byte) ((value >> (24 - b * 8)) & 0xFF);
                }
            }
        }

        // Validate CRC32
        CRC32 crc = new CRC32();
        crc.update(payload);
        if ((int) crc.getValue() != sbox[1]) {
            return null; // CRC mismatch - corrupted or not our payload
        }

        return payload;
    }

    /**
     * Selects table size based on payload length.
     * Smaller payloads use smaller tables to reduce overhead.
     */
    public int selectTableSize(int payloadLength) {
        if (payloadLength <= MAX_PAYLOAD_SIZE_128) {
            return SIZE_128;
        } else if (payloadLength <= MAX_PAYLOAD_SIZE_192) {
            return SIZE_192;
        } else {
            return SIZE_256;
        }
    }

    /**
     * Checks if a given size is a valid S-Box table size.
     */
    public boolean isValidTableSize(int size) {
        return size == SIZE_128 || size == SIZE_192 || size == SIZE_256;
    }

    /**
     * Returns max payload capacity for a given table size.
     */
    public int maxPayloadSize(int tableSize) {
        return (tableSize - 3) * 4;
    }
}
