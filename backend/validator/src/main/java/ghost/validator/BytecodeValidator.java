package ghost.validator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates that modified .class files are structurally correct and executable.
 * 
 * Provides two levels of validation:
 * 1. Structural validation using ASM's CheckClassAdapter
 * 2. Runtime validation by actually loading and executing the class
 */
public class BytecodeValidator {

    /**
     * Result of bytecode validation.
     */
    public record ValidationResult(
        boolean valid,
        boolean structurallyValid,
        boolean runtimeValid,
        String structuralErrors,
        String runtimeErrors
    ) {
        public static ValidationResult success() {
            return new ValidationResult(true, true, true, null, null);
        }

        public static ValidationResult structuralFailure(String errors) {
            return new ValidationResult(false, false, false, errors, null);
        }

        public static ValidationResult runtimeFailure(String errors) {
            return new ValidationResult(false, true, false, null, errors);
        }
    }

    /**
     * Validates class bytes structurally using ASM's CheckClassAdapter.
     * This ensures the bytecode is well-formed and JVM-verifiable.
     *
     * @param classBytes the class bytes to validate
     * @return validation result
     */
    public ValidationResult validateStructure(byte[] classBytes) {
        try {
            ClassReader classReader = new ClassReader(classBytes);
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            // CheckClassAdapter throws exceptions for invalid bytecode
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            CheckClassAdapter checkAdapter = new CheckClassAdapter(classWriter, true);
            
            classReader.accept(checkAdapter, 0);

            // If we get here without exception, the class is structurally valid
            return new ValidationResult(true, true, false, null, null);
        } catch (Exception e) {
            return ValidationResult.structuralFailure(e.getMessage());
        }
    }

    /**
     * Validates class bytes by actually loading and executing the class.
     * This confirms the class runs unchanged after injection.
     *
     * @param classBytes the class bytes to validate
     * @param className the fully qualified class name (e.g., "SampleClass")
     * @param methodToRun optional method name to invoke (null to skip execution)
     * @return validation result
     */
    public ValidationResult validateRuntime(byte[] classBytes, String className, String methodToRun) {
        Path tempDir = null;
        try {
            // First validate structure
            ValidationResult structuralResult = validateStructure(classBytes);
            if (!structuralResult.structurallyValid()) {
                return structuralResult;
            }

            // Create a temporary directory for the class file
            tempDir = Files.createTempDirectory("ghost-validator");
            Path classFile = tempDir.resolve(className + ".class");
            Files.write(classFile, classBytes);

            // Load the class using URLClassLoader
            URL[] urls = { tempDir.toUri().toURL() };
            try (URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader())) {
                Class<?> loadedClass = classLoader.loadClass(className);

                // If a method is specified, try to run it
                if (methodToRun != null && !methodToRun.isEmpty()) {
                    Method method = loadedClass.getMethod(methodToRun);
                    method.invoke(null); // Assume static method for simplicity
                }

                return ValidationResult.success();
            }
        } catch (Exception e) {
            return ValidationResult.runtimeFailure(e.getMessage());
        } finally {
            // Cleanup temp directory
            if (tempDir != null) {
                try {
                    Files.deleteIfExists(tempDir.resolve(className + ".class"));
                    Files.deleteIfExists(tempDir);
                } catch (Exception ignored) {
                    // Best effort cleanup
                }
            }
        }
    }

    /**
     * Performs full validation: structural + runtime.
     *
     * @param classBytes the class bytes to validate
     * @param className the fully qualified class name
     * @return validation result
     */
    public ValidationResult validateFull(byte[] classBytes, String className) {
        return validateRuntime(classBytes, className, null);
    }
}
