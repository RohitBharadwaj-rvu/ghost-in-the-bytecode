/**
 * GHOST PROTOCOL v2.0 - Dashboard Logic
 */

const GhostApp = (function () {
    'use strict';

    // Configuration
    const API_BASE = 'http://localhost:8080';

    // UI State
    let currentMode = 'inject';
    let payloadFile = null;
    let carrierFile = null;
    let modifiedFile = null;

    // DOM Elements Cache
    const el = {};

    function init() {
        cacheElements();
        bindEvents();
        initBackground();
        log('SYSTEM_INIT', 'Ghost Protocol v2.0 initialized...', 'info');
    }

    function cacheElements() {
        // Nav
        el.navBtns = document.querySelectorAll('.nav-btn');
        el.injectWorkspace = document.getElementById('inject-mode');
        el.extractWorkspace = document.getElementById('extract-mode');
        el.modeLabel = document.getElementById('current-mode-label');

        // Inject Inputs
        el.payloadDrop = document.getElementById('payload-drop');
        el.carrierDrop = document.getElementById('carrier-drop');
        el.injectPass = document.getElementById('inject-passphrase');
        el.injectBtn = document.getElementById('inject-btn');
        el.stealthRadios = document.querySelectorAll('input[name="injection-mode"]');

        // Extract Inputs
        el.modifiedDrop = document.getElementById('modified-drop');
        el.extractPass = document.getElementById('extract-passphrase');
        el.extractBtn = document.getElementById('extract-btn');

        // Terminal
        el.terminal = document.getElementById('terminal');
        el.termLogs = document.getElementById('term-logs');
        el.termClose = document.getElementById('term-close');
        el.termActions = document.getElementById('result-actions');
        el.downloadLink = document.getElementById('download-link');

        // Global
        el.globalStatus = document.getElementById('global-status');
        el.visToggles = document.querySelectorAll('.toggle-vis-v2');
        el.fileRemoves = document.querySelectorAll('.file-remove');
    }

    function bindEvents() {
        // Navigation
        el.navBtns.forEach(btn => {
            btn.addEventListener('click', () => switchMode(btn.dataset.mode));
        });

        // Inject Flow
        setupDropzone('payload', el.payloadDrop, (f) => { payloadFile = f; validateInject(); });
        setupDropzone('carrier', el.carrierDrop, (f) => { carrierFile = f; validateInject(); });

        el.injectPass.addEventListener('input', validateInject);
        el.injectBtn.addEventListener('click', handleInject);

        // Extract Flow
        setupDropzone('modified', el.modifiedDrop, (f) => { modifiedFile = f; validateExtract(); });

        el.extractPass.addEventListener('input', validateExtract);
        el.extractBtn.addEventListener('click', handleExtract);

        // Terminal
        el.termClose.addEventListener('click', closeTerminal);

        // Utils
        el.visToggles.forEach(btn => {
            btn.addEventListener('click', (e) => {
                const input = document.getElementById(btn.dataset.target);
                input.type = input.type === 'password' ? 'text' : 'password';
                btn.textContent = input.type === 'password' ? '👁' : '◎';
            });
        });

        el.fileRemoves.forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                clearFile(btn.dataset.target);
            });
        });
    }

    // --- Core Logic ---

    function switchMode(mode) {
        currentMode = mode;

        // Update Nav
        el.navBtns.forEach(btn => {
            btn.classList.toggle('active', btn.dataset.mode === mode);
        });

        // Update Workspace
        el.injectWorkspace.classList.toggle('hidden', mode !== 'inject');
        el.extractWorkspace.classList.toggle('hidden', mode !== 'extract');
        el.injectWorkspace.classList.toggle('active', mode === 'inject');

        // Update Header
        el.modeLabel.textContent = mode === 'inject' ? 'ENCODE_PAYLOAD' : 'EXTRACT_PAYLOAD';

        closeTerminal();
    }

    async function handleInject() {
        if (el.injectBtn.disabled) return;

        openTerminal();
        log('PROCESS_START', 'Initiating injection sequence...', 'info');

        try {
            // 1. Read files
            log('READ_IO', `Reading payload: ${payloadFile.name} (${formatSize(payloadFile.size)})`);
            const payloadBytes = await readFile(payloadFile);

            log('READ_IO', `Reading carrier: ${carrierFile.name}`);
            const carrierBytes = await readFile(carrierFile);

            // 2. Encrypt
            log('CRYPTO', 'Deriving key (PBKDF2) & Encrypting payload (AES-GCM)...', 'info');
            await delay(400); // UI feel
            const passphrase = el.injectPass.value;
            const encryptedPayload = await GhostCrypto.encrypt(payloadBytes, passphrase);
            log('CRYPTO', 'Encryption complete. Payload secured.', 'success');

            // 3. Inject
            const mode = document.querySelector('input[name="injection-mode"]:checked').value;
            const modeName = mode === 'SBOX_SMEAR' ? 'GHOST_SBOX (L2)' : 'ATTRIBUTE (L1)';

            log('INJECT', `Injecting via ${modeName} protocol...`, 'info');

            const formData = new FormData();
            formData.append('carrierClass', new Blob([carrierBytes]), 'carrier.class');
            formData.append('encryptedPayload', new Blob([encryptedPayload]), 'payload.bin');
            formData.append('mode', mode);

            const res = await fetch(`${API_BASE}/inject`, { method: 'POST', body: formData });

            if (!res.ok) throw new Error(await res.text());

            const modifiedBytes = new Uint8Array(await res.arrayBuffer());
            log('INJECT', 'Bytecode injection successful.', 'success');

            // 4. Finish
            const outName = carrierFile.name.replace('.class', '_ghost.class');
            presentDownload(modifiedBytes, outName);
            log('COMPLETE', 'Artifact ready for download.', 'success');

        } catch (err) {
            log('ERROR', err.message, 'error');
        }
    }

    async function handleExtract() {
        if (el.extractBtn.disabled) return;

        openTerminal();
        log('PROCESS_START', 'Initiating extraction sequence...', 'info');

        try {
            // 1. Read
            log('READ_IO', `Analyzing artifact: ${modifiedFile.name}`);
            const classBytes = await readFile(modifiedFile);

            // 2. Extract
            log('EXTRACT', 'Scanning bytecode for signatures...', 'info');
            const formData = new FormData();
            formData.append('modifiedClass', new Blob([classBytes]), 'modified.class');

            const res = await fetch(`${API_BASE}/extract`, { method: 'POST', body: formData });
            if (!res.ok) throw new Error(await res.text());

            const encryptedPayload = new Uint8Array(await res.arrayBuffer());
            log('EXTRACT', 'Payload recovered. Attempting decryption...', 'success');

            // 3. Decrypt
            const passphrase = el.extractPass.value;
            const decryptedPayload = await GhostCrypto.decrypt(encryptedPayload, passphrase);
            log('CRYPTO', 'Decryption successful. Integrity verified.', 'success');

            // 4. Finish
            presentDownload(decryptedPayload, 'recovered_payload.bin');

        } catch (err) {
            if (err.message.includes('decrypt') || err.name === 'OperationError') {
                log('ERROR', 'Decryption failed: Invalid passphrase or corrupted data', 'error');
            } else {
                log('ERROR', err.message, 'error');
            }
        }
    }

    // --- UI Helpers ---

    function setupDropzone(type, zone, cb) {
        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.style.display = 'none';
        if (type === 'carrier' || type === 'modified') fileInput.accept = '.class';
        document.body.appendChild(fileInput);

        zone.addEventListener('click', () => fileInput.click());

        zone.addEventListener('dragover', (e) => {
            e.preventDefault();
            zone.classList.add('dragover');
        });

        zone.addEventListener('dragleave', () => {
            zone.classList.remove('dragover');
        });

        zone.addEventListener('drop', (e) => {
            e.preventDefault();
            zone.classList.remove('dragover');
            if (e.dataTransfer.files[0]) cb(e.dataTransfer.files[0]);
        });

        fileInput.addEventListener('change', () => {
            if (fileInput.files[0]) cb(fileInput.files[0]);
        });

        // Update UI when file selected
        const originalCb = cb;
        cb = (file) => {
            updateFileInfo(type, file);
            originalCb(file);
        };
    }

    function updateFileInfo(type, file) {
        const zone = document.getElementById(`${type}-drop`);
        const info = document.getElementById(`${type}-info`);
        const nameEl = document.getElementById(`${type}-name`);
        const sizeEl = document.getElementById(`${type}-size`);

        zone.querySelector('.dz-content').classList.add('hidden'); // Hide default content
        zone.querySelector('.dz-content').style.opacity = '0'; // Helper 

        info.classList.remove('hidden');
        zone.classList.add('has-file');

        nameEl.textContent = file.name;
        sizeEl.textContent = formatSize(file.size);
    }

    function clearFile(type) {
        const zone = document.getElementById(`${type}-drop`);
        const info = document.getElementById(`${type}-info`);

        info.classList.add('hidden');
        zone.classList.remove('has-file');
        zone.querySelector('.dz-content').style.opacity = '1';
        zone.querySelector('.dz-content').classList.remove('hidden');

        if (type === 'payload') { payloadFile = null; validateInject(); }
        if (type === 'carrier') { carrierFile = null; validateInject(); }
        if (type === 'modified') { modifiedFile = null; validateExtract(); }
    }

    function validateInject() {
        const valid = payloadFile && carrierFile && el.injectPass.value.length > 0;
        el.injectBtn.disabled = !valid;
        if (valid) el.injectBtn.querySelector('.glitch-text').textContent = "INITIATE_SEQUENCE";
    }

    function validateExtract() {
        const valid = modifiedFile && el.extractPass.value.length > 0;
        el.extractBtn.disabled = !valid;
    }

    // --- Terminal & Logs ---

    function openTerminal() {
        el.terminal.classList.add('open');
        el.termLogs.innerHTML = '';
        el.termActions.classList.add('hidden');
    }

    function closeTerminal() {
        el.terminal.classList.remove('open');
    }

    function log(tag, msg, type = 'normal') {
        const line = document.createElement('div');
        line.className = `log-line ${type}`;

        const timestamp = new Date().toLocaleTimeString('en-US', { hour12: false });
        line.textContent = `[${timestamp}] [${tag}] ${msg}`;

        el.termLogs.appendChild(line);
        el.termLogs.scrollTop = el.termLogs.scrollHeight;
    }

    function presentDownload(data, filename) {
        const blob = new Blob([data]);
        const url = URL.createObjectURL(blob);
        el.downloadLink.href = url;
        el.downloadLink.download = filename;
        el.termActions.classList.remove('hidden');
    }

    // --- Utils ---

    function readFile(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(new Uint8Array(reader.result));
            reader.onerror = () => reject(new Error('File read error'));
            reader.readAsArrayBuffer(file);
        });
    }

    function formatSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / 1048576).toFixed(1) + ' MB';
    }

    function delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    function initBackground() {
        const canvas = document.getElementById('bg-canvas');
        const ctx = canvas.getContext('2d');

        let width, height;

        // Strange Attractor Parameters (Peter de Jong)
        // x' = sin(a * y) - cos(b * x)
        // y' = sin(c * x) - cos(d * y)
        // We will drift these parameters slowly and influence them with mouse
        let a = 1.4, b = -2.3, c = 2.4, d = -2.1;

        // Configuration
        const TRAIL_STRENGTH = 0.08; // Lower = longer trails (more mesmerizing)
        const MOUSE_GRAVITY = 0.15; // How strongly particles are pulled to mouse
        const CENTER_FOLLOW_SPEED = 0.08; // How fast the cloud follows the mouse

        const mouse = { x: 0, y: 0 };
        const center = { x: 0, y: 0 };
        const activeCenter = { x: 0, y: 0 }; // Interpolated center that follows mouse
        let particles = [];

        class Particle {
            constructor() {
                this.reset();
            }

            reset() {
                this.x = (Math.random() - 0.5) * 4;
                this.y = (Math.random() - 0.5) * 4;
                this.vx = 0;
                this.vy = 0;
                this.life = Math.random() * 200 + 100;
                this.age = 0;
            }

            update(mouseAttractorSpace) {
                // Calculate attractor force field at current position
                const tx = Math.sin(a * this.y) - Math.cos(b * this.x);
                const ty = Math.sin(c * this.x) - Math.cos(d * this.y);

                // Fluid motion: accelerate towards attractor target
                const dx = tx - this.x;
                const dy = ty - this.y;

                this.vx += dx * 0.005;
                this.vy += dy * 0.005;

                // MOUSE GRAVITY WELL: Pull particles towards mouse position
                const mx = mouseAttractorSpace.x - this.x;
                const my = mouseAttractorSpace.y - this.y;
                const distToMouse = Math.sqrt(mx * mx + my * my);

                if (distToMouse < 3) { // Within influence radius
                    const pull = (1 - distToMouse / 3) * MOUSE_GRAVITY;
                    this.vx += mx * pull * 0.1;
                    this.vy += my * pull * 0.1;
                }

                // Drag/Friction to stabilize
                this.vx *= 0.94;
                this.vy *= 0.94;

                this.x += this.vx;
                this.y += this.vy;

                // Age recycling
                this.age++;
                if (this.age > this.life || Math.abs(this.x) > 10 || Math.abs(this.y) > 10) {
                    this.reset();
                }
            }

            draw() {
                // Map attractor space (-2 to 2) to screen space
                // Use activeCenter which follows the mouse!
                const scale = width > height ? height / 5 : width / 5;
                const sx = activeCenter.x + this.x * scale;
                const sy = activeCenter.y + this.y * scale;

                // Dynamic color based on velocity
                const speed = Math.sqrt(this.vx * this.vx + this.vy * this.vy);
                const alpha = Math.min(1, speed * 3) * 0.5; // Brighter when moving fast

                ctx.fillStyle = `rgba(0, 255, 230, ${alpha})`;
                ctx.fillRect(sx, sy, 1.5, 1.5);
            }
        }

        function resize() {
            width = window.innerWidth;
            height = window.innerHeight;
            canvas.width = width;
            canvas.height = height;
            center.x = width / 2;
            center.y = height / 2;
            activeCenter.x = center.x;
            activeCenter.y = center.y;
            mouse.x = center.x;
            mouse.y = center.y;

            // Scaled particle count based on resolution
            particles = [];
            const count = Math.min(5000, Math.floor((width * height) / 300));
            for (let i = 0; i < count; i++) particles.push(new Particle());
        }

        window.addEventListener('resize', resize);
        window.addEventListener('mousemove', e => {
            const rect = canvas.getBoundingClientRect();
            mouse.x = e.clientX - rect.left;
            mouse.y = e.clientY - rect.top;
        });

        resize();

        let time = 0;

        function animate() {
            // Trail effect: Fade out existing canvas instead of pure clear
            ctx.fillStyle = `rgba(0, 0, 0, ${TRAIL_STRENGTH})`;
            ctx.fillRect(0, 0, width, height);

            // Additive blending for "glowing" intersections
            ctx.globalCompositeOperation = 'lighter';

            // Evolve parameters slowly for "mesmerizing" drift
            time += 0.002;

            // SMOOTHLY MOVE activeCenter TOWARDS MOUSE (the cloud follows the cursor)
            activeCenter.x += (mouse.x - activeCenter.x) * CENTER_FOLLOW_SPEED;
            activeCenter.y += (mouse.y - activeCenter.y) * CENTER_FOLLOW_SPEED;

            // Mouse Interaction: Morph the attractor math based on cursor X/Y
            const mx = (mouse.x - center.x) / width; // -0.5 to 0.5
            const my = (mouse.y - center.y) / height;

            // Stronger morphing for more dynamic shapes
            a = 1.4 + Math.sin(time) * 0.3 + mx * 3.0;
            b = -2.3 + Math.cos(time * 0.7) * 0.3 + my * 3.0;
            c = 2.4 + Math.sin(time * 1.2) * 0.2;
            d = -2.1 + Math.cos(time * 0.5) * 0.2;

            // Calculate mouse position in attractor space for gravity well
            const scale = width > height ? height / 5 : width / 5;
            const mouseAttractorSpace = {
                x: (mouse.x - activeCenter.x) / scale,
                y: (mouse.y - activeCenter.y) / scale
            };

            particles.forEach(p => {
                p.update(mouseAttractorSpace);
                p.draw();
            });

            // Reset blend mode for clear rect next frame
            ctx.globalCompositeOperation = 'source-over';

            requestAnimationFrame(animate);
        }

        animate();
    }

    document.addEventListener('DOMContentLoaded', init);

    return { init };
})();
