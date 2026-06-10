package com.darkiiraze.createvoicelink.voice;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;

/**
 * Simple Voice Chat integration.
 * Intercepts microphone packets at the server level, decodes Opus audio,
 * and routes PCM data to the Vosk STT pipeline for voice command recognition.
 *
 * This must be registered as a "voicechat" entrypoint in neoforge.mods.toml:
 *   [[entrypoints]]
 *   type = "voicechat"
 *   value = "com.darkiiraze.createvoicelink.voice.VoiceChatPlugin"
 */
public class VoiceChatPlugin implements VoicechatPlugin {

    private static VoicechatApi api;
    private static VoiceCommandEngine commandEngine;

    @Override
    public String getPluginId() {
        return CreateVoiceLink.MODID;
    }

    @Override
    public void initialize(VoicechatApi voicechatApi) {
        api = voicechatApi;
        commandEngine = new VoiceCommandEngine(api);
        CreateVoiceLink.LOGGER.info("VoiceChatPlugin initialised — voice command engine ready.");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (commandEngine == null) return;
        
        var conn = event.getSenderConnection();
        if (conn == null) return;
        
        var player = conn.getPlayer();
        if (player == null) return;
        
        byte[] opusData = event.getPacket().getOpusEncodedData();
        if (opusData == null || opusData.length == 0) return;
        
        commandEngine.processVoicePacket(
            player.getUuid(),
            player.getPlayer().position(),
            player.getServerLevel(),
            opusData
        );
    }

    public static VoicechatApi getApi() {
        return api;
    }
}
