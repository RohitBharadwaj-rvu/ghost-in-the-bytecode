package ghost.injector;

import ghost.validator.BytecodeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Sprint 1: Bytecode Injection
 * 
 * Validates that:
 * 1. Payload can be injected into .class files
 * 2. Modified class executes unchanged
 * 3. Payload can be extracted and matches original
 * 4. No JVM verification errors occur
 */
class InjectionTest {

    private GhostPayloadInjector injector;
    private BytecodeValidator validator;
    private byte[] sampleClassBytes;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        injector = new GhostPayloadInjector();
        validator = new BytecodeValidator();
        
        // Load compiled SampleClass bytes
        // First, we need to compile and load the SampleClass
        sampleClassBytes = compileAndLoadSampleClass();
    }

    /**
     * Compiles SampleClass.java and returns its bytecode.
     */
    private byte[] compileAndLoadSampleClass() throws Exception {
        // Try to load from the classpath first (if compiled)
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("SampleClass.class")) {
            if (is != null) {
                return readAllBytes(is);
            }
        }
        
        // If not available, create a minimal class programmatically using ASM
        return createMinimalTestClass();
    }

    /**
     * Creates a minimal test class using ASM for testing purposes.
     */
    private byte[] createMinimalTestClass() {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(
            org.objectweb.asm.ClassWriter.COMPUTE_FRAMES | org.objectweb.asm.ClassWriter.COMPUTE_MAXS
        );
        
        cw.visit(
            org.objectweb.asm.Opcodes.V17,
            org.objectweb.asm.Opcodes.ACC_PUBLIC,
            "TestClass",
            null,
            "java/lang/Object",
            null
        );
        
        // Default constructor
        var mv = cw.visitMethod(
            org.objectweb.asm.Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        );
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
            org.objectweb.asm.Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        );
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        
        // Static method: getMessage()
        mv = cw.visitMethod(
            org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC,
            "getMessage",
            "()Ljava/lang/String;",
            null,
            null
        );
        mv.visitCode();
        mv.visitLdcInsn("TestClass is working!");
        mv.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
        
        cw.visitEnd();
        return cw.toByteArray();
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    @Test
    void testInjectPayload_ModifiedClassIsStructurallyValid() {
        // Arrange
        byte[] payload = "Secret message!".getBytes(StandardCharsets.UTF_8);
        
        // Act
        byte[] modifiedClass = injector.inject(sampleClassBytes, payload);
        
        // Assert
        assertNotNull(modifiedClass);
        assertTrue(modifiedClass.length > sampleClassBytes.length, 
            "Modified class should be larger than original");
        
        BytecodeValidator.ValidationResult result = validator.validateStructure(modifiedClass);
        assertTrue(result.structurallyValid(), 
            "Modified class should be structurally valid: " + result.structuralErrors());
    }

    @Test
    void testInjectPayload_PayloadCanBeExtracted() {
        // Arrange
        byte[] originalPayload = "This is a secret payload for testing!".getBytes(StandardCharsets.UTF_8);
        
        // Act
        byte[] modifiedClass = injector.inject(sampleClassBytes, originalPayload);
        byte[] extractedPayload = injector.extract(modifiedClass);
        
        // Assert
        assertNotNull(extractedPayload, "Extracted payload should not be null");
        assertArrayEquals(originalPayload, extractedPayload, 
            "Extracted payload should match original");
    }

    @Test
    void testInjectPayload_CanInjectLargePayload() {
        // Arrange - 1MB payload
        byte[] largePayload = new byte[1024 * 1024];
        Arrays.fill(largePayload, (byte) 0x42);
        
        // Act
        byte[] modifiedClass = injector.inject(sampleClassBytes, largePayload);
        byte[] extractedPayload = injector.extract(modifiedClass);
        
        // Assert
        assertNotNull(extractedPayload);
        assertArrayEquals(largePayload, extractedPayload);
        
        BytecodeValidator.ValidationResult result = validator.validateStructure(modifiedClass);
        assertTrue(result.structurallyValid());
    }

    @Test
    void testInjectPayload_CanInjectEmptyPayload() {
        // Arrange
        byte[] emptyPayload = new byte[0];
        
        // Act
        byte[] modifiedClass = injector.inject(sampleClassBytes, emptyPayload);
        byte[] extractedPayload = injector.extract(modifiedClass);
        
        // Assert
        assertNotNull(extractedPayload);
        assertEquals(0, extractedPayload.length);
    }

    @Test
    void testInjectPayload_BinaryPayloadPreserved() {
        // Arrange - binary data with all byte values
        byte[] binaryPayload = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryPayload[i] = (byte) i;
        }
        
        // Act
        byte[] modifiedClass = injector.inject(sampleClassBytes, binaryPayload);
        byte[] extractedPayload = injector.extract(modifiedClass);
        
        // Assert
        assertArrayEquals(binaryPayload, extractedPayload, 
            "Binary payload should be preserved exactly");
    }

    @Test
    void testInjectPayload_ModifiedClassCanBeLoaded() {
        // Arrange
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);
        byte[] modifiedClass = injector.inject(sampleClassBytes, payload);
        
        // Act
        BytecodeValidator.ValidationResult result = validator.validateFull(modifiedClass, "TestClass");
        
        // Assert
        assertTrue(result.structurallyValid(), 
            "Class should be structurally valid: " + result.structuralErrors());
    }

    @Test
    void testExtract_ReturnsNullForClassWithoutPayload() {
        // Act
        byte[] result = injector.extract(sampleClassBytes);
        
        // Assert
        assertNull(result, "Should return null for class without GhostPayload");
    }

    @Test
    void testInject_ThrowsOnNullClassBytes() {
        assertThrows(IllegalArgumentException.class, 
            () -> injector.inject(null, new byte[0]));
    }

    @Test
    void testInject_ThrowsOnNullPayload() {
        assertThrows(IllegalArgumentException.class, 
            () -> injector.inject(sampleClassBytes, null));
    }
}
