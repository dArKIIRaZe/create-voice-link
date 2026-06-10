package com.darkiiraze.createvoicelink.voice;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import com.darkiiraze.createvoicelink.block.ComputerBlockEntity;
import com.darkiiraze.createvoicelink.block.ComputerBlockEntity.VoiceCommand;
import com.darkiiraze.createvoicelink.block.MicrophoneBlockEntity;
import de.maxhenkel.voicechat.api.VoicechatApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.vosk.Model;
import org.vosk.Recognizer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;

/**
 * Core voice command engine.
 * 
 * Flow:
 * 1. Opus audio packets arrive from VoiceChatPlugin
 * 2. Decode to PCM (48000Hz mono -> 16000Hz for Vosk)
 * 3. Accumulate audio frames into per-player buffer
 * 4. Run Vosk recognition when buffer is full or silence detected
 * 5. Check if any nearby Microphone blocks are linked to Computers
 * 6. Match recognised text against enrolled command samples
 * 7. If match found, activate Redstone Link on that Computer
 * 
 * Also handles voice sample enrollment: when a player clicks "Record Sample"
 * in the GUI, the next spoken phrase is captured and stored as a training
 * sample for the specified command.
 */
public class VoiceCommandEngine {
    
    private final VoicechatApi voicechatApi;
    private final ExecutorService sttExecutor = Executors.newFixedThreadPool(2);
    
    /** Per-player audio buffer for accumulating voice input */
    private final Map<UUID, PlayerAudioBuffer> playerBuffers = new ConcurrentHashMap<>();
    
    /** Players currently in sample-capture mode (phrase -> computer) */
    private static final ConcurrentHashMap<UUID, PendingCapture> pendingCaptures = new ConcurrentHashMap<>();
    
    private Model voskModel;
    
    private static final int TARGET_SAMPLE_RATE = 16000;
    private static final int FRAME_SIZE_SAMPLES = 960;
    private static final int SILENCE_TIMEOUT_MS = 1500;
    private static final float MATCH_SIMILARITY_THRESHOLD = 0.75f;
    
    public VoiceCommandEngine(VoicechatApi api) {
        this.voicechatApi = api;
        loadVoskModel();
    }
    
    // ---- Vosk Model Loading ----
    
    private void loadVoskModel() {
        sttExecutor.submit(() -> {
            try {
                File modelDir = new File("config/createvoicelink/vosk-model");
                if (!modelDir.exists()) {
                    CreateVoiceLink.LOGGER.warn(
                        "Vosk model not found at {}. Download a small model from " +
                        "https://alphacephei.com/vosk/models (vosk-model-small-en-us-0.15, ~40MB) " +
                        "and extract it there.", modelDir.getAbsolutePath());
                    modelDir.mkdirs();
                    return;
                }
                voskModel = new Model(modelDir.getAbsolutePath());
                CreateVoiceLink.LOGGER.info("Vosk STT model loaded successfully.");
            } catch (Exception e) {
                CreateVoiceLink.LOGGER.error("Failed to load Vosk model: {}", e.getMessage());
            }
        });
    }
    
    // ---- Sample Capture ----
    
    private record PendingCapture(String phrase, ComputerBlockEntity computer) {}
    
    /**
     * Called from the network handler when a player clicks "Record Sample" in the GUI.
     * The next recognised speech from this player will be stored as a training sample.
     */
    public static void requestSampleCapture(UUID playerId, String phrase, ComputerBlockEntity computer) {
        pendingCaptures.put(playerId, new PendingCapture(phrase.toLowerCase(), computer));
        CreateVoiceLink.LOGGER.info("Sample capture requested for {}: '{}'", playerId, phrase);
    }
    
    // ---- Voice Packet Processing ----
    
    /**
     * Process an incoming Opus-encoded voice packet from Simple Voice Chat.
     */
    public void processVoicePacket(UUID playerId, Vec3 playerPos, ServerLevel level, byte[] opusData) {
        if (voskModel == null) return;
        
        var decoder = voicechatApi.createDecoder();
        short[] pcm48000 = decoder.decode(opusData);
        decoder.close();
        
        short[] pcm16000 = downsample(pcm48000, 3);
        
        PlayerAudioBuffer buf = playerBuffers.computeIfAbsent(playerId,
            k -> new PlayerAudioBuffer(playerPos, level));
        buf.addAudio(pcm16000);
        buf.updatePosition(playerPos);
        
        long now = System.currentTimeMillis();
        long silence = now - buf.lastAudioTime;
        
        if (buf.totalSamples >= FRAME_SIZE_SAMPLES * 50 ||
            (silence > SILENCE_TIMEOUT_MS && buf.totalSamples > FRAME_SIZE_SAMPLES * 15)) {
            
            short[] fullAudio = buf.drain();
            buf.lastAudioTime = now;
            
            sttExecutor.submit(() -> {
                String text = recognize(fullAudio);
                if (text != null && !text.isBlank()) {
                    handleRecognizedText(playerId, playerPos, level, text.trim());
                }
            });
        }
    }
    
