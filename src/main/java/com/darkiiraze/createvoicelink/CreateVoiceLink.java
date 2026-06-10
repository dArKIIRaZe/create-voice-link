package com.darkiiraze.createvoicelink;

import com.darkiiraze.createvoicelink.block.ModBlocks;
import com.darkiiraze.createvoicelink.gui.ModMenuTypes;
import com.darkiiraze.createvoicelink.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CreateVoiceLink.MODID)
public class CreateVoiceLink {
    public static final String MODID = "createvoicelink";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public CreateVoiceLink(IEventBus modEventBus) {
        LOGGER.info("Create: Voice Link initialising...");
        
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModTabs.register(modEventBus);
        
        LOGGER.info("Create: Voice Link loaded. Speak your commands.");
    }
}
