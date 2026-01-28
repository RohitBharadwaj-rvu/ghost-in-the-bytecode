# API Reference — Sprint 1 Classes

This document provides the API reference for Sprint 1 (Bytecode Injection) classes that Sprint 2+ should use.

---

## ghost.injector.GhostPayloadAttribute

Custom ASM attribute for storing payload in `.class` files.

### Constants
```java
public static final int MAGIC = 0x47504801;  // "GPH" + version 1
public static final String ATTRIBUTE_NAME = "GhostPayload";
```

### Payload Format
| Offset | Size | Description |
|--------|------|-------------|
| 0 | 4 bytes | Magic (0x47504801) |
| 4 | 4 bytes | Data length |
| 8 | N bytes | Payload data |

### Constructor
```java
// Create with payload data
new GhostPayloadAttribute(byte[] data)

// Create empty prototype for ClassReader
new GhostPayloadAttribute()
```

### Methods
```java
// Get payload data (returns copy)
byte[] getData()

// For ASM serialization/deserialization (internal use)
ByteVector write(ClassWriter, byte[], int, int, int)
Attribute read(ClassReader, int, int, char[], int, Label[])
```

---

## ghost.injector.GhostPayloadInjector

Injects and extracts payloads from class files.

### Methods
```java
// Inject payload into class bytes
byte[] inject(byte[] originalClassBytes, byte[] payload)

// Extract payload from modified class (returns null if not found)
byte[] extract(byte[] classBytes)
```

### Usage
```java
GhostPayloadInjector injector = new GhostPayloadInjector();

// Inject
byte[] modified = injector.inject(originalClass, payload);

// Extract
byte[] recovered = injector.extract(modified);
assertArrayEquals(payload, recovered);
```

---

## ghost.injector.ClassFileReader

Reads `.class` files from disk.

```java
ClassFileReader reader = new ClassFileReader();
byte[] classBytes = reader.read(Path.of("MyClass.class"));
byte[] classBytes = reader.read("MyClass.class");
```

---

## ghost.injector.ClassFileWriter

Writes modified `.class` files to disk.

```java
ClassFileWriter writer = new ClassFileWriter();
writer.write(modifiedBytes, Path.of("output/MyClass.class"));
writer.write(modifiedBytes, "output/MyClass.class");
```

---

## ghost.validator.BytecodeValidator

Validates class file correctness.

### ValidationResult Record
```java
record ValidationResult(
    boolean valid,             // Overall validity
    boolean structurallyValid, // ASM CheckClassAdapter passed
    boolean runtimeValid,      // Class loads and executes
    String structuralErrors,   // Error message if structural failure
    String runtimeErrors       // Error message if runtime failure
)
```

### Methods
```java
BytecodeValidator validator = new BytecodeValidator();

// Structural validation only (fast)
ValidationResult result = validator.validateStructure(classBytes);

// Full validation including class loading
ValidationResult result = validator.validateFull(classBytes, "ClassName");

// Runtime validation with method execution
ValidationResult result = validator.validateRuntime(classBytes, "ClassName", "main");
```

---

## Sprint 2 Integration Notes

For the **Extractor Module**, you should:

1. Use `GhostPayloadAttribute` as a prototype when reading classes:
   ```java
   classReader.accept(visitor, 
       new Attribute[] { new GhostPayloadAttribute() }, 0);
   ```

2. The `extract()` method already exists in `GhostPayloadInjector` - Sprint 2 should create a dedicated `PayloadExtractor` class in the `extractor` module that provides:
   - Standalone extraction (no injection dependency)
   - Enhanced integrity validation
   - Error reporting for corrupted payloads
