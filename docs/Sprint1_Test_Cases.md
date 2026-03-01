# Software Testing Lab Report: Sprint 1
**Project**: Ghost in the Bytecode
**Date**: February 26, 2026
**Team**: INNOVATORS HUB
**Submission Type**: Detailed Test Case Documentation

---

## 1. SANITY TESTING
*Objective: To verify that the basic functionality of the software application works after minor changes or bug fixes.*

### Test Case: SAN-01 - Valid Carrier File Upload
- **Objective**: Verify that the system accepts a standard Java class file.
- **Pre-conditions**: Web UI is open; a valid `.class` file is available on the local system.
- **Steps**:
  1. Navigate to the injection interface.
  2. Drag and drop `Example.class` into the Carrier upload area.
  3. Observe UI feedback.
- **Expected Result**: The file name is displayed in the upload area, and no error message appears.
- **Status**: Pass (Verified in Day 3 demo)

### Test Case: SAN-02 - UI Success Notification
- **Objective**: Ensure the user receives clear confirmation after a successful action.
- **Pre-conditions**: File has been successfully uploaded/processed.
- **Steps**:
  1. Upload a valid carrier.
  2. Trigger the "Initiate Sequence" button.
- **Expected Result**: A clear "Upload successful" or "Success" message is displayed on the UI (Ref: Day 6 fix).
- **Status**: Pass

### Test Case: SAN-03 - Lower Bound Size Acceptance (1MB)
- **Objective**: Verify the system handles files at the minimum established boundary.
- **Pre-conditions**: A valid `.class` file of exactly 1MB size.
- **Steps**:
  1. Select a 1MB file for upload.
  2. Click upload.
- **Expected Result**: System processes the file without flagging it as too small or failing.
- **Status**: Pass

### Test Case: SAN-04 - Upper Bound Size Acceptance (10MB)
- **Objective**: Verify the system handles files at the maximum established boundary.
- **Pre-conditions**: A valid `.class` file of exactly 10MB size.
- **Steps**:
  1. Select a 10MB file for upload.
  2. Trigger the upload process.
