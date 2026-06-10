package com.darkiiraze.createvoicelink;

import com.darkiiraze.createvoicelink.block.ComputerBlockEntity;
import com.darkiiraze.createvoicelink.block.MicrophoneBlockEntity;
import com.darkiiraze.createvoicelink.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

import static net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BLOCK_ENTITY_TYPE, CreateVoiceLink.MODID);

    public static final Supplier<BlockEntityType<MicrophoneBlockEntity>> MICROPHONE = 
        BLOCK_ENTITIES.register("microphone",
            () -> BlockEntityType.Builder.of(MicrophoneBlockEntity::new, ModBlocks.MICROPHONE_BLOCK.get())
                .build(null));

    public static final Supplier<BlockEntityType<ComputerBlockEntity>> COMPUTER = 
        BLOCK_ENTITIES.register("computer",
            () -> BlockEntityType.Builder.of(ComputerBlockEntity::new, ModBlocks.COMPUTER_BLOCK.get())
                .build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
