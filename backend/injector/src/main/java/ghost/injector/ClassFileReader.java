package ghost.injector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads .class file bytes from disk.
 */
public class ClassFileReader {

    /**
     * Reads the entire contents of a .class file.
     *
     * @param classFilePath path to the .class file
     * @return byte array containing the class file data
     * @throws IOException if the file cannot be read
     */
    public byte[] read(Path classFilePath) throws IOException {
        if (!Files.exists(classFilePath)) {
            throw new IOException("Class file not found: " + classFilePath);
        }
        if (!classFilePath.toString().endsWith(".class")) {
            throw new IllegalArgumentException("File must have .class extension: " + classFilePath);
        }
        return Files.readAllBytes(classFilePath);
    }

    /**
     * Reads the entire contents of a .class file.
     *
     * @param classFilePath path to the .class file as string
     * @return byte array containing the class file data
     * @throws IOException if the file cannot be read
     */
    public byte[] read(String classFilePath) throws IOException {
        return read(Path.of(classFilePath));
    }
}
