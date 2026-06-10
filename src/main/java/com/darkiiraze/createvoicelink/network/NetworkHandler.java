package com.darkiiraze.createvoicelink.network;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = CreateVoiceLink.MODID)
public class NetworkHandler {
    
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        
        registrar.playToServer(
            AddCommandPacket.TYPE,
            AddCommandPacket.STREAM_CODEC,
            AddCommandPacket::handle
        );
        
        registrar.playToServer(
            RemoveCommandPacket.TYPE,
            RemoveCommandPacket.STREAM_CODEC,
            RemoveCommandPacket::handle
        );
        
        registrar.playToServer(
            AddSampleRequestPacket.TYPE,
            AddSampleRequestPacket.STREAM_CODEC,
            AddSampleRequestPacket::handle
        );
    }
}
