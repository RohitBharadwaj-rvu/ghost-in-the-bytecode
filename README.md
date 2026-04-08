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

The application features a fully unified build system and is containerized for seamless deployment. The frontend UI is cleverly baked directly into the Spring Boot backend during compilation.

### 🐳 Quick Start (Docker - Recommended)
The fastest way to run the project on any OS is via Docker Desktop.
```bash
docker-compose up --build -d
```
Then simply navigate to `http://localhost:8080` in your browser.

### ☕ Native Execution (Maven)
If you prefer running without Docker:
1. Ensure **Java 17+** and **Maven 3.8+** are installed.
2. Build the unified project (which also embeds the frontend into the Java API):
```bash
mvn clean install
```
3. Start the Spring Boot backend:
```bash
cd backend/api
mvn spring-boot:run
```
4. Navigate to `http://localhost:8080`. *(Windows users can optionally double-click the `StartGhost.bat` shortcut at the root directory).*

### 📊 Code Quality & DevOps
The deployment pipeline automatically generates **Javadoc**, executes **Checkstyle** (static code analysis), and runs **SpotBugs** (bytecode-level vulnerability scanning) on every build.

## 🛠️ Project Structure

- `frontend/`: Web interface and client-side cryptographic module.
- `backend/`:
  - `injector/`: Bytecode-level payload embedding (ASM-based).
  - `extractor/`: Standalone payload extraction and recovery.
  - `validator/`: Structural and runtime bytecode validation.
  - `api/`: Spring Boot REST API for web integration.

---

## 📐 Architecture Diagrams

## 📐 Architecture Diagrams

See [docs/architecture.md](docs/architecture.md) for detailed UML diagrams rendered with Mermaid:
- **Architecture**: Component Diagram & Class Diagram
- **Model**: Payload Lifecycle State Diagram
- **Design**: S-Box Embedding Algorithm Flowchart
- **Interaction**: Use Case & Sequence Diagrams

---

## 🛡️ Security Model

1. **Confidentiality:** Payloads are encrypted with AES-256-GCM using a key derived via PBKDF2 (100k iterations).
2. **Integrity:** GCM's authentication tag ensures the payload hasn't been tampered with.
3. **Zero Trust:** Encryption happens locally in your browser; only the encrypted bytes are sent to the backend.

---

## ⚖️ Usage
This tool is for educational and research purposes only. Ensure you have permission before modifying any third-party bytecode.
