package ghost.injector;

import org.objectweb.asm.*;

/**
 * Injects an S-Box encoded payload into a class file as a static int[] field.
 * 
 * Features:
 * - Contextual field naming derived from class hash
 * - Load-bearing <clinit> that computes checksum from the table
 * - Resistant to dead-code elimination (ProGuard/R8)
 * 
 * Injected structure:
 * private static final int[] _T3 = { ... }; // S-Box table
 * public static final long _rk; // "integrity check" field
 * 
 * static {
 * long acc = 0;
 * for (int v : _T3) acc ^= v * 31L;
 * _rk = acc;
 * }
 */
public class SBoxPayloadInjector {

    private final SBoxEncoder encoder = new SBoxEncoder();

    /**
     * Injects payload into class bytes using S-Box smearing.
     * 
     * @param originalClassBytes the original .class file bytes
     * @param payload            the payload data to hide
     * @return modified class bytes with embedded payload
     */
    public byte[] inject(byte[] originalClassBytes, byte[] payload) {
        if (originalClassBytes == null || originalClassBytes.length == 0) {
            throw new IllegalArgumentException("Original class bytes cannot be null or empty");
        }
        if (payload == null) {
            payload = new byte[0];
        }

        // Encode payload into S-Box
        int[] sbox = encoder.encode(payload);

        ClassReader classReader = new ClassReader(originalClassBytes);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);

        // Get class name for contextual field naming
        String className = classReader.getClassName();
        String[] fieldNames = generateFieldNames(className);
        String tableFieldName = fieldNames[0]; // e.g., "_T3"
        String checkFieldName = fieldNames[1]; // e.g., "_rk"

        // Transform class: add fields and clinit
        SBoxClassVisitor visitor = new SBoxClassVisitor(
                classWriter, sbox, tableFieldName, checkFieldName);
        classReader.accept(visitor, 0);

        return classWriter.toByteArray();
    }

    /**
     * Generates contextual field names based on class name hash.
     * Returns [tableFieldName, checkFieldName].
     */
    String[] generateFieldNames(String className) {
        int hash = className.hashCode();
        int absHash = Math.abs(hash);

        // Table field: _T0-_T9 or _S0-_S9
        String tablePrefix = (hash % 2 == 0) ? "_T" : "_S";
        String tableFieldName = tablePrefix + (absHash % 10);

        // Check field: _ak, _bk, ... _zk (crypto-looking names)
        char checkChar = (char) ('a' + (absHash % 26));
        String checkFieldName = "_" + checkChar + "k";

        return new String[] { tableFieldName, checkFieldName };
    }

    /**
     * ClassVisitor that injects S-Box fields and clinit.
     */
    private static class SBoxClassVisitor extends ClassVisitor {
        private final int[] sbox;
        private final String tableFieldName;
        private final String checkFieldName;
        private String className;
        private boolean hasExistingClinit = false;

        SBoxClassVisitor(ClassVisitor cv, int[] sbox, String tableFieldName, String checkFieldName) {
            super(Opcodes.ASM9, cv);
            this.sbox = sbox;
            this.tableFieldName = tableFieldName;
            this.checkFieldName = checkFieldName;
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                String superName, String[] interfaces) {
            this.className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                String signature, String[] exceptions) {
            if ("<clinit>".equals(name)) {
                hasExistingClinit = true;
                // Wrap existing clinit to prepend our initialization
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new ClinitInjector(mv, className, sbox, tableFieldName, checkFieldName);
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            // Add the S-Box field: private static final int[] _T3
            FieldVisitor fv = cv.visitField(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                    tableFieldName,
                    "[I",
                    null,
                    null);
            if (fv != null)
                fv.visitEnd();

            // Add the check field: public static final long _rk
            fv = cv.visitField(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                    checkFieldName,
                    "J",
                    null,
                    null);
            if (fv != null)
                fv.visitEnd();

            // If no existing clinit, create one from scratch
            if (!hasExistingClinit) {
                MethodVisitor mv = cv.visitMethod(
                        Opcodes.ACC_STATIC,
                        "<clinit>",
                        "()V",
                        null,
                        null);
                if (mv != null) {
                    mv.visitCode();
                    emitSBoxInitialization(mv, className, sbox, tableFieldName, checkFieldName);
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0); // COMPUTE_FRAMES handles this
                    mv.visitEnd();
                }
            }

            super.visitEnd();
        }
    }

    /**
     * MethodVisitor that prepends S-Box initialization to existing clinit.
     */
    private static class ClinitInjector extends MethodVisitor {
        private final String className;
        private final int[] sbox;
        private final String tableFieldName;
        private final String checkFieldName;
        private boolean initialized = false;

        ClinitInjector(MethodVisitor mv, String className, int[] sbox,
                String tableFieldName, String checkFieldName) {
            super(Opcodes.ASM9, mv);
            this.className = className;
            this.sbox = sbox;
            this.tableFieldName = tableFieldName;
            this.checkFieldName = checkFieldName;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (!initialized) {
                emitSBoxInitialization(mv, className, sbox, tableFieldName, checkFieldName);
                initialized = true;
            }
        }
    }

    /**
     * Emits bytecode to initialize the S-Box table and compute checksum.
     * 
     * Generated bytecode equivalent:
     * _T3 = new int[] { ... };
     * long acc = 0;
     * for (int v : _T3) acc ^= v * 31L;
     * _rk = acc;
     */
    private static void emitSBoxInitialization(MethodVisitor mv, String className,
            int[] sbox, String tableField, String checkField) {
        // Create and initialize the int array
        pushInt(mv, sbox.length);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);

        // Fill array values
        for (int i = 0; i < sbox.length; i++) {
            mv.visitInsn(Opcodes.DUP);
            pushInt(mv, i);
            pushInt(mv, sbox[i]);
            mv.visitInsn(Opcodes.IASTORE);
        }

        // Store in field
        mv.visitFieldInsn(Opcodes.PUTSTATIC, className, tableField, "[I");

        // Compute checksum: long acc = 0
        mv.visitInsn(Opcodes.LCONST_0);
        mv.visitVarInsn(Opcodes.LSTORE, 0); // local 0 = acc

        // Loop through array
        mv.visitFieldInsn(Opcodes.GETSTATIC, className, tableField, "[I");
        mv.visitVarInsn(Opcodes.ASTORE, 2); // local 2 = array ref
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 3); // local 3 = index

        Label loopStart = new Label();
        Label loopEnd = new Label();

        mv.visitLabel(loopStart);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, loopEnd);

        // acc ^= array[i] * 31L
        mv.visitVarInsn(Opcodes.LLOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitInsn(Opcodes.IALOAD);
        mv.visitInsn(Opcodes.I2L);
        mv.visitLdcInsn(31L);
        mv.visitInsn(Opcodes.LMUL);
        mv.visitInsn(Opcodes.LXOR);
        mv.visitVarInsn(Opcodes.LSTORE, 0);

        // i++
        mv.visitIincInsn(3, 1);
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);

        mv.visitLabel(loopEnd);

        // Store checksum: _rk = acc
        mv.visitVarInsn(Opcodes.LLOAD, 0);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, className, checkField, "J");
    }

    /**
     * Pushes an int constant onto the stack using the most efficient instruction.
     */
    private static void pushInt(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }
}
