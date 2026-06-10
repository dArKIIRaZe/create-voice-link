package com.darkiiraze.createvoicelink;

import com.darkiiraze.createvoicelink.block.ModBlocks;
import com.darkiiraze.createvoicelink.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateVoiceLink.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VOICE_LINK_TAB = 
        TABS.register("voice_link_tab", () -> CreativeModeTab.builder()
            .title(Component.literal("Create: Voice Link"))
            .icon(() -> new ItemStack(ModBlocks.COMPUTER_BLOCK.get()))
            .displayItems((params, output) -> {
                output.accept(ModBlocks.MICROPHONE_BLOCK.get());
                output.accept(ModBlocks.COMPUTER_BLOCK.get());
                output.accept(ModItems.WRENCH.get());
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
