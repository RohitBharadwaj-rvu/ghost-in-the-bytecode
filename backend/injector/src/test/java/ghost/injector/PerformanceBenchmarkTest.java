package ghost.injector;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ghost.extractor.PayloadExtractor;
import ghost.extractor.PayloadExtractor.ExtractionResult;

/**
 * Performance benchmarks comparing Attribute-based injection vs S-Box
 * injection.
 * 
 * Measures:
 * - Injection time (payload -> modified class)
 * - Extraction time (modified class -> payload)
 * - Output size overhead
 */
@DisplayName("Performance Benchmarks: Attribute vs S-Box Mode")
public class PerformanceBenchmarkTest {

    private static byte[] carrierClass;
    private static final int WARMUP_ITERATIONS = 10;
    private static final int BENCHMARK_ITERATIONS = 100;

    // Test payloads of varying sizes
    private static final int[] PAYLOAD_SIZES = { 50, 200, 500, 800 };

    @BeforeAll
    static void loadCarrierClass() throws IOException {
        try (InputStream is = PerformanceBenchmarkTest.class.getResourceAsStream("/SimpleCarrier.class")) {
            if (is == null) {
                throw new IOException("SimpleCarrier.class not found in test resources");
            }
            carrierClass = is.readAllBytes();
        }
    }

    @Test
    @DisplayName("Benchmark: Injection Performance Comparison")
    void benchmarkInjectionPerformance() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("INJECTION PERFORMANCE BENCHMARK");
        System.out.println("=".repeat(70));
        System.out.printf("%-12s | %-20s | %-20s | %-10s%n",
                "Payload", "Attribute Mode", "S-Box Mode", "Ratio");
        System.out.println("-".repeat(70));

