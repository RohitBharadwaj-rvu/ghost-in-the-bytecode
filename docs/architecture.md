# System Architecture

## Overview
The system consists of a web frontend and a backend service responsible for bytecode manipulation.

---

## Use Case Diagram

```mermaid
graph TB
    subgraph Actors
        User["👤 User"]
    end

    subgraph "Ghost in the Bytecode"
        UC1["Encrypt Payload (Client-Side)"]
        UC2["Inject Payload into .class"]
        UC3["Extract Payload from .class"]
        UC4["Decrypt Payload (Client-Side)"]
        UC5["Validate Modified Bytecode"]
        UC6["Select Injection Mode"]
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

## Sequence Diagram — Injection Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as Web UI
    participant Crypto as crypto.js
    participant API as GhostController
    participant Injector as GhostPayloadInjector
    participant Validator as BytecodeValidator

    User->>UI: Upload carrier .class + payload file
    UI->>Crypto: Encrypt payload (AES-256-GCM)
    Crypto-->>UI: Encrypted bytes
    UI->>API: POST /inject (carrier, encrypted payload, mode)
    API->>Injector: inject(classBytes, payload, mode)
    Injector-->>API: Modified class bytes
    API-->>UI: Download modified.class
```

## Sequence Diagram — Extraction Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as Web UI
    participant API as GhostController
    participant Extractor as PayloadExtractor
    participant Crypto as crypto.js

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
    class GhostController {
        -GhostPayloadInjector injector
        -PayloadExtractor extractor
        +inject(carrier, payload, mode) ResponseEntity
        +extract(modifiedClass) ResponseEntity
    }

    class GhostPayloadInjector {
        -SBoxPayloadInjector sboxInjector
        +inject(classBytes, payload) byte[]
        +inject(classBytes, payload, mode) byte[]
        +extract(classBytes) byte[]
    }

    class SBoxPayloadInjector {
        +inject(classBytes, payload) byte[]
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

    class InjectionMode {
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

    GhostController --> GhostPayloadInjector : uses
    GhostController --> PayloadExtractor : uses
    GhostPayloadInjector --> SBoxPayloadInjector : delegates S-Box
    GhostPayloadInjector --> InjectionMode : selects mode
    PayloadExtractor --> SBoxPayloadExtractor : delegates S-Box
    PayloadExtractor --> ExtractionResult : returns
    BytecodeValidator --> ValidationResult : returns
```

---

## Components

### 1. Frontend (Web UI)
- File upload/download
- Client-side encryption/decryption
- Key management (user-supplied)

### 2. Injection Service (Backend)
- Reads Java `.class` files
- Injects encrypted payload into class attributes
- Outputs valid `.class` files

### 3. Extraction Service
- Reads modified `.class` files
- Extracts encrypted payload
- Returns payload bytes

### 4. Validation Module
- Ensures modified classes still execute
- Detects corruption or invalid bytecode

## Architectural Principle
All injected data must be **static**, **non-executable**, and **JVM-verifiable**.
