package ghost.extractor;

import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts payload from S-Box smeared class files.
 * 
 * Extraction strategy:
 * 1. Parse class, find all static final int[] fields with valid sizes
 * (128/192/256)
 * 2. For each candidate: check if sbox[0] ^ sbox[last] yields valid length
 * 3. Attempt decode, verify CRC32
 * 4. Return first successful extraction
 */
public class SBoxPayloadExtractor {

    private final SBoxEncoder encoder = new SBoxEncoder();

    /**
     * Attempts to extract a payload from class bytes using S-Box detection.
     * 
     * @param classBytes the class file bytes
     * @return ExtractionResult with payload or error
     */
    public ExtractionResult extract(byte[] classBytes) {
        if (classBytes == null || classBytes.length == 0) {
            return ExtractionResult.corrupted("Class bytes cannot be null or empty");
        }

        try {
            ClassReader classReader = new ClassReader(classBytes);
            SBoxFieldCollector collector = new SBoxFieldCollector();
            classReader.accept(collector, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            String className = classReader.getClassName();

            // Try each candidate field
            for (FieldCandidate candidate : collector.getCandidates()) {
                int[] sboxValues = extractFieldValues(classBytes, className, candidate.name);
                if (sboxValues != null && encoder.isValidTableSize(sboxValues.length)) {
                    byte[] payload = encoder.decode(sboxValues);
                    if (payload != null) {
                        return ExtractionResult.success(payload);
                    }
                }
            }

            return ExtractionResult.failure(ExtractionResult.ErrorType.NO_PAYLOAD, "No valid S-Box payload found");
        } catch (Exception e) {
            return ExtractionResult.corrupted("Failed to parse class: " + e.getMessage());
        }
    }

    /**
     * Extracts the actual int[] values from a static field.
     * This requires analyzing the <clinit> bytecode.
     */
    private int[] extractFieldValues(byte[] classBytes, String className, String fieldName) {
        ClassReader classReader = new ClassReader(classBytes);
        ClinitValueExtractor extractor = new ClinitValueExtractor(className, fieldName);
        classReader.accept(extractor, 0);
        return extractor.getValues();
    }

    /**
     * Represents a candidate static int[] field.
     */
    private static class FieldCandidate {
        final String name;
        final int access;

        FieldCandidate(String name, int access) {
            this.name = name;
            this.access = access;
        }
    }

    /**
     * ClassVisitor that collects static final int[] fields.
     */
    private static class SBoxFieldCollector extends ClassVisitor {
        private final List<FieldCandidate> candidates = new ArrayList<>();

        SBoxFieldCollector() {
            super(Opcodes.ASM9);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                String signature, Object value) {
            // Look for static int[] fields
            if ((access & Opcodes.ACC_STATIC) != 0 && "[I".equals(descriptor)) {
                candidates.add(new FieldCandidate(name, access));
            }
            return null;
        }

        List<FieldCandidate> getCandidates() {
            return candidates;
        }
    }

    /**
     * ClassVisitor that extracts int[] values from <clinit> initialization.
     */
    private static class ClinitValueExtractor extends ClassVisitor {
        private final String targetClassName;
        private final String targetFieldName;
        private int[] values = null;

        ClinitValueExtractor(String className, String fieldName) {
            super(Opcodes.ASM9);
            this.targetClassName = className;
            this.targetFieldName = fieldName;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                String signature, String[] exceptions) {
            if ("<clinit>".equals(name)) {
                return new ClinitAnalyzer(targetClassName, targetFieldName, vals -> values = vals);
            }
            return null;
        }

        int[] getValues() {
            return values;
        }
    }

    /**
     * MethodVisitor that traces array initialization and PUTSTATIC to extract
     * values.
     * 
     * Pattern detected:
     * SIPUSH/LDC size
     * NEWARRAY T_INT
     * DUP, ICONST/BIPUSH/SIPUSH/LDC index, ICONST/BIPUSH/SIPUSH/LDC value, IASTORE
     * ... repeat ...
     * PUTSTATIC className/fieldName [I
     */
    private static class ClinitAnalyzer extends MethodVisitor {
        private final String targetClassName;
        private final String targetFieldName;
        private final java.util.function.Consumer<int[]> resultCallback;

        private int[] currentArray = null;
        private int pendingIndex = -1;

        ClinitAnalyzer(String className, String fieldName, java.util.function.Consumer<int[]> callback) {
            super(Opcodes.ASM9);
            this.targetClassName = className;
            this.targetFieldName = fieldName;
            this.resultCallback = callback;
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (opcode == Opcodes.NEWARRAY && operand == Opcodes.T_INT) {
                // Array size should have been pushed before this
                // We'll handle this in visitLdcInsn and push constants
            } else if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                handlePushedInt(operand);
            }
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof Integer) {
                handlePushedInt((Integer) value);
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
                handlePushedInt(opcode - Opcodes.ICONST_0);
            } else if (opcode == Opcodes.IASTORE && currentArray != null && pendingIndex >= 0) {
                // Value should have been set by previous handlePushedInt calls
                pendingIndex = -1;
            }
        }

        private int arraySize = 0;
        private int lastPushedValue = 0;

        private void handlePushedInt(int value) {
            if (currentArray == null) {
                // This might be the array size
                arraySize = value;
            } else if (pendingIndex < 0) {
                // This is the array index
                pendingIndex = value;
            } else {
                // This is the value to store
                if (pendingIndex >= 0 && pendingIndex < currentArray.length) {
                    currentArray[pendingIndex] = value;
                }
                lastPushedValue = value;
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            // ANEWARRAY wouldn't apply for int[], but we handle it for completeness
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (opcode == Opcodes.PUTSTATIC &&
                    targetClassName.equals(owner) &&
                    targetFieldName.equals(name) &&
                    "[I".equals(descriptor)) {
                // Found our target field assignment
                if (currentArray != null) {
                    resultCallback.accept(currentArray);
                }
            }
        }

        // Called after NEWARRAY to initialize our tracking array
        @Override
        public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
            // Not needed for our analysis
        }

        // We need to detect when NEWARRAY T_INT happens and create our tracking array
        private boolean pendingNewArray = false;

        @Override
        public void visitCode() {
            // Reset state
            currentArray = null;
            pendingIndex = -1;
            arraySize = 0;
        }

        // Override to catch the NEWARRAY pattern
        @Override
        public void visitVarInsn(int opcode, int var) {
            // Track local variable stores if needed
        }

        // Better approach: track stack simulation
        private java.util.ArrayList<Integer> intStack = new java.util.ArrayList<>();

        private void pushStack(int value) {
            intStack.add(value);
        }

        private int popStack() {
            if (intStack.isEmpty())
                return 0;
            return intStack.remove(intStack.size() - 1);
        }
    }
}
