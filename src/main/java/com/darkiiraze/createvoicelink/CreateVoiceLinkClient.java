package com.darkiiraze.createvoicelink;

import com.darkiiraze.createvoicelink.gui.ComputerScreen;
import com.darkiiraze.createvoicelink.gui.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

@Mod(value = CreateVoiceLink.MODID, dist = Dist.CLIENT)
public class CreateVoiceLinkClient {
    public CreateVoiceLinkClient(IEventBus modEventBus, ModContainer modContainer) {
        CreateVoiceLink.LOGGER.info("Create: Voice Link client initialising...");
        modEventBus.addListener(this::onRegisterScreens);
    }

    @SubscribeEvent
    public void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.COMPUTER_MENU.get(), ComputerScreen::new);
    }
}
