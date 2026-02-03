# Software Requirements Specification (SRS)
# Ghost in the Bytecode

**Version**: 1.0
**Date**: 2026-02-03

---

## 1. INTRODUCTION

### 1.1 Document Purpose
The purpose of this document is to define the software requirements for the "Ghost in the Bytecode" system. It specifies the functional and non-functional requirements to guide development, testing, and validation of the system. This document is intended for developers, security researchers, and stakeholders involved in the project.

### 1.2 Product Scope
"Ghost in the Bytecode" is a security-focused tool designed to embed encrypted data, referred to as payloads, into compiled Java class files, known as carriers, without breaking their execution. The system features secure payload injection which embeds data into class files using custom attributes or steganographic techniques such as S-Box smearing. It enforces client-side encryption to ensure the backend never processes plaintext data, adhering to a Zero-Knowledge or Zero-Trust model. The system maintains bytecode integrity so that the modified carrier classes remain valid and executable by the Java Virtual Machine. Furthermore, it provides payload recovery capabilities to extract and decrypt the hidden data from the modified class files.

### 1.3 Intended Audience and Document Overview
This document is prepared for a technical audience. Developers will use it to understand the architecture, API, and core logic necessary for implementing the injection and extraction mechanisms. Testers will rely on this specification to validate the security, integrity, and functionality of the system. Security researchers may use this document to analyze the steganographic methods employed by the application.

### 1.4 Definitions, Acronyms and Abbreviations
A Carrier is defined as a valid Java class file used to hide the payload. A Payload is the arbitrary data or file that is to be hidden inside the carrier. Injection refers to the process of embedding the payload into the carrier, while Extraction is the process of recovering the payload from the carrier. The JVM stands for the Java Virtual Machine. ASM is the Java bytecode manipulation framework used by the backend. An S-Box, or Substitution Box, is a cryptographic component used in this system as a camouflage for payload data. AES-GCM refers to the Advanced Encryption Standard in Galois/Counter Mode.

### 1.5 Document Conventions
Standard document conventions are followed wherein bold text indicates key terms or emphasis. Code font is utilized to indicate file names, API endpoints, or code snippets.

### 1.6 References and Acknowledgments
This document references the Java Virtual Machine Specification (JVMS), the ASM Bytecode Manipulation Framework Documentation, and the Web Crypto API Specification.

---

## 2. OVERALL DESCRIPTION

### 2.1 Product Overview
The system operates as a client-server application. The Frontend, a Web UI, handles user interaction and performs encryption and decryption locally within the browser. The Backend, built as a Spring Boot API, relies on the frontend for secure data handling and performs the complex bytecode manipulation tasks, including Injection, Extraction, and validation.

### 2.2 Product Functionality
The product functionality is divided into four main operations. First, the client-side encryption allows the user to provide a payload and a passphrase, which the browser uses to encrypt the payload via AES-256-GCM. Second, the server-side injection accepts the carrier class and the encrypted payload, embeds the payload using the selected mode (Attribute or S-Box), aids in validating the modified class structure and runtime executability, and returns the modified class file to the user. Third, the server-side extraction accepts a modified class file, scans it for hidden payloads using both Attribute and S-Box scanning methods, and returns the raw encrypted payload. Finally, the client-side decryption allows the user to provide the encrypted payload and passphrase, which the browser uses to decrypt and restore the original file.

### 2.3 Design and Implementation Constraints
The system faces several constraints. The bytecode size is a primary concern, as the injected payload increases the class file size; large payloads may potentially hit JVM class size limits, although this is rare for typical use cases. Browser compatibility is required, specifically a modern browser that supports the Web Crypto API. reliable backend operation requires Java 17 or higher.

### 2.4 Assumptions and Dependencies
It is assumed that the user provides a valid, non-obfuscated Java class file to serve as a carrier. Furthermore, the security model relies on the assumption that the user will remember the passphrase, as there is no recovery mechanism for lost passphrases.

---

## 3. SPECIFIC REQUIREMENTS

### 3.1 External Interface Requirements

#### 3.1.1 User Interfaces
The user interface is a responsive, dark-themed HTML5 and CSS3 application. It provides drag-and-drop areas for uploading Payload and Carrier files. Configuration options include the selection of Injection Mode, with choices for Standard Attribute or Ghost S-Box, and an input field for the Encryption Passphrase which is masked and toggleable. The interface provides feedback through a terminal-style log output showing status messages such as initiating, encrypting, and success.

