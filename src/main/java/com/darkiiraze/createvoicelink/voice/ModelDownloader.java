package com.darkiiraze.createvoicelink.voice;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Auto-downloads the Vosk STT model on first mod load.
 * 
 * Downloads vosk-model-small-en-us-0.15 (~40MB) from alphacephei.com
 * and extracts it to config/createvoicelink/vosk-model/.
 * 
 * Shows status messages to players when they join if download is
 * in progress or completed.
 */
@EventBusSubscriber(modid = CreateVoiceLink.MODID)
public class ModelDownloader {

    private static final String MODEL_URL = 
        "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip";
    
    private static final Path MODEL_DIR = 
        Path.of("config", "createvoicelink", "vosk-model");
    
    private static final Path DOWNLOAD_DIR = 
        Path.of("config", "createvoicelink");
    
    private static final String READY_FILE = ".model-ready";
    
    private static final AtomicBoolean downloadStarted = new AtomicBoolean(false);
    private static final AtomicBoolean downloadComplete = new AtomicBoolean(false);
    private static final AtomicBoolean downloadFailed = new AtomicBoolean(false);
    
    private static volatile String statusMessage = "Waiting...";
    private static volatile int progressPercent = 0;
    
    /**
     * Starts the model download when the server starts.
     * Safe to call multiple times — only runs once.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ensureModelAvailable();
    }
    
    /**
     * Notifies joining players about download status.
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!downloadStarted.get()) return;
        
        var player = event.getEntity();
        
        if (downloadFailed.get()) {
            player.displayClientMessage(
                Component.literal("§c⚠ Voice Link: Model download failed. Voice commands unavailable."), false);
            player.displayClientMessage(
                Component.literal("§7  Download manually: §n" + MODEL_URL), false);
            player.displayClientMessage(
                Component.literal("§7  Extract to: §nconfig/createvoicelink/vosk-model/"), false);
        } else if (downloadComplete.get()) {
            player.displayClientMessage(
                Component.literal("§a✓ Voice Link: Speech recognition model ready."), false);
        } else {
            player.displayClientMessage(
                Component.literal("§e⟳ Voice Link: Downloading speech model... §7" + progressPercent + "%"), false);
        }
    }
    
    /**
     * Check if model exists; if not, start download.
     * Returns immediately — download runs in background.
     */
    public static void ensureModelAvailable() {
        if (downloadStarted.getAndSet(true)) return;
        
        // Already downloaded?
        if (Files.exists(MODEL_DIR.resolve(READY_FILE))) {
            CreateVoiceLink.LOGGER.info("Vosk model already installed at {}", MODEL_DIR);
            downloadComplete.set(true);
            statusMessage = "Ready";
            progressPercent = 100;
            return;
        }
        
        // Clean up partial/empty directory from previous failed attempt
        if (Files.exists(MODEL_DIR)) {
            try {
                // If model dir exists but not READY, nuke it and start fresh
                if (!Files.exists(MODEL_DIR.resolve(READY_FILE))) {
                    deleteRecursive(MODEL_DIR);
                }
            } catch (IOException ignored) {}
        }
        
        CreateVoiceLink.LOGGER.info("Starting Vosk model download from {}", MODEL_URL);
        statusMessage = "Starting download...";
        
        CompletableFuture.runAsync(() -> {
            try {
                downloadAndExtract();
                downloadComplete.set(true);
                progressPercent = 100;
                statusMessage = "Ready — voice commands active";
                CreateVoiceLink.LOGGER.info("Vosk model download complete: {}", MODEL_DIR);
            } catch (Exception e) {
                downloadFailed.set(true);
                statusMessage = "Failed: " + e.getMessage();
                CreateVoiceLink.LOGGER.error("Vosk model download failed: {}", e.getMessage(), e);
            }
        });
    }
    
    private static void downloadAndExtract() throws IOException {
        Files.createDirectories(DOWNLOAD_DIR);
        
        Path zipPath = DOWNLOAD_DIR.resolve("vosk-model-download.zip");
        
        // Download with progress
        statusMessage = "Connecting...";
        HttpURLConnection conn = (HttpURLConnection) URI.create(MODEL_URL).toURL().openConnection();
        conn.setRequestProperty("User-Agent", "CreateVoiceLink/1.0");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(300000);
        conn.connect();
        
        long totalBytes = conn.getContentLengthLong();
        if (totalBytes <= 0) totalBytes = 45_000_000L; // ~43MB estimate
        
        statusMessage = "Downloading model...";
        
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(zipPath))) {
            
            byte[] buf = new byte[8192];
            long downloaded = 0;
            int n;
            int lastReported = -1;
            
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                downloaded += n;
                int pct = (int) ((downloaded * 100) / totalBytes);
                if (pct != lastReported && pct % 5 == 0) {
                    progressPercent = pct;
                    statusMessage = "Downloading... " + pct + "%";
                    CreateVoiceLink.LOGGER.debug("Model download: {}%", pct);
                    lastReported = pct;
                }
            }
        }
        
        // Extract
        statusMessage = "Extracting model...";
        progressPercent = 0;
        
        Files.createDirectories(MODEL_DIR);
        
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
            ZipEntry entry;
            long entryCount = 0;
            
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = MODEL_DIR.resolve(sanitisePath(entry.getName()));
                
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    try (OutputStream fos = Files.newOutputStream(targetPath)) {
                        zis.transferTo(fos);
                    }
                }
                
                entryCount++;
                if (entryCount % 20 == 0) {
                    progressPercent = Math.min(99, (int) ((entryCount * 100) / 200));
                    statusMessage = "Extracting... " + progressPercent + "%";
                }
                
                zis.closeEntry();
            }
        }
        
        // Mark as ready
        Files.writeString(MODEL_DIR.resolve(READY_FILE), "1");
        
        // Clean up zip
        try { Files.deleteIfExists(zipPath); } catch (IOException ignored) {}
        
        progressPercent = 100;
        statusMessage = "Ready";
    }
    
    /**
     * Sanitise zip entry paths to prevent zip-slip attacks.
     */
    private static String sanitisePath(String entryName) {
        String normalized = entryName.replace('\\', '/');
        // Strip the root folder (vosk-model-small-en-us-0.15/ -> .)
        int slash = normalized.indexOf('/');
        if (slash > 0 && slash < normalized.length() - 1) {
            normalized = normalized.substring(slash + 1);
        } else if (slash == normalized.length() - 1) {
            return "";
        }
        // Reject path traversal
        while (normalized.startsWith("../") || normalized.startsWith("/")) {
            normalized = normalized.replaceFirst("^(\\.\\./|/)", "");
        }
        return normalized;
    }
    
    private static void deleteRecursive(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                stream.forEach(p -> {
                    try { deleteRecursive(p); } catch (IOException ignored) {}
                });
            }
        }
        Files.deleteIfExists(dir);
    }
    
    /** Returns true if the model is downloaded and ready to use. */
    public static boolean isModelReady() {
        return downloadComplete.get() || Files.exists(MODEL_DIR.resolve(READY_FILE));
    }
    
    /** Current status string for UI display. */
    public static String getStatus() {
        return statusMessage;
    }
    
    /** 0-100 progress. */
    public static int getProgress() {
        return progressPercent;
    }
    
    /** Force a fresh download on next server start. */
    public static void forceRedownload() {
        try {
            deleteRecursive(MODEL_DIR);
        } catch (IOException ignored) {}
        downloadStarted.set(false);
        downloadComplete.set(false);
        downloadFailed.set(false);
        progressPercent = 0;
    }
}
