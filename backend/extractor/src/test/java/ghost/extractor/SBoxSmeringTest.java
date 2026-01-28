package ghost.extractor;

import ghost.injector.GhostPayloadInjector;
import ghost.injector.InjectionMode;
import ghost.injector.SBoxEncoder;
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
 * Comprehensive tests for S-Box Smearing functionality.
 */
class SBoxSmeringTest {

    private GhostPayloadInjector injector;
    private PayloadExtractor extractor;
    private SBoxEncoder encoder;
    private BytecodeValidator validator;
    private byte[] testClassBytes;

    @BeforeEach
    void setUp() {
        injector = new GhostPayloadInjector();
        extractor = new PayloadExtractor();
        encoder = new SBoxEncoder();
        validator = new BytecodeValidator();
        testClassBytes = createMinimalTestClass();
    }

    // ==================== Core Functionality Tests ====================

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
    void testSBoxDecode_ImplicitSignature() {
        byte[] payload = "Hello S-Box World!".getBytes(StandardCharsets.UTF_8);
        int[] sbox = encoder.encode(payload);
        byte[] decoded = encoder.decode(sbox);

        assertNotNull(decoded, "Decoded payload should not be null");
        assertArrayEquals(payload, decoded, "Decoded payload should match original");
    }

    @Test
    @DisplayName("SBoxEncode: Payload ≤500 bytes uses 128-entry table")
    void testSBoxEncode_Size128() {
        byte[] smallPayload = new byte[100]; // Well under 500
        Arrays.fill(smallPayload, (byte) 0x42);

        int[] sbox = encoder.encode(smallPayload);
        assertEquals(SBoxEncoder.SIZE_128, sbox.length,
                "Small payload should use 128-entry table");
    }

    @Test
    @DisplayName("SBoxEncode: Payload 501-752 bytes uses 192-entry table")
    void testSBoxEncode_Size192() {
        byte[] mediumPayload = new byte[600]; // Between 500 and 756
        Arrays.fill(mediumPayload, (byte) 0x42);

        int[] sbox = encoder.encode(mediumPayload);
        assertEquals(SBoxEncoder.SIZE_192, sbox.length,
                "Medium payload should use 192-entry table");
    }

    @Test
    @DisplayName("SBoxEncode: Payload >752 bytes uses 256-entry table")
    void testSBoxEncode_Size256() {
        byte[] largePayload = new byte[800]; // Over 756
        Arrays.fill(largePayload, (byte) 0x42);

        int[] sbox = encoder.encode(largePayload);
        assertEquals(SBoxEncoder.SIZE_256, sbox.length,
                "Large payload should use 256-entry table");
    }

    // ==================== Injection Tests ====================

    @Test
    @DisplayName("SBoxInject: Field names follow contextual naming pattern")
    void testSBoxInject_FieldsHaveContextualNames() {
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);
        byte[] modified = injector.inject(testClassBytes, payload, InjectionMode.SBOX_SMEAR);

        // Parse modified class and check field names
        org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(modified);
        FieldNameCollector collector = new FieldNameCollector();
        reader.accept(collector, 0);

        // Should have a field matching _[TS]\d pattern and _[a-z]k pattern
        boolean hasTableField = collector.fieldNames.stream()
                .anyMatch(name -> name.matches("_[TS]\\d"));
        boolean hasCheckField = collector.fieldNames.stream()
                .anyMatch(name -> name.matches("_[a-z]k"));

