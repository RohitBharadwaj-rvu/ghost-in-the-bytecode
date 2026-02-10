# Ghost in the Bytecode

> 👻 **Covert Payload Injection & Encryption for Java Class Files**

Ghost in the Bytecode is a security-focused tool that allows you to encrypt arbitrary files and embed them inside valid, runnable Java `.class` files. The carrier classes remain fully executable by the JVM, while the encrypted payload remains hidden in a custom class attribute.

## ✨ Core Features

- 🔒 **Client-Side Encryption:** AES-256-GCM (Web Crypto API). The backend never sees your plaintext or passphrase.
- 💉 **Custom Attribute Injection:** Payloads are stored in a JVM-compliant `GhostPayload` attribute.
- ☕ **Carrier Execution:** Modified classes run normally without JVM verification errors.
- 🕵️ **Extraction & Recovery:** Dedicated standalone module for safe payload recovery.
- 🎨 **Modern Web UI:** Dark-themed interface with drag-and-drop support.

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

- `frontend/`: Web interface, CSS, and `crypto.js` (Web Crypto logic).
- `backend/`:
  - `injector/`: Core logic for bytecode manipulation (ASM-based).
  - `extractor/`: Standalone logic for payload recovery.
  - `validator/`: Structural and runtime validation of modified classes.
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
