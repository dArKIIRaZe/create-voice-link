package com.darkiiraze.createvoicelink.network;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import com.darkiiraze.createvoicelink.block.ComputerBlockEntity;
import com.darkiiraze.createvoicelink.voice.VoiceCommandEngine;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client requests the server to record a voice sample for a command.
 * The server-side VoiceChatPlugin will capture the next speech from this player
 * and store it as an enrollment sample for the given phrase.
 */
public record AddSampleRequestPacket(String phrase) implements CustomPacketPayload {
    
    public static final Type<AddSampleRequestPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateVoiceLink.MODID, "add_sample_request"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, AddSampleRequestPacket> STREAM_CODEC = 
        StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, AddSampleRequestPacket::phrase,
            AddSampleRequestPacket::new
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(AddSampleRequestPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player.containerMenu instanceof com.darkiiraze.createvoicelink.gui.ComputerMenu menu) {
                ComputerBlockEntity computer = menu.getComputer();
                if (computer != null) {
                    // Signal the voice engine to capture next speech as a sample
                    VoiceCommandEngine.requestSampleCapture(player.getUUID(), packet.phrase(), computer);
                }
            }
        });
    }
}
