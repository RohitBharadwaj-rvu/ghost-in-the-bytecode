/**
 * Ghost in the Bytecode - Client-Side Encryption Module
 * 
 * Security: AES-256-GCM with PBKDF2 key derivation
 * All operations are binary-safe (Uint8Array only)
 * Backend never sees plaintext
 */

const GhostCrypto = (function() {
    'use strict';

    // Constants
    const VERSION = 1;
    const SALT_LENGTH = 16;
    const IV_LENGTH = 12;
    const PBKDF2_ITERATIONS = 100000;
    const KEY_LENGTH_BITS = 256;

    /**
     * Derives a 256-bit AES key from passphrase using PBKDF2-HMAC-SHA256.
     * 
     * @param {string} passphrase - User-provided passphrase
     * @param {Uint8Array} salt - 16-byte random salt
     * @returns {Promise<CryptoKey>} AES-GCM key
     */
    async function deriveKey(passphrase, salt) {
        // Import passphrase as key material
        const encoder = new TextEncoder();
        const keyMaterial = await crypto.subtle.importKey(
            'raw',
            encoder.encode(passphrase),
            'PBKDF2',
            false,
            ['deriveBits']
        );

        // Derive 256 bits using PBKDF2-HMAC-SHA256
        const derivedBits = await crypto.subtle.deriveBits(
            {
                name: 'PBKDF2',
                salt: salt,
                iterations: PBKDF2_ITERATIONS,
                hash: 'SHA-256'
            },
            keyMaterial,
            KEY_LENGTH_BITS
        );

        // Import derived bits as AES-GCM key
        return crypto.subtle.importKey(
            'raw',
            derivedBits,
            'AES-GCM',
            false,
            ['encrypt', 'decrypt']
        );
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * 
     * Output format: [Version:1][Salt:16][IV:12][Ciphertext+AuthTag:N]
     * 
     * @param {Uint8Array} plaintext - Data to encrypt
     * @param {string} passphrase - Encryption passphrase
     * @returns {Promise<Uint8Array>} Encrypted payload
     */
    async function encrypt(plaintext, passphrase) {
        if (!(plaintext instanceof Uint8Array)) {
            throw new TypeError('Plaintext must be Uint8Array');
        }
        if (typeof passphrase !== 'string' || passphrase.length === 0) {
            throw new TypeError('Passphrase must be non-empty string');
        }

        // Generate random salt and IV
        const salt = crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
        const iv = crypto.getRandomValues(new Uint8Array(IV_LENGTH));

        // Derive key from passphrase
        const key = await deriveKey(passphrase, salt);

        // Encrypt using AES-GCM (auth tag is automatically appended)
        const ciphertext = await crypto.subtle.encrypt(
            {
                name: 'AES-GCM',
                iv: iv
            },
            key,
            plaintext
        );

        // Combine: [Version][Salt][IV][Ciphertext+AuthTag]
        const result = new Uint8Array(1 + SALT_LENGTH + IV_LENGTH + ciphertext.byteLength);
        result[0] = VERSION;
        result.set(salt, 1);
        result.set(iv, 1 + SALT_LENGTH);
        result.set(new Uint8Array(ciphertext), 1 + SALT_LENGTH + IV_LENGTH);

        return result;
    }

    /**
     * Decrypts payload using AES-256-GCM.
     * 
     * On authentication failure (wrong passphrase or tampered data),
     * this function THROWS - it does NOT return partial or empty data.
     * 
     * @param {Uint8Array} encryptedPayload - Data to decrypt
     * @param {string} passphrase - Decryption passphrase
     * @returns {Promise<Uint8Array>} Decrypted plaintext
     * @throws {Error} On authentication failure or invalid format
     */
    async function decrypt(encryptedPayload, passphrase) {
        if (!(encryptedPayload instanceof Uint8Array)) {
            throw new TypeError('Encrypted payload must be Uint8Array');
        }
        if (typeof passphrase !== 'string' || passphrase.length === 0) {
            throw new TypeError('Passphrase must be non-empty string');
        }

        const minLength = 1 + SALT_LENGTH + IV_LENGTH + 16; // +16 for auth tag
        if (encryptedPayload.length < minLength) {
            throw new Error('Encrypted payload too short');
        }

        // Parse version
        const version = encryptedPayload[0];
        if (version !== VERSION) {
            throw new Error(`Unsupported version: ${version}`);
        }

        // Parse salt, IV, and ciphertext
        const salt = encryptedPayload.slice(1, 1 + SALT_LENGTH);
        const iv = encryptedPayload.slice(1 + SALT_LENGTH, 1 + SALT_LENGTH + IV_LENGTH);
        const ciphertext = encryptedPayload.slice(1 + SALT_LENGTH + IV_LENGTH);

        // Derive key from passphrase
        const key = await deriveKey(passphrase, salt);

        // Decrypt using AES-GCM
        // This THROWS if authentication fails (wrong passphrase or tampered)
        const plaintext = await crypto.subtle.decrypt(
            {
                name: 'AES-GCM',
                iv: iv
            },
            key,
            ciphertext
        );

        return new Uint8Array(plaintext);
    }

    /**
     * Utility: Convert string to Uint8Array (UTF-8)
     */
    function stringToBytes(str) {
        return new TextEncoder().encode(str);
    }

    /**
     * Utility: Convert Uint8Array to string (UTF-8)
     */
    function bytesToString(bytes) {
        return new TextDecoder().decode(bytes);
    }

    // Public API
    return {
        encrypt,
        decrypt,
        stringToBytes,
        bytesToString,
        VERSION,
        SALT_LENGTH,
        IV_LENGTH,
        PBKDF2_ITERATIONS
    };
})();

// Export for Node.js environments (testing)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = GhostCrypto;
}
