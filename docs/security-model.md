# Security Model

## Threat Model
- Backend is untrusted
- Network is untrusted
- Only user possesses decryption key

## Encryption
- AES-256-GCM
- Random IV per encryption
- Authentication tag verified on decrypt

## Guarantees
- Confidentiality of payload
- Integrity detection
- No executable payload risk

## Non-Goals
- Steganographic resistance against forensic JVM analysis
- Malware obfuscation
