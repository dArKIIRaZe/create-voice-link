package com.darkiiraze.createvoicelink.gui;

import com.darkiiraze.createvoicelink.CreateVoiceLink;
import com.darkiiraze.createvoicelink.block.ComputerBlockEntity;
import com.darkiiraze.createvoicelink.network.AddCommandPacket;
import com.darkiiraze.createvoicelink.network.RemoveCommandPacket;
import com.darkiiraze.createvoicelink.voice.ModelDownloader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ComputerScreen extends AbstractContainerScreen<ComputerMenu> {
    private static final ResourceLocation BACKGROUND = 
        ResourceLocation.fromNamespaceAndPath(CreateVoiceLink.MODID, "textures/gui/computer.png");
    
    private EditBox phraseField;
    private EditBox freqField;
    private Button addButton;
    private List<Button> commandButtons = new ArrayList<>();
    
    private int leftPos, topPos;
    private int imageWidth = 256, imageHeight = 220;
    
    public ComputerScreen(ComputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
    
    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        
        phraseField = new EditBox(font, leftPos + 8, topPos + 18, 120, 20, 
            Component.literal("Voice command..."));
        phraseField.setMaxLength(64);
        addRenderableWidget(phraseField);
        
        freqField = new EditBox(font, leftPos + 136, topPos + 18, 80, 20,
            Component.literal("Freq item..."));
        freqField.setMaxLength(32);
        addRenderableWidget(freqField);
        
        addButton = Button.builder(Component.literal("Add"), btn -> {
            String phrase = phraseField.getValue().trim();
            String freqItem = freqField.getValue().trim();
            if (!phrase.isEmpty() && !freqItem.isEmpty()) {
                PacketDistributor.sendToServer(new AddCommandPacket(phrase, freqItem, ""));
                phraseField.setValue("");
                freqField.setValue("");
            }
        }).bounds(leftPos + 220, topPos + 17, 30, 20).build();
        addRenderableWidget(addButton);
        
        rebuildCommandList();
    }
    
    private void rebuildCommandList() {
        for (Button btn : commandButtons) {
            removeWidget(btn);
        }
        commandButtons.clear();
        
        ComputerBlockEntity comp = menu.getComputer();
        if (comp == null) return;
        
        int y = topPos + 45;
        int idx = 0;
        for (var entry : comp.getCommands().entrySet()) {
            ComputerBlockEntity.VoiceCommand cmd = entry.getValue();
            String label = cmd.phrase + " [" + cmd.samples.size() + " samples]";
            
            Button delBtn = Button.builder(Component.literal(label + "  X"), btn -> {
                PacketDistributor.sendToServer(new RemoveCommandPacket(cmd.phrase));
            }).bounds(leftPos + 8, y + idx * 22, 224, 18).build();
            addRenderableWidget(delBtn);
            commandButtons.add(delBtn);
            idx++;
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
    
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        
        ComputerBlockEntity comp = menu.getComputer();
        String header = comp != null ? comp.getComputerName() : "Voice Computer";
        String powerText = comp != null && comp.isPowered() 
            ? "POWERED" : "NO POWER";
        int powerColor = comp != null && comp.isPowered() ? 0x00FF00 : 0xFF0000;
        
        gui.drawCenteredString(font, header, leftPos + imageWidth / 2, topPos + 5, 0xAAAAAA);
        gui.drawString(font, powerText, leftPos + imageWidth - 55, topPos + 5, powerColor);
        
        if (!ModelDownloader.isModelReady()) {
            int modelY = topPos + imageHeight - 14;
            String modelStatus = ModelDownloader.getStatus();
            int pct = ModelDownloader.getProgress();
            
            gui.fill(leftPos + 4, modelY - 2, leftPos + imageWidth - 4, modelY + 12, 0x80000000);
            
            if (pct > 0) {
                int barWidth = (imageWidth - 20) * pct / 100;
                gui.fill(leftPos + 6, modelY + 6, leftPos + 8 + barWidth, modelY + 9, 0xFFD4B14A);
            }
            
            gui.drawString(font, Component.literal("\u27F3 " + modelStatus), leftPos + 8, modelY, 0xFFD4B14A);
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (phraseField.isFocused() || freqField.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
