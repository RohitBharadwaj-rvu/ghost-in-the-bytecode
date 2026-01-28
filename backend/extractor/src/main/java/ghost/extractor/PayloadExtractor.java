package ghost.extractor;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Extracts payloads from Java class files.
 * 
 * Supports two extraction modes:
 * - ATTRIBUTE: Traditional GhostPayload custom attribute
 * - SBOX: S-Box smeared payload in static int[] fields
 * 
 * Default behavior tries both methods automatically.
 */
public class PayloadExtractor {

    private final SBoxPayloadExtractor sboxExtractor = new SBoxPayloadExtractor();

    /**
     * Extraction mode selection.
     */
    public enum ExtractionMode {
        /** Try both methods, S-Box first then attribute */
        AUTO,
        /** Only try GhostPayload attribute */
        ATTRIBUTE,
        /** Only try S-Box smeared payload */
        SBOX
    }

    /**
     * Extracts payload using auto-detection (tries S-Box first, then attribute).
     *
     * @param classBytes the class file bytes to extract from
     * @return ExtractionResult with payload data or structured error
     */
    public ExtractionResult extract(byte[] classBytes) {
        return extract(classBytes, ExtractionMode.AUTO);
    }

    /**
     * Extracts payload using the specified mode.
     *
     * @param classBytes the class file bytes to extract from
     * @param mode       the extraction mode to use
     * @return ExtractionResult with payload data or structured error
     */
    public ExtractionResult extract(byte[] classBytes, ExtractionMode mode) {
        if (classBytes == null || classBytes.length == 0) {
            return ExtractionResult.failure(
                    ExtractionResult.ErrorType.CORRUPTED,
                    "Class bytes are null or empty");
        }

        switch (mode) {
            case SBOX:
                return sboxExtractor.extract(classBytes);
            case ATTRIBUTE:
                return extractAttribute(classBytes);
            case AUTO:
            default:
                // Try S-Box first (newer method), fall back to attribute
                ExtractionResult sboxResult = sboxExtractor.extract(classBytes);
                if (sboxResult.success()) {
                    return sboxResult;
                }
                return extractAttribute(classBytes);
        }
    }

    /**
     * Extracts using the GhostPayload attribute method.
     */
    private ExtractionResult extractAttribute(byte[] classBytes) {
        try {
            ClassReader classReader = new ClassReader(classBytes);
            ExtractorVisitor visitor = new ExtractorVisitor();

            // CRITICAL: Register attribute prototype for ASM to recognize it
            Attribute[] attributePrototypes = { new GhostPayloadAttribute() };
            classReader.accept(visitor, attributePrototypes, 0);

            return visitor.getResult();

        } catch (Exception e) {
            return ExtractionResult.corrupted("Failed to parse class: " + e.getMessage());
        }
    }

    /**
     * ClassVisitor that captures the GhostPayload attribute.
     */
    private static class ExtractorVisitor extends ClassVisitor {
        private GhostPayloadAttribute foundAttribute = null;

        ExtractorVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            if (attribute instanceof GhostPayloadAttribute gpa) {
                this.foundAttribute = gpa;
            }
            super.visitAttribute(attribute);
        }

        /**
         * Returns the extraction result.
         */
        ExtractionResult getResult() {
            if (foundAttribute == null) {
                return ExtractionResult.noPayload();
            }

            // The attribute's read() method already performed validation
            ExtractionResult validationResult = foundAttribute.getValidationResult();
            if (validationResult != null) {
                return validationResult;
            }

            // Fallback if no validation result (shouldn't happen)
            byte[] data = foundAttribute.getData();
            if (data != null) {
                return ExtractionResult.success(data);
            }
            return ExtractionResult.noPayload();
        }
    }
}
