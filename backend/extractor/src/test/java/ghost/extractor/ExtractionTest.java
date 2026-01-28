package ghost.extractor;

import ghost.injector.GhostPayloadInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Sprint 2: Payload Extraction
 */
class ExtractionTest {

    private PayloadExtractor extractor;
    private GhostPayloadInjector injector;
    private byte[] testClassBytes;

    @BeforeEach
    void setUp() {
        extractor = new PayloadExtractor();
        injector = new GhostPayloadInjector();
        testClassBytes = createMinimalTestClass();
    }

    /**
     * Creates a minimal test class using ASM.
     */
    private byte[] createMinimalTestClass() {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(
                org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                org.objectweb.asm.Opcodes.V17,
                org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "TestClass",
                null,
                "java/lang/Object",
                null);
        var mv = cw.visitMethod(
                org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                org.objectweb.asm.Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    // ==================== Success Cases ====================

    @Test
    void testExtract_ValidPayload_Success() {
        // Arrange
        byte[] payload = "Test payload data".getBytes(StandardCharsets.UTF_8);
        byte[] injectedClass = injector.inject(testClassBytes, payload);

        // Act
        ExtractionResult result = extractor.extract(injectedClass);

        // Assert
        assertTrue(result.success(), "Extraction should succeed");
        assertEquals(ExtractionResult.ErrorType.NONE, result.errorType());
        assertArrayEquals(payload, result.payload());
        assertNull(result.errorMessage());
    }

    @Test
    void testExtract_LargePayload_Success() {
        // Arrange - 1MB payload
        byte[] largePayload = new byte[1024 * 1024];
        Arrays.fill(largePayload, (byte) 0xAB);
        byte[] injectedClass = injector.inject(testClassBytes, largePayload);

        // Act
        ExtractionResult result = extractor.extract(injectedClass);

        // Assert
        assertTrue(result.success());
        assertArrayEquals(largePayload, result.payload());
    }

    @Test
    void testExtract_EmptyPayload_Success() {
        // Arrange
        byte[] emptyPayload = new byte[0];
        byte[] injectedClass = injector.inject(testClassBytes, emptyPayload);

        // Act
        ExtractionResult result = extractor.extract(injectedClass);

        // Assert
        assertTrue(result.success());
        assertEquals(0, result.payload().length);
    }

    @Test
    void testExtract_BinaryPayload_PreservedExactly() {
        // Arrange - all byte values
        byte[] binaryPayload = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryPayload[i] = (byte) i;
        }
        byte[] injectedClass = injector.inject(testClassBytes, binaryPayload);

        // Act
        ExtractionResult result = extractor.extract(injectedClass);

        // Assert
        assertTrue(result.success());
        assertArrayEquals(binaryPayload, result.payload());
    }

    // ==================== No Payload Cases ====================

    @Test
    void testExtract_NoPayloadAttribute_ReturnsNoPayload() {
        // Act - extract from class without injection
        ExtractionResult result = extractor.extract(testClassBytes);

        // Assert
        assertFalse(result.success());
        assertEquals(ExtractionResult.ErrorType.NO_PAYLOAD, result.errorType());
        assertNull(result.payload());
        assertNotNull(result.errorMessage());
    }

    // ==================== Error Cases ====================

    @Test
    void testExtract_NullBytes_ReturnsCorrupted() {
        // Act
        ExtractionResult result = extractor.extract(null);

        // Assert
        assertFalse(result.success());
        assertEquals(ExtractionResult.ErrorType.CORRUPTED, result.errorType());
    }

    @Test
    void testExtract_EmptyBytes_ReturnsCorrupted() {
        // Act
        ExtractionResult result = extractor.extract(new byte[0]);

        // Assert
        assertFalse(result.success());
        assertEquals(ExtractionResult.ErrorType.CORRUPTED, result.errorType());
    }

    // ==================== Roundtrip Test ====================

    @Test
    void testRoundtrip_InjectThenExtract_ByteEquality() {
        // Arrange
        String[] testPayloads = {
                "Simple text",
                "Unicode: 日本語 🎉",
                "",
                new String(new byte[1000], StandardCharsets.UTF_8)
        };

        for (String payloadStr : testPayloads) {
            byte[] payload = payloadStr.getBytes(StandardCharsets.UTF_8);
            byte[] injectedClass = injector.inject(testClassBytes, payload);

            // Act
            ExtractionResult result = extractor.extract(injectedClass);

            // Assert
            assertTrue(result.success(), "Roundtrip failed for: " + payloadStr);
            assertArrayEquals(payload, result.payload(),
                    "Payload mismatch for: " + payloadStr);
        }
    }

    // ==================== Corrupted Magic Test ====================

    @Test
    void testExtract_CorruptedMagic_ReturnsInvalidMagic() {
        // Arrange - inject valid payload
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);
        byte[] injectedClass = injector.inject(testClassBytes, payload);

        // Corrupt the magic bytes (find GhostPayload attribute and modify magic)
        // The attribute is near the end of the class file
        byte[] corruptedClass = corruptMagicInClass(injectedClass);

        // Act
        ExtractionResult result = extractor.extract(corruptedClass);

        // Assert
        assertFalse(result.success());
        assertEquals(ExtractionResult.ErrorType.INVALID_MAGIC, result.errorType());
    }

    /**
     * Corrupts the magic bytes in a class file with GhostPayload attribute.
     */
    private byte[] corruptMagicInClass(byte[] classBytes) {
        byte[] corrupted = classBytes.clone();

        // Find "GhostPayload" string in constant pool, then locate attribute data
        // The magic (0x47504801) appears right after the attribute length
        byte[] magicBytes = { 0x47, 0x50, 0x48, 0x01 };

        for (int i = 0; i < corrupted.length - 4; i++) {
            if (corrupted[i] == magicBytes[0] &&
                    corrupted[i + 1] == magicBytes[1] &&
                    corrupted[i + 2] == magicBytes[2] &&
                    corrupted[i + 3] == magicBytes[3]) {
                // Corrupt the magic
                corrupted[i] = 0x00;
                corrupted[i + 1] = 0x00;
                break;
            }
        }
        return corrupted;
    }
}