        assertTrue(hasTableField, "Should have contextual table field (_T0-_T9 or _S0-_S9)");
        assertTrue(hasCheckField, "Should have contextual check field (_ak-_zk)");
    }

    @Test
    @DisplayName("SBoxInject: Modified class is structurally valid")
    void testSBoxInject_ClassStructurallyValid() {
        byte[] payload = "Validate this structure!".getBytes(StandardCharsets.UTF_8);
        byte[] modified = injector.inject(testClassBytes, payload, InjectionMode.SBOX_SMEAR);

        BytecodeValidator.ValidationResult result = validator.validateStructure(modified);
        assertTrue(result.structurallyValid(),
                "Modified class should be structurally valid: " + result.structuralErrors());
    }

    @Test
    @DisplayName("SBoxInject: Modified class can be loaded")
    void testSBoxInject_ClassLoadsSuccessfully() {
        byte[] payload = "Loadable payload".getBytes(StandardCharsets.UTF_8);
        byte[] modified = injector.inject(testClassBytes, payload, InjectionMode.SBOX_SMEAR);

        // Use BytecodeValidator's full validation which includes class loading
        BytecodeValidator.ValidationResult result = validator.validateFull(modified, "TestClass");
        assertTrue(result.structurallyValid(),
                "Class should pass full validation: " + result.structuralErrors());
    }

    // ==================== Extraction Tests ====================

    @Test
    @DisplayName("SBoxExtract: Roundtrip with small payload (50 bytes)")
    void testSBoxExtract_Roundtrip_SmallPayload() {
        byte[] payload = new byte[50];
        Arrays.fill(payload, (byte) 0xAB);

        byte[] modified = injector.inject(testClassBytes, payload, InjectionMode.SBOX_SMEAR);
        ExtractionResult result = extractor.extract(modified, PayloadExtractor.ExtractionMode.SBOX);

        assertTrue(result.success(), "Extraction should succeed: " + result.errorMessage());
        assertArrayEquals(payload, result.payload(), "Extracted payload should match original");
    }

    @Test
    @DisplayName("SBoxExtract: Roundtrip with medium payload (600 bytes)")
    void testSBoxExtract_Roundtrip_MediumPayload() {
        byte[] payload = new byte[600];
        Arrays.fill(payload, (byte) 0xCD);

        byte[] modified = injector.inject(testClassBytes, payload, InjectionMode.SBOX_SMEAR);
        ExtractionResult result = extractor.extract(modified, PayloadExtractor.ExtractionMode.SBOX);

        assertTrue(result.success(), "Extraction should succeed");
        assertArrayEquals(payload, result.payload(), "Extracted payload should match original");
    }

    @Test
    @DisplayName("SBoxExtract: Roundtrip with large payload (900 bytes)")
    void testSBoxExtract_Roundtrip_LargePayload() {
        byte[] payload = new byte[900];
        Arrays.fill(payload, (byte) 0xEF);

        byte[] modified = injector.inject(testClassBytes, payload, InjectionMode.SBOX_SMEAR);
        ExtractionResult result = extractor.extract(modified, PayloadExtractor.ExtractionMode.SBOX);

        assertTrue(result.success(), "Extraction should succeed");
        assertArrayEquals(payload, result.payload(), "Extracted payload should match original");
    }

    @Test
    @DisplayName("SBoxExtract: Clean class without S-Box returns NO_PAYLOAD")
    void testSBoxExtract_NoSBoxField_ReturnsNoPayload() {
        ExtractionResult result = extractor.extract(testClassBytes, PayloadExtractor.ExtractionMode.SBOX);

        assertFalse(result.success(), "Should not succeed on clean class");
        assertEquals(ExtractionResult.ErrorType.NO_PAYLOAD, result.errorType());
    }

    // ==================== Security/Edge Case Tests ====================

    @Test
    @DisplayName("SBoxEncode: Two encodes of same payload produce different noise")
    void testSBoxEncode_RandomNoiseDiffers() {
        byte[] payload = "Same payload".getBytes(StandardCharsets.UTF_8);

        int[] sbox1 = encoder.encode(payload);
        int[] sbox2 = encoder.encode(payload);

        // The actual payload portions should be the same, but random noise differs
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
    @DisplayName("SBoxExtract: Corrupted XOR validation fails extraction")
    void testSBoxExtract_CorruptedXorValidation_Fails() {
        byte[] payload = "Valid payload".getBytes(StandardCharsets.UTF_8);
        int[] sbox = encoder.encode(payload);

        // Corrupt the last element to break XOR validation
        sbox[sbox.length - 1] ^= 0x12345678;

        byte[] decoded = encoder.decode(sbox);
        assertNull(decoded, "Corrupted S-Box should fail decoding");
    }

    @Test
    @DisplayName("SBoxInject: Different classes get different field names")
    void testSBoxInject_TwoDifferentClasses_DifferentFieldNames() {
        // Create two classes with different names
        byte[] class1 = createTestClass("Alpha");
        byte[] class2 = createTestClass("Beta");
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);

        byte[] modified1 = injector.inject(class1, payload, InjectionMode.SBOX_SMEAR);
        byte[] modified2 = injector.inject(class2, payload, InjectionMode.SBOX_SMEAR);

        // Get field names from each
        Set<String> fields1 = getFieldNames(modified1);
        Set<String> fields2 = getFieldNames(modified2);

        // At least one field name should differ (due to hash-based naming)
        assertNotEquals(fields1, fields2,
                "Different classes should have different contextual field names");
    }

    // ==================== Stress Tests ====================

    @Test
    @DisplayName("Stress: 100 roundtrip iterations with random payloads")
    void testSBoxRoundtrip_100Iterations() {
        java.util.Random random = new java.util.Random(42); // Deterministic for reproducibility

        for (int i = 0; i < 100; i++) {
            int size = random.nextInt(800) + 1; // 1-800 bytes
            byte[] payload = new byte[size];
            random.nextBytes(payload);

            byte[] modified = injector.inject(testClassBytes, payload, InjectionMode.SBOX_SMEAR);
            ExtractionResult result = extractor.extract(modified, PayloadExtractor.ExtractionMode.SBOX);

            assertTrue(result.success(),
                    "Iteration " + i + " (size=" + size + "): extraction should succeed");
            assertArrayEquals(payload, result.payload(),
                    "Iteration " + i + ": payload mismatch");
        }
    }

    // ==================== Helper Methods ====================

    private byte[] createMinimalTestClass() {
        return createTestClass("TestClass");
    }

    private byte[] createTestClass(String className) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(
                org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                org.objectweb.asm.Opcodes.V17,
                org.objectweb.asm.Opcodes.ACC_PUBLIC,
                className,
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

    private Set<String> getFieldNames(byte[] classBytes) {
        org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(classBytes);
        FieldNameCollector collector = new FieldNameCollector();
        reader.accept(collector, 0);
        return collector.fieldNames;
    }

    private static class FieldNameCollector extends org.objectweb.asm.ClassVisitor {
        Set<String> fieldNames = new HashSet<>();

        FieldNameCollector() {
            super(org.objectweb.asm.Opcodes.ASM9);
        }

        @Override
        public org.objectweb.asm.FieldVisitor visitField(int access, String name,
                String descriptor, String signature, Object value) {
            fieldNames.add(name);
            return null;
        }
    }
}
