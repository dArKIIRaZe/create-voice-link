package com.darkiiraze.createvoicelink.gui;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import java.util.function.Supplier;

import static net.minecraft.core.registries.Registries.MENU;
import com.darkiiraze.createvoicelink.CreateVoiceLink;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(MENU, CreateVoiceLink.MODID);
    
    public static final Supplier<MenuType<ComputerMenu>> COMPUTER_MENU = 
        MENUS.register("computer_menu",
            () -> IMenuTypeExtension.create((id, inv, data) -> 
                new ComputerMenu(id, inv)));
    
    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
