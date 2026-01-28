package ghost.injector;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * Custom class-level attribute for storing encrypted payload in .class files.
 * 
 * The JVM ignores unknown attributes, making this safe for storing arbitrary data.
 * 
 * Payload format:
 * - Magic: 4 bytes (0x47504801 = "GPH" + version 1)
 * - Length: 4 bytes
 * - Data: N bytes
 */
public class GhostPayloadAttribute extends Attribute {

    /** Magic number: "GPH" (Ghost Payload Header) + version 1 */
    public static final int MAGIC = 0x47504801;

    /** The attribute name used in the class file */
    public static final String ATTRIBUTE_NAME = "GhostPayload";

    /** The raw payload data (without header) */
    private final byte[] data;

    /**
     * Creates a new GhostPayloadAttribute with the given data.
     *
     * @param data the payload data to embed
     */
    public GhostPayloadAttribute(byte[] data) {
        super(ATTRIBUTE_NAME);
        this.data = data != null ? data.clone() : new byte[0];
    }

    /**
     * Creates an empty GhostPayloadAttribute for use as a prototype in ClassReader.
     */
    public GhostPayloadAttribute() {
        this(new byte[0]);
    }

    /**
     * Returns a copy of the payload data.
     *
     * @return the payload data
     */
    public byte[] getData() {
        return data.clone();
    }

    /**
     * Serializes this attribute to bytes for inclusion in a class file.
     * Format: [MAGIC:4][LENGTH:4][DATA:N]
     */
    @Override
    protected ByteVector write(
            ClassWriter classWriter,
            byte[] code,
            int codeLength,
            int maxStack,
            int maxLocals
    ) {
        ByteVector bv = new ByteVector();
        // Write magic number (4 bytes, big-endian)
        bv.putInt(MAGIC);
        // Write data length (4 bytes)
        bv.putInt(data.length);
        // Write payload data
        bv.putByteArray(data, 0, data.length);
        return bv;
    }

    /**
     * Deserializes this attribute from bytes read from a class file.
     * This method is called by ClassReader when it encounters this attribute.
     */
    @Override
    protected Attribute read(
            ClassReader classReader,
            int offset,
            int length,
            char[] charBuffer,
            int codeAttributeOffset,
            Label[] labels
    ) {
        // Validate minimum length (magic + length = 8 bytes)
        if (length < 8) {
            throw new IllegalArgumentException("Invalid GhostPayload attribute: too short");
        }

        // Read magic number (4 bytes, big-endian)
        int magic = ((classReader.b[offset] & 0xFF) << 24)
                  | ((classReader.b[offset + 1] & 0xFF) << 16)
                  | ((classReader.b[offset + 2] & 0xFF) << 8)
                  | (classReader.b[offset + 3] & 0xFF);

        if (magic != MAGIC) {
            throw new IllegalArgumentException(
                String.format("Invalid GhostPayload magic: expected 0x%08X, got 0x%08X", MAGIC, magic)
            );
        }

        // Read data length (4 bytes)
        int dataLength = ((classReader.b[offset + 4] & 0xFF) << 24)
                       | ((classReader.b[offset + 5] & 0xFF) << 16)
                       | ((classReader.b[offset + 6] & 0xFF) << 8)
                       | (classReader.b[offset + 7] & 0xFF);

        // Validate data length
        if (dataLength < 0 || dataLength > length - 8) {
            throw new IllegalArgumentException("Invalid GhostPayload data length: " + dataLength);
        }

        // Extract payload data
        byte[] payloadData = new byte[dataLength];
        System.arraycopy(classReader.b, offset + 8, payloadData, 0, dataLength);

        return new GhostPayloadAttribute(payloadData);
    }

    /**
     * Returns whether this attribute is unknown to ASM.
     * Custom attributes are always "unknown" to the standard JVM.
     */
    @Override
    public boolean isUnknown() {
        return true;
    }
}
