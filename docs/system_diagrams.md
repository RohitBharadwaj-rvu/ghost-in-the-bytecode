# System Diagrams: Ghost in the Bytecode

This document contains the UML and Mock diagrams for the varying components and flows within the Ghost in the Bytecode system. These diagrams are generated using GitHub's native Mermaid integration.

## 1. Process Flow / Use-Case Diagram

The following sequence diagram outlines the major use cases: **Secure Message Transfer** (Injection) and **Message Recovery** (Extraction). It details the interactions between the User, the Browser (handling encryption), and the Server (handling bytecode manipulation).

```mermaid
sequenceDiagram
    autonumber
    participant U as User
    participant B as Client (Browser)
    participant S as Server (Spring Boot)

    %% Injection Flow
    rect rgb(200, 220, 240)
    note right of U: Use Case 1: Secure Message Transfer (Injection)
    U->>B: Upload Carrier (.class) & Payload (File)
    U->>B: Enter Passphrase & Select Injection Mode
    U->>B: Click "Initiate Sequence"
    B->>B: Derive Key (PBKDF2) & Encrypt Payload (AES-256-GCM)
    B->>S: POST /inject (Carrier, Encrypted Payload bytes, Mode)
    S->>S: Parse Class structure (ASM Framework)
    S->>S: Inject Payload (Attribute or S-Box Smear)
    S->>S: Validate modified bytecode (URLClassLoader)
    S-->>B: Return Modified Carrier (.class) file
    B-->>U: Prompt to Download modified class
    end

    %% Extraction Flow
    rect rgb(220, 240, 200)
    note right of U: Use Case 2: Message Recovery (Extraction)
    U->>B: Upload Modified Carrier (.class)
    U->>B: Enter Passphrase
    U->>B: Click "Extract Payload"
    B->>S: POST /extract (Modified Carrier)
    S->>S: Parse Class structure (ASM Framework)
    S->>S: Scan & Extract Encrypted Payload
    S-->>B: Return Encrypted Payload bytes
    B->>B: Decrypt Payload (AES-256-GCM)
    B-->>U: Prompt to Download original Payload file
    end
```

## 2. Wireframes / Web UI Mock Diagram

Since Mermaid does not have a dedicated wireframing tool, standard flow node shapes are utilized to demonstrate the layout and structural hierarchy of the Single-Page Application (SPA) Web UI.

```mermaid
graph TD
    classDef container fill:#2d3748,stroke:#4a5568,stroke-width:2px,color:#fff;
    classDef element fill:#4a5568,stroke:#718096,stroke-width:1px,color:#e2e8f0;
    classDef action fill:#48bb78,stroke:#2f855a,stroke-width:2px,color:#fff;
    classDef terminal fill:#000,stroke:#39ff14,stroke-width:2px,color:#39ff14;

    subgraph Window [Browser Window - Dark Theme SPA]
        Header["<h1/> Ghost in the Bytecode"]:::container
        
        subgraph DropZones [File Upload Section]
            CarrierDrop["[ Drag & Drop Carrier Class Here ]"]:::element
            PayloadDrop["[ Drag & Drop Payload File Here ]"]:::element
        end
        
        subgraph Config [Configuration Section]
            ModeSelection{"Mode: [Attribute] / [Ghost S-Box]"}:::element
            PassphraseInput["[***] Enter Encryption Passphrase"]:::element
        end

        BtnAction(("Initiate Sequence / Extract")):::action

        subgraph Feedback [Status & Logs]
            TermOutput> Terminal: \n> Encrypting...\n> Uploading...\n> Success!]:::terminal
        end
        
        Header --- DropZones
        Header --- Config
        DropZones --- BtnAction
        Config --- BtnAction
        BtnAction --- Feedback
    end
```

## 3. Architecture Diagram

The architecture diagram outlines the system's Client-Server topology, illustrating the boundaries of the Zero-Knowledge principle. The server never processes plaintext, and the client handles all cryptographic operations.

```mermaid
graph LR
    subgraph Client [Client-Side (Browser)]
        UI[Web UI / HTML5 / CSS3]
        Crypto[Web Crypto API]
        UI <-->|AES-GCM / PBKDF2| Crypto
    end

    subgraph Backend [Server-Side (Spring Boot)]
        API[REST Controllers]
        ASM[ASM Bytecode Manipulator]
        JVM[JVM Validation Engine]
        
        API -->|Parse / Build| ASM
        ASM -->|Verify Executability| JVM
    end

    %% Network Boundaries
    UI -- "POST /inject\n(Multipart Data)" --> API
    UI -- "POST /extract\n(Carrier File)" --> API
    
    API -- "Modified Class" --> UI
    API -- "Encrypted Bytes" --> UI

    classDef browser fill:#ebf8ff,stroke:#3182ce,stroke-width:2px;
    classDef server fill:#f0fff4,stroke:#38a169,stroke-width:2px;
    
    class Client browser;
    class Backend server;
```