#### 3.1.2 Hardware Interfaces
The system requires standard web server and client hardware. No specialized hardware interfaces are necessary.

#### 3.1.3 Software Interfaces
The backend exposes a REST API for the frontend to consume. This includes a POST endpoint for injection that handles multipart uploads of the carrier, payload, and mode. Another POST endpoint is provided for extraction, handling the multipart upload of the modified class. The system also interacts with the JVM, acting as a custom class loader for validation purposes.

#### 3.1.4 Communications Interfaces
Communication occurs over HTTP/1.1 or HTTP/2 over TCP/IP, suitable for both localhost and deployed server environments. JSON is utilized for error messages, while binary streams are used for file transfers.

### 3.2 Functional Requirements

#### 3.2.1 Payload Encryption & Decryption
The system is required to encrypt the payload on the client side using AES-256-GCM to ensure confidentiality. The encryption key must be derived from the user passphrase using PBKDF2 with 100,000 iterations and SHA-256 hash. A random 16-byte Salt and 12-byte IV must be generated for each encryption operation to ensure uniqueness and security. Crucially, the backend must not receive the plaintext payload or the passphrase at any point.

#### 3.2.2 Bytecode Injection
The system must support Attribute Mode, which involves storing data in a custom GhostPayload attribute (FR-05). This attribute must be ignored by standard JVMs during execution but preserved in the file. Additionally, the system must support S-Box Smear Mode, where data is hidden within generated method bytecode that resembles cryptographic S-Box lookups (FR-06). This generated code must be valid bytecode but unreachable or having no side effects on the program logic.

#### 3.2.3 Class Validation
All modified classes must pass structural validation using the ASM CheckClassAdapter (FR-07). Furthermore, all modified classes must be loadable by a standard URLClassLoader without triggering any verification errors (FR-08).

#### 3.2.4 Payload Extraction
The system must be capable of detecting and extracting payloads hidden via Attribute Mode (FR-09). Similarly, it must be able to detect and extract payloads hidden via S-Box Smear Mode (FR-10). If extraction fails or no payload is found, the system must return an appropriate error message to the user (FR-11).

### 3.3 Use Case Model

#### 3.3.1 Use Case #1: Secure Message Transfer
In this use case, the actor acts as the User, named Alice. Her goal is to hide a secret text file inside a calculator app, specifically Calc.class, to send to Bob. The flow begins when Alice opens the Web UI. She drops the secret text file as the Payload and the class file as the Carrier. She enters a strong passphrase and selects the Ghost S-Box mode for stealth. Upon clicking the initiate sequence button, the browser encrypts the secret. The backend then injects the encrypted bytes into the class file. Finally, Alice downloads the modified class file and sends it to Bob.

#### 3.3.2 Use Case #2: Message Recovery
In this use case, the actor is the User, named Bob. His goal is to retrieve the secret from the received class file. The flow begins when Bob opens the Web UI and switches to the decode mode. He drops the modified class file into the interface. He enters the shared passphrase provided by Alice. Upon clicking the extract payload button, the backend extracts the encrypted bytes from the file. The browser then decrypts the bytes using the passphrase, and the original secret text file is downloaded to Bob's system.

---

## 4. OTHER NON-FUNCTIONAL REQUIREMENTS

### 4.1 Performance Requirements
Injection and extraction operations for typical class files under 100KB and payloads under 10KB should complete in under 2 seconds. In terms of overhead, Attribute Mode increases the file size by the payload size plus a small header. S-Box Mode increases the file size by the payload size plus the code generation overhead, which is approximately 500 to 1000 bytes extra.

### 4.2 Safety and Security Requirements
The system adheres to a Zero Knowledge principle, meaning the server has no knowledge of the payload content. Integrity is ensured via AES-GCM tags which prevent tampering with the encrypted payload. Stealth is maintained by ensuring the modified class file remains fully functional and passes standard JVM verification.

### 4.3 Software Quality Attributes
Reliability is a key quality attribute, ensuring the system does not corrupt the carrier class logic. Maintainability is achieved by separating the injector, extractor, and api modules for modular development.

---

## 5. OTHER REQUIREMENTS
The project is licensed for educational use only. Compliance requirements dictate that users must ensure they have permission to modify the target bytecode before using the system.

---

## APPENDIX A – DATA DICTIONARY
GhostPayload is the name of the custom attribute used in Attribute Mode. An SBox is a lookup table used in symmetric key algorithms, and in this context, we generate fake S-Boxes to store data. classBytes refers to the byte array representing the content of a class file.
