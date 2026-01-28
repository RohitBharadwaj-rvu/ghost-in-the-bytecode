# Encryption Model

## Algorithm
AES-GCM (256-bit)

## Key Derivation
- User-provided passphrase
- PBKDF2 with salt
- Iteration count >= 100,000

## Output Format
| Field | Description |
|------|------------|
| Salt | Random      |
| IV   | Random      |
| Data | Ciphertext |
| Tag  | Auth tag   |

## Error Handling
- Authentication failure aborts decryption
- No partial output
