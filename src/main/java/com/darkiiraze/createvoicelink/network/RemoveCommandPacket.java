package com.darkiiraze.createvoicelink.network;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import com.darkiiraze.createvoicelink.block.ComputerBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client GUI to server to remove a voice command.
 */
public record RemoveCommandPacket(String phrase) implements CustomPacketPayload {
    
    public static final Type<RemoveCommandPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateVoiceLink.MODID, "remove_command"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveCommandPacket> STREAM_CODEC = 
        StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, RemoveCommandPacket::phrase,
            RemoveCommandPacket::new
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(RemoveCommandPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player.containerMenu instanceof com.darkiiraze.createvoicelink.gui.ComputerMenu menu) {
                ComputerBlockEntity computer = menu.getComputer();
                if (computer != null) {
                    computer.removeCommand(packet.phrase());
                }
            }
        });
    }
}