    private String recognize(short[] audio) {
        try (Recognizer recognizer = new Recognizer(voskModel, TARGET_SAMPLE_RATE)) {
            ByteBuffer byteBuf = ByteBuffer.allocate(audio.length * 2);
            byteBuf.order(ByteOrder.LITTLE_ENDIAN);
            for (short s : audio) byteBuf.putShort(s);
            
            if (recognizer.acceptWaveForm(byteBuf.array(), byteBuf.array().length)) {
                String result = recognizer.getResult();
                JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                return json.has("text") ? json.get("text").getAsString() : null;
            }
            String partial = recognizer.getPartialResult();
            JsonObject json = JsonParser.parseString(partial).getAsJsonObject();
            return json.has("partial") ? json.get("partial").getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private void handleRecognizedText(UUID playerId, Vec3 playerPos, ServerLevel level, String text) {
        // Check for pending sample capture first
        PendingCapture capture = pendingCaptures.remove(playerId);
        if (capture != null) {
            capture.computer.addSample(capture.phrase, text);
            CreateVoiceLink.LOGGER.info("Voice sample captured for '{}': \"{}\" (player {})",
                capture.phrase, text, playerId);
            return;
        }
        
        // Normal voice command matching
        BlockPos playerBP = BlockPos.containing(playerPos);
        
        for (int x = -10; x <= 10; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos checkPos = playerBP.offset(x, y, z);
                    BlockEntity be = level.getBlockEntity(checkPos);
                    
                    if (be instanceof MicrophoneBlockEntity mic) {
                        if (!mic.isPlayerInRange(playerBP)) continue;
                        
                        for (BlockPos compPos : mic.getLinkedComputers()) {
                            BlockEntity compBe = level.getBlockEntity(compPos);
                            if (compBe instanceof ComputerBlockEntity computer) {
                                tryMatchCommand(computer, text);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void tryMatchCommand(ComputerBlockEntity computer, String recognizedText) {
        String normalized = recognizedText.toLowerCase().trim();
        
        VoiceCommand bestMatch = null;
        float bestScore = 0f;
        
        for (VoiceCommand cmd : computer.getCommands().values()) {
            if (!cmd.samples.isEmpty()) {
                for (String sample : cmd.samples) {
                    float score = stringSimilarity(normalized, sample);
                    if (score > bestScore) { bestScore = score; bestMatch = cmd; }
                }
            }
            float phraseScore = stringSimilarity(normalized, cmd.phrase.toLowerCase());
            if (phraseScore > bestScore) { bestScore = phraseScore; bestMatch = cmd; }
        }
        
        if (bestMatch != null && bestScore >= MATCH_SIMILARITY_THRESHOLD) {
            CreateVoiceLink.LOGGER.info("Voice command matched! Computer='{}', Phrase='{}', Score={}",
                computer.getComputerName(), bestMatch.phrase, String.format("%.2f", bestScore));
            computer.activateVoiceCommand(bestMatch);
        }
    }
    
    // ---- String Similarity (Jaro-Winkler) ----
    
    private float stringSimilarity(String a, String b) {
        if (a.equals(b)) return 1.0f;
        if (a.isEmpty() || b.isEmpty()) return 0f;
        
        int maxLen = Math.max(a.length(), b.length());
        int window = Math.max(1, maxLen / 2 - 1);
        
        boolean[] matchedA = new boolean[a.length()];
        boolean[] matchedB = new boolean[b.length()];
        int matches = 0;
        
        for (int i = 0; i < a.length(); i++) {
            int start = Math.max(0, i - window);
            int end = Math.min(b.length(), i + window + 1);
            for (int j = start; j < end; j++) {
                if (!matchedB[j] && a.charAt(i) == b.charAt(j)) {
                    matches++;
                    matchedA[i] = matchedB[j] = true;
                    break;
                }
            }
        }
        if (matches == 0) return 0f;
        
        int transpositions = 0, k = 0;
        for (int i = 0; i < a.length(); i++) {
            if (matchedA[i]) {
                while (!matchedB[k]) k++;
                if (a.charAt(i) != b.charAt(k)) transpositions++;
                k++;
            }
        }
        transpositions /= 2;
        
        float jaro = ((float) matches / a.length() +
                      (float) matches / b.length() +
                      (float) (matches - transpositions) / matches) / 3.0f;
        
        int prefix = 0;
        for (int i = 0; i < Math.min(4, Math.min(a.length(), b.length())); i++) {
            if (a.charAt(i) == b.charAt(i)) prefix++; else break;
        }
        
        return jaro + prefix * 0.1f * (1.0f - jaro);
    }
    
    // ---- Audio Utility ----
    
    private short[] downsample(short[] input, int factor) {
        short[] output = new short[input.length / factor];
        for (int i = 0; i < output.length; i++) output[i] = input[i * factor];
        return output;
    }
    
    // ---- Per-Player Audio Buffer ----
    
    private static class PlayerAudioBuffer {
        private final List<short[]> frames = new ArrayList<>();
        int totalSamples = 0;
        Vec3 playerPos;
        ServerLevel level;
        long lastAudioTime = System.currentTimeMillis();
        
        PlayerAudioBuffer(Vec3 pos, ServerLevel level) {
            this.playerPos = pos;
            this.level = level;
        }
        
        void addAudio(short[] frame) {
            frames.add(frame);
            totalSamples += frame.length;
            lastAudioTime = System.currentTimeMillis();
        }
        
        void updatePosition(Vec3 pos) { this.playerPos = pos; }
        
        short[] drain() {
            short[] out = new short[totalSamples];
            int offset = 0;
            for (short[] frame : frames) {
                System.arraycopy(frame, 0, out, offset, frame.length);
                offset += frame.length;
            }
            frames.clear();
            totalSamples = 0;
            return out;
        }
    }
}