- **Expected Result**: System accepts the file and completes validation.
- **Status**: Pass (Ref: Saanvi's acceptance check Day 3)

### Test Case: SAN-05 - Drag-and-Drop Interaction
- **Objective**: Verify the usability of the drag-and-drop file interface.
- **Pre-conditions**: Browser window is active.
- **Steps**:
  1. Drag a `.class` file from a folder directly onto the dashed upload border.
  2. Release the mouse button.
- **Expected Result**: The interface highlights during the drag over and correctly registers the dropped file.
- **Status**: Pass

---

## 2. SECURITY TESTING
*Objective: To evaluate the software for vulnerabilities, potential attacks, and data breaches.*

### Test Case: SEC-01 - Extension Filter Enforcement
- **Objective**: Prevent non-Java class files from being uploaded as carriers.
- **Pre-conditions**: Access to `.txt`, `.png`, and `.exe` files.
- **Steps**:
  1. Attempt to upload `secret.txt`.
  2. Attempt to upload `image.png`.
- **Expected Result**: System displays an "Invalid file type" error and blocks the upload (Ref: BF-03).
- **Status**: Pass

### Test Case: SEC-02 - Corrupted Bytecode Fingerprinting
- **Objective**: Ensure the system rejects files that do not follow Java class structural rules.
- **Pre-conditions**: A file renamed to `.class` but containing random binary data.
- **Steps**:
  1. Upload the corrupted binary file.
  2. Trigger validation.
- **Expected Result**: System returns HTTP 422 (Unprocessable Entity) and indicates the file is not a valid class (Ref: Day 5 integration).
- **Status**: Pass

### Test Case: SEC-03 - Magic Number Validation (0xCAFEBABE)
- **Objective**: Verify the backend checks the first 4 bytes of every carrier.
- **Pre-conditions**: A file that has a `.class` extension but lacks the `CAFEBABE` header.
- **Steps**:
  1. Upload the masquerading file.
- **Expected Result**: The `ClassFileReader` or `BytecodeValidator` fails early and reports a "Not a valid Java file" error.
- **Status**: Pass

### Test Case: SEC-04 - Filename Path Traversal Sanitization
- **Objective**: Prevent attackers from using filenames to overwrite server-side files.
- **Pre-conditions**: A filename crafted with traversal characters (e.g., `../../../etc/passwd.class`).
- **Steps**:
  1. Upload a file with a malicious name.
- **Expected Result**: The system strips directory indicators and processes only the base filename (Ref: Day 4 fix).
- **Status**: Pass

### Test Case: SEC-05 - Client-Side Encryption Integrity (Zero-Knowledge)
- **Objective**: Confirm that plaintext payloads never reach the server logs or memory.
- **Pre-conditions**: Network proxy (e.g., Burp Suite) to intercept traffic.
- **Steps**:
  1. Enter a secret message in the payload field.
  2. Click "Initiate".
  3. Inspect the outgoing POST request.
- **Expected Result**: The payload field contains only encrypted cipher-text; the passphrase is never sent (NFR 2.1).
- **Status**: Pass

---

## 3. STRESS TESTING
*Objective: To check the system's stability and error handling under extreme conditions or loads.*

### Test Case: STR-01 - Extreme File Size Rejection (15MB+)
- **Objective**: Prevent memory exhaustion attacks using very large files.
- **Pre-conditions**: A `.class` file larger than 15MB.
- **Steps**:
  1. Attempt to upload the 15MB file.
- **Expected Result**: JavaScript blocks the upload immediately with a "File exceeds 10MB limit" warning (Ref: Day 3 Demo).
- **Status**: Pass

### Test Case: STR-02 - Large Payload Embedding (5MB Spike)
- **Objective**: Ensure the steganography engine handles large attributes without crashing the JVM.
- **Pre-conditions**: 5MB random data payload.
- **Steps**:
  1. Inject the 5MB payload into a small carrier class.
  2. Attempt to load the resulting class using a ClassLoader.
- **Expected Result**: System completes injection; although exceeding "safe" limits, the process does not throw an OutOfMemoryError (Ref: Saket's Spike Day 4).
- **Status**: Pass

### Test Case: STR-03 - Rapid Concurrent Requests
- **Objective**: Ensure the server handles bursts of traffic without dropping requests.
- **Pre-conditions**: Automation script to send 10 concurrent upload requests.
- **Steps**:
  1. Execute the burst script.
- **Expected Result**: All 10 requests are queued and processed successfully; no 503 Service Unavailable errors.
- **Status**: Pass

### Test Case: STR-04 - Null/Empty File Submission
- **Objective**: Verify robust handling of empty input streams.
- **Pre-conditions**: A 0-byte `.class` file.
- **Steps**:
  1. Select and upload the empty file.
- **Expected Result**: System returns "Empty file provided" error; does not result in a NullPointerException.
- **Status**: Pass (Ref: Rohit A's test Day 3).

### Test Case: STR-05 - JVM Attribute Length Limits
- **Objective**: Determine the system behavior when hitting hard JVM attribute limits.
- **Pre-conditions**: Payload size > 64KB.
- **Steps**:
  1. Inject a payload larger than 64KB.
  2. Verify if standard JVM tools (like `javap`) can still read the file.
- **Expected Result**: System warns that payloads > 64KB might not be compatible with all tools, but structurally valid bytecode is produced (Ref: Sprint Review Finding).
- **Status**: Pass (Warning logged).

---

## 4. PERFORMANCE TESTING
*Objective: To measure the responsiveness, speed, and stability under expected workloads.*

### Test Case: PER-01 - Standard Injection Latency
- **Objective**: Verify injection meets the < 2 second requirement.
- **Pre-conditions**: Carrier < 100KB, Payload < 10KB.
- **Steps**:
  1. Start timer.
  2. Perform injection.
  3. Stop timer.
- **Expected Result**: Total time from upload click to download ready is < 2.0 seconds (NFR 1.1).
- **Status**: Pass

### Test Case: PER-02 - Frontend Visual Responsiveness
- **Objective**: Confirm the UI provides immediate visual status to avoid "dead" interface.
- **Pre-conditions**: Slow network simulation (Throttle to 3G).
- **Steps**:
  1. Trigger an upload.
- **Expected Result**: A loading indicator or progress bar appears within 100ms of user input (NFR 6.1).
- **Status**: Pass

### Test Case: PER-03 - Throughput Under Load (50 Requests)
- **Objective**: Verify the system maintains performance under heavy concurrent users.
- **Pre-conditions**: Load balancer environment simulated.
- **Steps**:
  1. Sustain 50 concurrent validation requests using JMeter.
- **Expected Result**: Average response time remains under 2.5 seconds (NFR 1.2).
- **Status**: Pass

### Test Case: PER-04 - Bytecode Bloat Factor (Attribute Mode)
- **Objective**: Measure the efficiency of the "GhostPayload" attribute injection.
- **Pre-conditions**: 1000-byte payload.
- **Steps**:
  1. Compare size of original class vs. injected class.
- **Expected Result**: Size difference is exactly 1000 bytes + attribute header (approx 8-12 bytes) (NFR 1.3).
- **Status**: Pass

### Test Case: PER-05 - Long-term Availability (UptimeRobot)
- **Objective**: Verify the system's availability monitoring is active.
- **Pre-conditions**: Application deployed to staging.
- **Steps**:
  1. Check UptimeRobot dashboard over a 24-hour period.
- **Expected Result**: Availability recorded at 99.5% or higher (NFR 6.2).
- **Status**: Pass
