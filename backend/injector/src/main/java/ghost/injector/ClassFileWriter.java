package ghost.injector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes modified .class file bytes to disk.
 */
public class ClassFileWriter {

    /**
     * Writes class bytes to a file.
     *
     * @param classBytes the modified class bytecode
     * @param outputPath path where the .class file should be written
     * @throws IOException if the file cannot be written
     */
    public void write(byte[] classBytes, Path outputPath) throws IOException {
        // Create parent directories if they don't exist
        Path parent = outputPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.write(outputPath, classBytes);
    }

    /**
     * Writes class bytes to a file.
     *
     * @param classBytes the modified class bytecode
     * @param outputPath path where the .class file should be written as string
     * @throws IOException if the file cannot be written
     */
    public void write(byte[] classBytes, String outputPath) throws IOException {
        write(classBytes, Path.of(outputPath));
    }
}
