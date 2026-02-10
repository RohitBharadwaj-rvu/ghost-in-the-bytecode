# System Architecture — ByteStego

## Overview
ByteStego is a steganographic framework for embedding encrypted payloads within structurally valid JVM bytecode. The system comprises a web-based frontend for client-side encryption and a modular Java backend for bytecode-level payload embedding, extraction, and validation.

---

## Use Case Diagram

```mermaid
graph TB
    subgraph Actors
        User["👤 User"]
    end

    subgraph "ByteStego System"
        UC1["Encrypt Payload (Client-Side AES-256-GCM)"]
        UC2["Embed Payload into .class File"]
        UC3["Extract Payload from .class File"]
        UC4["Decrypt Payload (Client-Side)"]
        UC5["Validate Modified Bytecode"]
        UC6["Select Embedding Mode"]
    end

    User --> UC1
    User --> UC2
    User --> UC3
    User --> UC4
    UC2 --> UC5
    UC2 --> UC6
    UC6 -.->|"ATTRIBUTE / SBOX_SMEAR"| UC2
```

---

## Sequence Diagram — Embedding Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as Web UI
    participant Crypto as CryptoModule
    participant API as SteganographyController
    participant Embedder as PayloadEmbedder

    User->>UI: Upload carrier .class + payload file
    UI->>Crypto: Encrypt payload (AES-256-GCM)
    Crypto-->>UI: Encrypted bytes
    UI->>API: POST /inject (carrier, encrypted payload, mode)
    API->>Embedder: embed(classBytes, payload, mode)
    Embedder-->>API: Modified class bytes
    API-->>UI: Download modified .class
```

## Sequence Diagram — Extraction Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as Web UI
    participant API as SteganographyController
    participant Extractor as PayloadExtractor
    participant Crypto as CryptoModule

    User->>UI: Upload modified .class
    UI->>API: POST /extract (modifiedClass)
    API->>Extractor: extract(classBytes)
    Extractor-->>API: ExtractionResult (payload bytes)
    API-->>UI: Download payload.bin
    UI->>Crypto: Decrypt payload (AES-256-GCM)
    Crypto-->>User: Original file
```

---

## Class Diagram

```mermaid
classDiagram
    class SteganographyController {
        -PayloadEmbedder embedder
        -PayloadExtractor extractor
        +inject(carrier, payload, mode) ResponseEntity
        +extract(modifiedClass) ResponseEntity
    }

    class PayloadEmbedder {
        -SBoxEmbedder sboxEmbedder
        +embed(classBytes, payload) byte[]
        +embed(classBytes, payload, mode) byte[]
        +extract(classBytes) byte[]
    }

    class SBoxEmbedder {
        +embed(classBytes, payload) byte[]
    }

    class PayloadExtractor {
        -SBoxPayloadExtractor sboxExtractor
        +extract(classBytes) ExtractionResult
        +extract(classBytes, mode) ExtractionResult
    }

    class SBoxPayloadExtractor {
        +extract(classBytes) ExtractionResult
    }

    class BytecodeValidator {
        +validateStructure(classBytes) ValidationResult
        +validateRuntime(classBytes, className, method) ValidationResult
        +validateFull(classBytes, className) ValidationResult
    }

    class EmbeddingMode {
        <<enumeration>>
        ATTRIBUTE
        SBOX_SMEAR
    }

    class ExtractionResult {
        <<record>>
        +success boolean
        +payload byte[]
        +errorType ErrorType
        +errorMessage String
    }

    class ValidationResult {
        <<record>>
        +valid boolean
        +structurallyValid boolean
        +runtimeValid boolean
    }

    SteganographyController --> PayloadEmbedder : uses
    SteganographyController --> PayloadExtractor : uses
    PayloadEmbedder --> SBoxEmbedder : delegates S-Box mode
    PayloadEmbedder --> EmbeddingMode : selects mode
    PayloadExtractor --> SBoxPayloadExtractor : delegates S-Box mode
    PayloadExtractor --> ExtractionResult : returns
    BytecodeValidator --> ValidationResult : returns
```

---

## Components

| Module | Responsibility |
|--------|---------------|
| **Web UI** | File upload/download, client-side AES-256-GCM encryption/decryption |
| **Embedder** | Bytecode-level payload embedding via ASM framework |
| **Extractor** | Payload recovery from modified class files |
| **Validator** | Structural and runtime verification of modified bytecode |
| **API** | Spring Boot REST interface bridging UI and backend modules |

## Architectural Constraint
All embedded data must be **static**, **non-executable**, and **JVM-verifiable**.