        for (int size : PAYLOAD_SIZES) {
            byte[] payload = generatePayload(size);

            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.ATTRIBUTE);
                GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.SBOX_SMEAR);
            }

            // Benchmark Attribute mode
            long attrStart = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.ATTRIBUTE);
            }
            long attrTime = System.nanoTime() - attrStart;
            double attrAvgMs = (attrTime / 1_000_000.0) / BENCHMARK_ITERATIONS;

            // Benchmark S-Box mode
            long sboxStart = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.SBOX_SMEAR);
            }
            long sboxTime = System.nanoTime() - sboxStart;
            double sboxAvgMs = (sboxTime / 1_000_000.0) / BENCHMARK_ITERATIONS;

            double ratio = sboxAvgMs / attrAvgMs;

            System.out.printf("%-12s | %-20s | %-20s | %-10s%n",
                    size + " bytes",
                    String.format("%.3f ms", attrAvgMs),
                    String.format("%.3f ms", sboxAvgMs),
                    String.format("%.2fx", ratio));
        }
        System.out.println("=".repeat(70));
    }

    @Test
    @DisplayName("Benchmark: Extraction Performance Comparison")
    void benchmarkExtractionPerformance() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("EXTRACTION PERFORMANCE BENCHMARK");
        System.out.println("=".repeat(70));
        System.out.printf("%-12s | %-20s | %-20s | %-10s%n",
                "Payload", "Attribute Mode", "S-Box Mode", "Ratio");
        System.out.println("-".repeat(70));

        for (int size : PAYLOAD_SIZES) {
            byte[] payload = generatePayload(size);

            // Prepare injected classes
            byte[] attrClass = GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.ATTRIBUTE);
            byte[] sboxClass = GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.SBOX_SMEAR);

            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                PayloadExtractor.extract(attrClass);
                PayloadExtractor.extract(sboxClass);
            }

            // Benchmark Attribute extraction
            long attrStart = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                PayloadExtractor.extract(attrClass);
            }
            long attrTime = System.nanoTime() - attrStart;
            double attrAvgMs = (attrTime / 1_000_000.0) / BENCHMARK_ITERATIONS;

            // Benchmark S-Box extraction
            long sboxStart = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                PayloadExtractor.extract(sboxClass);
            }
            long sboxTime = System.nanoTime() - sboxStart;
            double sboxAvgMs = (sboxTime / 1_000_000.0) / BENCHMARK_ITERATIONS;

            double ratio = sboxAvgMs / attrAvgMs;

            System.out.printf("%-12s | %-20s | %-20s | %-10s%n",
                    size + " bytes",
                    String.format("%.3f ms", attrAvgMs),
                    String.format("%.3f ms", sboxAvgMs),
                    String.format("%.2fx", ratio));
        }
        System.out.println("=".repeat(70));
    }

    @Test
    @DisplayName("Benchmark: Output Size Overhead Comparison")
    void benchmarkSizeOverhead() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("SIZE OVERHEAD BENCHMARK");
        System.out.println("=".repeat(70));
        System.out.printf("%-12s | %-15s | %-15s | %-15s | %-15s%n",
                "Payload", "Original", "Attr Mode", "S-Box Mode", "S-Box Overhead");
        System.out.println("-".repeat(70));

        for (int size : PAYLOAD_SIZES) {
            byte[] payload = generatePayload(size);

            byte[] attrClass = GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.ATTRIBUTE);
            byte[] sboxClass = GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.SBOX_SMEAR);

            int originalSize = carrierClass.length;
            int attrSize = attrClass.length;
            int sboxSize = sboxClass.length;
            int sboxOverhead = sboxSize - attrSize;

            System.out.printf("%-12s | %-15s | %-15s | %-15s | %-15s%n",
                    size + " bytes",
                    originalSize + " bytes",
                    attrSize + " bytes",
                    sboxSize + " bytes",
                    (sboxOverhead >= 0 ? "+" : "") + sboxOverhead + " bytes");
        }
        System.out.println("=".repeat(70));
    }

    @Test
    @DisplayName("Benchmark: Summary Statistics")
    void benchmarkSummary() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("BENCHMARK SUMMARY");
        System.out.println("=".repeat(70));

        List<Double> injectionRatios = new ArrayList<>();
        List<Double> extractionRatios = new ArrayList<>();

        for (int size : PAYLOAD_SIZES) {
            byte[] payload = generatePayload(size);

            // Measure injection
            long attrInjectStart = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.ATTRIBUTE);
            }
            double attrInjectMs = (System.nanoTime() - attrInjectStart) / 1_000_000.0 / 50;

            long sboxInjectStart = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.SBOX_SMEAR);
            }
            double sboxInjectMs = (System.nanoTime() - sboxInjectStart) / 1_000_000.0 / 50;

            injectionRatios.add(sboxInjectMs / attrInjectMs);

            // Measure extraction
            byte[] attrClass = GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.ATTRIBUTE);
            byte[] sboxClass = GhostPayloadInjector.inject(carrierClass, payload, InjectionMode.SBOX_SMEAR);

            long attrExtractStart = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                PayloadExtractor.extract(attrClass);
            }
            double attrExtractMs = (System.nanoTime() - attrExtractStart) / 1_000_000.0 / 50;

            long sboxExtractStart = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                PayloadExtractor.extract(sboxClass);
            }
            double sboxExtractMs = (System.nanoTime() - sboxExtractStart) / 1_000_000.0 / 50;

            extractionRatios.add(sboxExtractMs / attrExtractMs);
        }

        double avgInjectRatio = injectionRatios.stream().mapToDouble(d -> d).average().orElse(0);
        double avgExtractRatio = extractionRatios.stream().mapToDouble(d -> d).average().orElse(0);

        System.out.println("Average S-Box / Attribute Injection Time Ratio: " + String.format("%.2fx", avgInjectRatio));
        System.out
                .println("Average S-Box / Attribute Extraction Time Ratio: " + String.format("%.2fx", avgExtractRatio));
        System.out.println();
        System.out.println("INTERPRETATION:");
        System.out.println("- Attribute Mode: Faster, minimal overhead, but detectable by signature scanners");
        System.out.println("- S-Box Mode: Slight overhead, but payload disguised as cryptographic constants");
        System.out.println("=".repeat(70));
    }

    private byte[] generatePayload(int size) {
        byte[] payload = new byte[size];
        new Random(42).nextBytes(payload); // Fixed seed for reproducibility
        return payload;
    }
}
