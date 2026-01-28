package ghost.injector;

import ghost.validator.BytecodeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SBoxPayloadInjector.
 * Tests bytecode injection without extraction (no extractor dependency).
 */
class SBoxPayloadInjectorTest {

    private SBoxPayloadInjector injector;
    private GhostPayloadInjector ghostInjector;
    private BytecodeValidator validator;
    private byte[] testClassBytes;

    @BeforeEach
    void setUp() {
        injector = new SBoxPayloadInjector();
        ghostInjector = new GhostPayloadInjector();
        validator = new BytecodeValidator();
        testClassBytes = createMinimalTestClass();
    }

    @Test
    @DisplayName("SBoxInject: Field names follow contextual naming pattern")
    void testSBoxInject_FieldsHaveContextualNames() {
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);
        byte[] modified = injector.inject(testClassBytes, payload);

        Set<String> fieldNames = getFieldNames(modified);

        // Should have a field matching _[TS]\d pattern
        boolean hasTableField = fieldNames.stream()
                .anyMatch(name -> name.matches("_[TS]\\d"));
        // Should have a field matching _[a-z]k pattern
        boolean hasCheckField = fieldNames.stream()
                .anyMatch(name -> name.matches("_[a-z]k"));

        assertTrue(hasTableField, "Should have contextual table field (_T0-_T9 or _S0-_S9): " + fieldNames);
        assertTrue(hasCheckField, "Should have contextual check field (_ak-_zk): " + fieldNames);
    }

    @Test
    @DisplayName("SBoxInject: Modified class is structurally valid")
    void testSBoxInject_ClassStructurallyValid() {
        byte[] payload = "Validate this structure!".getBytes(StandardCharsets.UTF_8);
        byte[] modified = injector.inject(testClassBytes, payload);

        BytecodeValidator.ValidationResult result = validator.validateStructure(modified);
        assertTrue(result.structurallyValid(),
                "Modified class should be structurally valid: " + result.structuralErrors());
    }

    @Test
    @DisplayName("SBoxInject: Modified class can be loaded")
    void testSBoxInject_ClassLoadsSuccessfully() {
        byte[] payload = "Loadable payload".getBytes(StandardCharsets.UTF_8);
        byte[] modified = injector.inject(testClassBytes, payload);

        BytecodeValidator.ValidationResult result = validator.validateFull(modified, "TestClass");
        assertTrue(result.structurallyValid(),
                "Class should pass full validation: " + result.structuralErrors());
    }

    @Test
    @DisplayName("SBoxInject: Different classes get different field names")
    void testSBoxInject_TwoDifferentClasses_DifferentFieldNames() {
        byte[] class1 = createTestClass("Alpha");
        byte[] class2 = createTestClass("Beta");
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);

        byte[] modified1 = injector.inject(class1, payload);
        byte[] modified2 = injector.inject(class2, payload);

        Set<String> fields1 = getFieldNames(modified1);
        Set<String> fields2 = getFieldNames(modified2);

        // At least one field name should differ (due to hash-based naming)
        assertNotEquals(fields1, fields2,
                "Different classes should have different contextual field names");
    }

    @Test
    @DisplayName("GhostInjector: SBOX_SMEAR mode delegates to SBoxPayloadInjector")
    void testGhostInjector_SBoxMode() {
        byte[] payload = "test via ghost injector".getBytes(StandardCharsets.UTF_8);
        byte[] modified = ghostInjector.inject(testClassBytes, payload, InjectionMode.SBOX_SMEAR);

        Set<String> fieldNames = getFieldNames(modified);

        // Should have S-Box fields, not GhostPayload attribute
        boolean hasTableField = fieldNames.stream()
                .anyMatch(name -> name.matches("_[TS]\\d"));

        assertTrue(hasTableField, "SBOX_SMEAR mode should inject S-Box fields");
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
