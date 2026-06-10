package com.darkiiraze.createvoicelink.item;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import static net.minecraft.core.registries.Registries.ITEM;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateVoiceLink.MODID);

    public static final DeferredItem<WrenchItem> WRENCH = ITEMS.register("wrench",
        () -> new WrenchItem(new Item.Properties()
            .stacksTo(1)
            .durability(128)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
