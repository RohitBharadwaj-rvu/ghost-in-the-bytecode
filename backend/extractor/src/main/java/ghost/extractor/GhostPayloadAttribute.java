package ghost.extractor;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * Independent copy of GhostPayload attribute for the extractor module.
 * 
 * This class duplicates the format from the injector module intentionally
 * to maintain module independence (no runtime dependency on injector).
 * 
 * Format:
 * - Magic: 4 bytes (0x47504801 = "GPH" + version 1)
 * - Length: 4 bytes
 * - Data: N bytes
 */
public class GhostPayloadAttribute extends Attribute {

    /** Magic number: "GPH" (Ghost Payload Header) + version 1 */
    public static final int GHOST_MAGIC = 0x47504801;

    /** Attribute name in class file */
    public static final String ATTRIBUTE_NAME = "GhostPayload";

    /** Header size: magic (4) + length (4) */
    public static final int HEADER_SIZE = 8;

    /** Extracted payload data */
    private final byte[] data;

    /** Validation result from read operation */
    private ExtractionResult validationResult;

    /**
     * Creates an empty prototype for ClassReader registration.
     */
    public GhostPayloadAttribute() {
        super(ATTRIBUTE_NAME);
        this.data = null;
        this.validationResult = null;
    }

    /**
     * Creates an attribute with parsed data and validation result.
     */
    private GhostPayloadAttribute(byte[] data, ExtractionResult validationResult) {
        super(ATTRIBUTE_NAME);
        this.data = data;
        this.validationResult = validationResult;
    }

    /**
     * Returns the extracted payload data, or null if extraction failed.
     */
    public byte[] getData() {
        return data != null ? data.clone() : null;
    }

    /**
     * Returns the validation result from the read operation.
     */
    public ExtractionResult getValidationResult() {
        return validationResult;
    }

    /**
     * Deserializes this attribute from bytes read from a class file.
     * Performs validation and captures any errors in the result.
     */
    @Override
    protected Attribute read(
            ClassReader classReader,
            int offset,
            int length,
            char[] charBuffer,
            int codeAttributeOffset,
            Label[] labels) {
        // Check minimum header size
        if (length < HEADER_SIZE) {
            return new GhostPayloadAttribute(null,
                    ExtractionResult.corrupted("Attribute too short: " + length + " bytes"));
        }

        try {
            // Read magic number (4 bytes, big-endian)
            int magic = ((classReader.b[offset] & 0xFF) << 24)
                    | ((classReader.b[offset + 1] & 0xFF) << 16)
                    | ((classReader.b[offset + 2] & 0xFF) << 8)
                    | (classReader.b[offset + 3] & 0xFF);

            // Validate magic
            if (magic != GHOST_MAGIC) {
                return new GhostPayloadAttribute(null,
                        ExtractionResult.invalidMagic(magic, GHOST_MAGIC));
            }

            // Read data length (4 bytes, big-endian)
            int dataLength = ((classReader.b[offset + 4] & 0xFF) << 24)
                    | ((classReader.b[offset + 5] & 0xFF) << 16)
                    | ((classReader.b[offset + 6] & 0xFF) << 8)
                    | (classReader.b[offset + 7] & 0xFF);

            // Validate length - negative check
            if (dataLength < 0) {
                return new GhostPayloadAttribute(null,
                        ExtractionResult.invalidLength(dataLength, length - HEADER_SIZE));
            }

            // Validate length - bounds check
            int availableBytes = length - HEADER_SIZE;
            if (dataLength > availableBytes) {
                return new GhostPayloadAttribute(null,
                        ExtractionResult.invalidLength(dataLength, availableBytes));
            }

            // Extract payload data
            byte[] payloadData = new byte[dataLength];
            System.arraycopy(classReader.b, offset + HEADER_SIZE, payloadData, 0, dataLength);

            return new GhostPayloadAttribute(payloadData, ExtractionResult.success(payloadData));

        } catch (Exception e) {
            return new GhostPayloadAttribute(null,
                    ExtractionResult.corrupted(e.getMessage()));
        }
    }

    /**
     * Write is not needed for extraction-only module.
     * Throws if called (indicates incorrect usage).
     */
    @Override
    protected ByteVector write(
            ClassWriter classWriter,
            byte[] code,
            int codeLength,
            int maxStack,
            int maxLocals) {
        throw new UnsupportedOperationException(
                "Extractor module does not support writing attributes");
    }

    @Override
    public boolean isUnknown() {
        return true;
    }
}
