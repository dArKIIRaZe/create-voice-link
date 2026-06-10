package com.darkiiraze.createvoicelink.voice;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Simple Voice Chat integration.
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
        
        // getPlayer() returns Object — the actual Minecraft ServerPlayer
        ServerPlayer sp = player.getPlayer() instanceof ServerPlayer p ? p : null;
        if (sp == null) return;
        Vec3 position = sp.position();
        
        // getServerLevel() returns the Voice Chat API level — cast to MC's ServerLevel
        ServerLevel level = player.getServerLevel() instanceof ServerLevel sl ? sl : null;
        if (level == null) return;
        
        commandEngine.processVoicePacket(
            player.getUuid(),
            position,
            level,
            opusData
        );
    }

    public static VoicechatApi getApi() {
        return api;
    }
}
