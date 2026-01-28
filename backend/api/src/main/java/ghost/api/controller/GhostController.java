package ghost.api.controller;

import ghost.extractor.ExtractionResult;
import ghost.extractor.PayloadExtractor;
import ghost.injector.GhostPayloadInjector;
import ghost.injector.InjectionMode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class GhostController {

    private final GhostPayloadInjector injector;
    private final PayloadExtractor extractor;

    public GhostController() {
        this.injector = new GhostPayloadInjector();
        this.extractor = new PayloadExtractor();
    }

    @PostMapping("/inject")
    public ResponseEntity<byte[]> inject(
            @RequestParam("carrierClass") MultipartFile carrierClass,
            @RequestParam("encryptedPayload") MultipartFile encryptedPayload,
            @RequestParam(value = "mode", defaultValue = "ATTRIBUTE") String modeParam) {
        try {
            // Validation
            if (carrierClass.isEmpty() || encryptedPayload.isEmpty()) {
                return ResponseEntity.badRequest().body("Files cannot be empty".getBytes());
            }

            // Parse injection mode
            InjectionMode mode;
            try {
                mode = InjectionMode.valueOf(modeParam.toUpperCase());
            } catch (IllegalArgumentException e) {
                mode = InjectionMode.ATTRIBUTE;
            }

            // Read bytes
            byte[] classBytes = carrierClass.getBytes();
            byte[] payloadBytes = encryptedPayload.getBytes();

            // Inject with selected mode
            byte[] modifiedClass = injector.inject(classBytes, payloadBytes, mode);

            // Return modified class
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"modified.class\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(modifiedClass);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error processing files: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Injection failed: " + e.getMessage()).getBytes());
        }
    }

    @PostMapping("/extract")
    public ResponseEntity<byte[]> extract(
            @RequestParam("modifiedClass") MultipartFile modifiedClass) {
        try {
            // Validation
            if (modifiedClass.isEmpty()) {
                return ResponseEntity.badRequest().body("File cannot be empty".getBytes());
            }

            // Read bytes
            byte[] classBytes = modifiedClass.getBytes();

            // Extract
            ExtractionResult result = extractor.extract(classBytes);

            if (!result.success()) {
                String errorMessage = "Extraction failed: " + result.errorType();
                if (result.errorMessage() != null) {
                    errorMessage += " (" + result.errorMessage() + ")";
                }
                return ResponseEntity.badRequest().body(errorMessage.getBytes());
            }

            // Return encrypted payload
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payload.bin\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(result.payload());

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error processing file: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Extraction failed: " + e.getMessage()).getBytes());
        }
    }
}
