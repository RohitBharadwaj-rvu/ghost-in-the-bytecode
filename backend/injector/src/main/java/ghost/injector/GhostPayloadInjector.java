package ghost.injector;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Injects payloads into .class files.
 * 
 * Supports two injection modes:
 * - ATTRIBUTE: Traditional GhostPayload custom attribute (smaller, faster)
 * - SBOX_SMEAR: Stealth S-Box smearing (resistant to static analysis)
 */
public class GhostPayloadInjector {

    private final SBoxPayloadInjector sboxInjector = new SBoxPayloadInjector();

    /**
     * Injects a payload using the default ATTRIBUTE mode.
     *
     * @param originalClassBytes the original .class file bytes
     * @param payload            the payload data to inject
     * @return modified class bytes with the payload embedded
     */
    public byte[] inject(byte[] originalClassBytes, byte[] payload) {
        return inject(originalClassBytes, payload, InjectionMode.ATTRIBUTE);
    }

    /**
     * Injects a payload using the specified injection mode.
     *
     * @param originalClassBytes the original .class file bytes
     * @param payload            the payload data to inject
     * @param mode               the injection mode to use
     * @return modified class bytes with the payload embedded
     */
    public byte[] inject(byte[] originalClassBytes, byte[] payload, InjectionMode mode) {
        if (originalClassBytes == null || originalClassBytes.length == 0) {
            throw new IllegalArgumentException("Original class bytes cannot be null or empty");
        }
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }

        switch (mode) {
            case SBOX_SMEAR:
                return sboxInjector.inject(originalClassBytes, payload);
            case ATTRIBUTE:
            default:
                return injectAttribute(originalClassBytes, payload);
        }
    }

    /**
     * Injects payload using the GhostPayload attribute method.
     */
    private byte[] injectAttribute(byte[] originalClassBytes, byte[] payload) {
        // Create class reader to parse the original class
        ClassReader classReader = new ClassReader(originalClassBytes);

        // Create class writer with COMPUTE_MAXS to recalculate stack sizes if needed
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);

        // Create a visitor that will inject our attribute
        ClassVisitor injectorVisitor = new GhostPayloadClassVisitor(classWriter, payload);

        // Parse and transform the class
        // Pass the prototype attribute so ASM recognizes existing GhostPayload
        // attributes
        classReader.accept(
                injectorVisitor,
                new org.objectweb.asm.Attribute[] { new GhostPayloadAttribute() },
                0);

        return classWriter.toByteArray();
    }

    /**
     * ClassVisitor that injects the GhostPayload attribute at the class level.
     */
    private static class GhostPayloadClassVisitor extends ClassVisitor {
        private final byte[] payload;
        private boolean attributeInjected = false;

        GhostPayloadClassVisitor(ClassVisitor classVisitor, byte[] payload) {
            super(Opcodes.ASM9, classVisitor);
            this.payload = payload;
        }

        @Override
        public void visitEnd() {
            // Inject the GhostPayload attribute at class level before ending
            if (!attributeInjected) {
                GhostPayloadAttribute attribute = new GhostPayloadAttribute(payload);
                cv.visitAttribute(attribute);
                attributeInjected = true;
            }
            super.visitEnd();
        }
    }

    /**
     * Extracts the GhostPayload data from a modified class file.
     * This is provided for validation purposes in Sprint 1.
     *
     * @param classBytes the class bytes to extract from
     * @return the extracted payload data, or null if no payload found
     */
    public byte[] extract(byte[] classBytes) {
        if (classBytes == null || classBytes.length == 0) {
            return null;
        }

        ClassReader classReader = new ClassReader(classBytes);
        PayloadExtractorVisitor extractor = new PayloadExtractorVisitor();

        // Pass the prototype attribute so ASM can parse our custom attribute
        classReader.accept(
                extractor,
                new org.objectweb.asm.Attribute[] { new GhostPayloadAttribute() },
                0);

        return extractor.getPayload();
    }

    /**
     * ClassVisitor that extracts the GhostPayload attribute.
     */
    private static class PayloadExtractorVisitor extends ClassVisitor {
        private byte[] payload = null;

        PayloadExtractorVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visitAttribute(org.objectweb.asm.Attribute attribute) {
            if (attribute instanceof GhostPayloadAttribute gpa) {
                this.payload = gpa.getData();
            }
            super.visitAttribute(attribute);
        }

        public byte[] getPayload() {
            return payload;
        }
    }
}
