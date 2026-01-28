package ghost.injector;

/**
 * Injection mode for hiding payloads in class files.
 */
public enum InjectionMode {
    /**
     * Current approach: Custom class-level attribute (GhostPayload).
     * - Smaller overhead
     * - May be flagged as "unknown attribute" by tools
     */
    ATTRIBUTE,

    /**
     * Stealth approach: Disguise payload as cryptographic S-Box table.
     * - Resistant to dead-code elimination
     * - Looks like legitimate crypto code
     * - ~512-1024 byte overhead depending on payload size
     */
    SBOX_SMEAR
}
