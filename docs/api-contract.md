# API Contract

## POST /inject
### Input
- baseClassFile: binary
- encryptedPayload: binary

### Output
- modifiedClassFile: binary

---

## POST /extract
### Input
- modifiedClassFile: binary

### Output
- encryptedPayload: binary
