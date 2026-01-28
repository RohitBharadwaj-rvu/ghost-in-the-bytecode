# Bytecode Injection Design

## Injection Location
Encrypted data is injected as a **custom class-level attribute**.

## Attribute Name
GhostPayload

## Attribute Structure
| Field           | Size        |
|----------------|------------|
| Version        | 1 byte     |
| Payload Length | 4 bytes    |
| Payload Data   | N bytes    |

## ASM Strategy
- Use ClassReader to parse class
- Extend org.objectweb.asm.Attribute
- Attach attribute to ClassWriter
- Preserve all existing fields and methods

## JVM Compliance
- No constant pool modification
- No method bytecode modification
- Attribute ignored safely by JVM
