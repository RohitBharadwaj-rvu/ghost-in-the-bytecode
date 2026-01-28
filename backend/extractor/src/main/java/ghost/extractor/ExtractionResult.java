package ghost.extractor;

/**
 * Result of a payload extraction attempt.
 * Provides detailed error information when extraction fails.
 */
public record ExtractionResult(
        boolean success,
        byte[] payload,
        ErrorType errorType,
        String errorMessage) {
    /**
     * Types of extraction errors.
     */
    public enum ErrorType {
        /** No error - extraction successful */
        NONE,
        /** GhostPayload attribute not found in class */
        NO_PAYLOAD,
        /** Magic header does not match expected value */
        INVALID_MAGIC,
        /** Payload length is invalid (negative or exceeds data) */
        INVALID_LENGTH,
        /** Data is corrupted or truncated */
        CORRUPTED
    }

    /**
     * Creates a successful extraction result.
     */
    public static ExtractionResult success(byte[] payload) {
        return new ExtractionResult(true, payload, ErrorType.NONE, null);
    }

    /**
     * Creates a failed result with the specified error.
     */
    public static ExtractionResult failure(ErrorType errorType, String message) {
        return new ExtractionResult(false, null, errorType, message);
    }

    /**
     * Creates a NO_PAYLOAD error result.
     */
    public static ExtractionResult noPayload() {
        return failure(ErrorType.NO_PAYLOAD, "GhostPayload attribute not found");
    }

    /**
     * Creates an INVALID_MAGIC error result.
     */
    public static ExtractionResult invalidMagic(int found, int expected) {
        return failure(ErrorType.INVALID_MAGIC,
                String.format("Invalid magic: expected 0x%08X, found 0x%08X", expected, found));
    }

    /**
     * Creates an INVALID_LENGTH error result.
     */
    public static ExtractionResult invalidLength(int length, int available) {
        return failure(ErrorType.INVALID_LENGTH,
                String.format("Invalid length: %d (available: %d)", length, available));
    }

    /**
     * Creates a CORRUPTED error result.
     */
    public static ExtractionResult corrupted(String details) {
        return failure(ErrorType.CORRUPTED, "Data corrupted: " + details);
    }
}
