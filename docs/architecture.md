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

---

## Component Diagram — Architecture

This diagram illustrates the high-level module structure and dependencies within the ByteStego system.

```mermaid
componentDiagram
    component "Web Frontend" as UI
    component "REST API" as API
    
    package "ByteStego Backend" {
        component "SteganographyController" as Controller
        component "PayloadEmbedder" as Embedder
        component "PayloadExtractor" as Extractor
        component "BytecodeValidator" as Validator
        component "ASM Library" as ASM
    }

    UI --> API : HTTP/JSON
    API --> Controller : Internal Call
    Controller --> Embedder : Uses
    Controller --> Extractor : Uses
    Embedder --> Validator : Validates Output
    Embedder --> ASM : Manipulates Bytecode
    Extractor --> ASM : Parses Bytecode
    Validator --> ASM : Analyzes Structure
```

---

## State Diagram — Payload Model

This diagram models the lifecycle of a payload as it transitions through the system states.

```mermaid
stateDiagram-v2
    [*] --> Plaintext : User Selects File
    Plaintext --> Encrypted : Client-Side Encryption (AES-GCM)
    Encrypted --> Embedded : Injection into .class (ByteStego)
    Embedded --> Extracted : Extraction from .class
    Extracted --> Decrypted : Client-Side Decryption
    Decrypted --> [*] : User Download
    
    state Embedded {
        [*] --> ValidatingStructure
        ValidatingStructure --> ValidatingRuntime : Structure OK
        ValidatingRuntime --> Ready : Runtime OK
        ValidatingStructure --> Corrupted : Error
        ValidatingRuntime --> Corrupted : Error
    }
```

---

## Algorithm Flowchart — S-Box Embedding Design

This flowchart details the logic for the stealthy "S-Box Smearing" embedding mode.

```mermaid
flowchart TD
    Start([Start Embedding]) --> InputCheck{Valid Input?}
    InputCheck -- No --> Error([Return Error])
    InputCheck -- Yes --> ParseClass[Parse .class with ASM]
    
    ParseClass --> SelectMode{Mode Selection}
    
    SelectMode -- Attribute --> InjectAttr[Inject 'GhostPayload' Attribute]
    
    SelectMode -- SBox Smear --> GenSBox[Generate S-Box Int Arrays]
    GenSBox --> SplitPayload[Split Payload into S-Box Chunks]
    SplitPayload --> FindClinit[Find <clinit> Method]
    FindClinit --> InfectClinit[Inject S-Box Initialization Code]
    InfectClinit --> Verify[Validate Bytecode Structure]
    
    InjectAttr --> Verify
    
    Verify --> Result{Valid?}
    Result -- Yes --> WriteClass[Write Modified .class]
    Result -- No --> Revert[Revert Changes]
    
    WriteClass --> End([End])
    Revert --> Error
```

---

## Architectural Constraint
All embedded data must be **static**, **non-executable**, and **JVM-verifiable**.
