package ghost.extractor;

import java.security.SecureRandom;
import java.util.zip.CRC32;

/**
 * S-Box codec for the extractor module.
 * 
 * This is an independent copy of the encoding/decoding logic to avoid
 * cross-module dependencies. The format must match ghost.injector.SBoxEncoder.
 * 
 * Encoding layout:
 * Index 0: Random int (validation component 1)
 * Index 1: CRC32 of payload
 * Index 2-N: Payload bytes packed into ints (big-endian)
 * Index N-last: Random noise
 * Last index: sbox[0] ^ payloadLength
 */
class SBoxEncoder {

    /** Table sizes that mimic real crypto implementations */
    static final int SIZE_128 = 128;
    static final int SIZE_192 = 192;
    static final int SIZE_256 = 256;

    /**
     * Decodes a payload from an S-Box table.
     * 
     * @param sbox the encoded S-Box table
     * @return decoded payload bytes, or null if validation fails
     */
    byte[] decode(int[] sbox) {
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
            return null; // CRC mismatch
        }

        return payload;
    }

    /**
     * Checks if a given size is a valid S-Box table size.
     */
    boolean isValidTableSize(int size) {
        return size == SIZE_128 || size == SIZE_192 || size == SIZE_256;
    }
}
