package com.darkiiraze.createvoicelink.block;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import static net.minecraft.core.registries.Registries.BLOCK;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateVoiceLink.MODID);

    public static final DeferredBlock<MicrophoneBlock> MICROPHONE_BLOCK = BLOCKS.register("microphone",
        () -> new MicrophoneBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0F, 6.0F)
            .sound(SoundType.METAL)
            .noOcclusion()));

    public static final DeferredBlock<ComputerBlock> COMPUTER_BLOCK = BLOCKS.register("computer",
        () -> new ComputerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.0F, 8.0F)
            .sound(SoundType.METAL)
            .noOcclusion()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
