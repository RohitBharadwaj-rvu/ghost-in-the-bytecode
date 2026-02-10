# ByteStego

> 🔬 **Steganographic Payload Embedding in JVM Bytecode**

ByteStego is a framework for steganographic embedding of encrypted payloads within structurally valid Java `.class` files. Carrier classes remain fully executable by the JVM, while the encrypted payload is concealed within custom class-level attributes.

## ✨ Core Features

- 🔒 **Client-Side Encryption:** AES-256-GCM via Web Crypto API. The backend never sees plaintext or passphrase.
- 💉 **Custom Attribute Embedding:** Payloads are stored in a JVM-compliant custom class attribute.
- ☕ **Carrier Execution:** Modified classes run normally without JVM verification errors.
- 🔍 **Extraction & Recovery:** Standalone module for payload recovery and validation.
- 🎨 **Web Interface:** Dark-themed UI with drag-and-drop support.

---

## 🚀 Getting Started

### 1. Prerequisites
- **Java 17+** (JDK)
- **Maven 3.8+**

### 2. Build the Project
Clone the repository and build all modules:
```bash
cd backend
mvn clean install
```

### 3. Run the Backend API
Start the Spring Boot server:
```bash
cd api
mvn spring-boot:run
```
The server will start at `http://localhost:8080`.

### 4. Launch the UI
Simply open `frontend/index.html` in any modern web browser.

---

## 🛠️ Project Structure

- `frontend/`: Web interface and client-side cryptographic module.
- `backend/`:
  - `injector/`: Bytecode-level payload embedding (ASM-based).
  - `extractor/`: Standalone payload extraction and recovery.
  - `validator/`: Structural and runtime bytecode validation.
  - `api/`: Spring Boot REST API for web integration.

---

## 📐 Architecture Diagrams

See [docs/architecture.md](docs/architecture.md) for UML diagrams (Use Case, Sequence, and Class diagrams) rendered with Mermaid.

---

## 🛡️ Security Model

1. **Confidentiality:** Payloads are encrypted with AES-256-GCM using a key derived via PBKDF2 (100k iterations).
2. **Integrity:** GCM's authentication tag ensures the payload hasn't been tampered with.
3. **Zero Trust:** Encryption happens locally in your browser; only the encrypted bytes are sent to the backend.

---

## ⚖️ Usage
This tool is for educational and research purposes only. Ensure you have permission before modifying any third-party bytecode.
