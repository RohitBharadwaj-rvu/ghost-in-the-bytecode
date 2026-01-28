package ghost.injector;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Demo: Injects a payload into a class and writes it to disk for javap
 * inspection.
 */
public class DemoInjection {
    public static void main(String[] args) throws Exception {
        // Create a simple test class using ASM
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(
                org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                org.objectweb.asm.Opcodes.V17,
                org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "DemoClass",
                null,
                "java/lang/Object",
                null);
        // Default constructor
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

        byte[] originalClass = cw.toByteArray();

        // Inject payload
        GhostPayloadInjector injector = new GhostPayloadInjector();
        byte[] payload = "SECRET_PAYLOAD_DATA_12345".getBytes();
        byte[] modifiedClass = injector.inject(originalClass, payload);

        // Write to file
        Path outputPath = Path.of("target/DemoClass.class");
        Files.write(outputPath, modifiedClass);

        System.out.println("Modified class written to: " + outputPath.toAbsolutePath());
        System.out.println("Payload size: " + payload.length + " bytes");
        System.out.println("Run: javap -v target/DemoClass.class");
    }
}
