package com.darkiiraze.createvoicelink.gui;

import com.darkiiraze.createvoicelink.block.ComputerBlockEntity;
import com.darkiiraze.createvoicelink.block.ComputerBlockEntity.VoiceCommand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Container for the Computer GUI.
 * Has slots for frequency items and player inventory.
 * Actual command management is handled via network packets to the server.
 */
public class ComputerMenu extends AbstractContainerMenu {
    
    private final ComputerBlockEntity computer;
    
    // Client-side constructor
    public ComputerMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, null);
    }
    
    // Server-side constructor
    public ComputerMenu(int id, Inventory playerInventory, ComputerBlockEntity computer) {
        super(ModMenuTypes.COMPUTER_MENU.get(), id);
        this.computer = computer;
        
        // Player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }
    
    public ComputerBlockEntity getComputer() {
        return computer;
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Standard shift-click logic
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            stack = slotStack.copy();
            if (index < 36) {
                if (!moveItemStackTo(slotStack, 36, slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(slotStack, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stack;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
