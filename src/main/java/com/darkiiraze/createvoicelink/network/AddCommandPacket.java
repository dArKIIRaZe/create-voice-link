package com.darkiiraze.createvoicelink.network;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import com.darkiiraze.createvoicelink.block.ComputerBlockEntity;
import com.darkiiraze.createvoicelink.block.ComputerBlockEntity.VoiceCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client GUI to server when adding a new voice command.
 */
public record AddCommandPacket(String phrase, String freqFirstItem, String freqSecondItem) 
    implements CustomPacketPayload {
    
    public static final Type<AddCommandPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateVoiceLink.MODID, "add_command"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, AddCommandPacket> STREAM_CODEC = 
        StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, AddCommandPacket::phrase,
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, AddCommandPacket::freqFirstItem,
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, AddCommandPacket::freqSecondItem,
            AddCommandPacket::new
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(AddCommandPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player.containerMenu instanceof com.darkiiraze.createvoicelink.gui.ComputerMenu menu) {
                ComputerBlockEntity computer = menu.getComputer();
                if (computer != null) {
                    // Parse frequency item string into ItemStack
                    // In practice the GUI would show available frequency items from redstone links
                    // For now we just store the phrase with empty frequency stacks
                    VoiceCommand cmd = new VoiceCommand(
                        packet.phrase(), 
                        ItemStack.EMPTY,  // freqFirst
                        ItemStack.EMPTY   // freqSecond
                    );
                    computer.addCommand(cmd);
                }
            }
        });
    }
}
