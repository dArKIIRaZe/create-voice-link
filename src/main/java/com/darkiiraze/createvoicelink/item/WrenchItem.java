package com.darkiiraze.createvoicelink.item;

import com.darkiiraze.createvoicelink.block.ComputerBlockEntity;
import com.darkiiraze.createvoicelink.block.MicrophoneBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * The Wrench links Microphone blocks to Computer blocks.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack stack = ctx.getItemInHand();
        boolean sneak = ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown();

        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (be instanceof MicrophoneBlockEntity mic) {
            CompoundTag tag = getOrCreateTag(stack);
            if (sneak) {
                int count = mic.getLinkedComputers().size();
                mic.getLinkedComputers().clear();
                mic.setChanged();
                tag.remove("LinkSource");
                tag.remove("IsMic");
                saveTag(stack, tag);
                ctx.getPlayer().displayClientMessage(
                    Component.literal("Cleared " + count + " computer link(s) from " + mic.getName())
                        .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.SUCCESS;
            } else {
                tag.putLong("LinkSource", pos.asLong());
                tag.putBoolean("IsMic", true);
                saveTag(stack, tag);
                ctx.getPlayer().displayClientMessage(
                    Component.literal("Linked from " + mic.getName() + " — right-click a Computer to complete link")
                        .withStyle(ChatFormatting.GREEN), true);
                return InteractionResult.SUCCESS;
            }
        }
        
        if (be instanceof ComputerBlockEntity comp) {
            CompoundTag tag = getOrCreateTag(stack);
            if (tag.getBoolean("IsMic") && tag.contains("LinkSource")) {
                BlockPos micPos = BlockPos.of(tag.getLong("LinkSource"));
                BlockEntity micBe = level.getBlockEntity(micPos);
                if (micBe instanceof MicrophoneBlockEntity mic) {
                    mic.linkToComputer(pos);
                    tag.remove("LinkSource");
                    tag.remove("IsMic");
                    saveTag(stack, tag);
                    ctx.getPlayer().displayClientMessage(
                        Component.literal(mic.getName() + " linked to " + comp.getComputerName())
                            .withStyle(ChatFormatting.GREEN), true);
                } else {
                    tag.remove("LinkSource");
                    tag.remove("IsMic");
                    saveTag(stack, tag);
                    ctx.getPlayer().displayClientMessage(
                        Component.literal("Original block removed — link cancelled")
                            .withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.SUCCESS;
            } else if (sneak && tag.getBoolean("IsMic")) {
                tag.remove("LinkSource");
                tag.remove("IsMic");
                saveTag(stack, tag);
                ctx.getPlayer().displayClientMessage(
                    Component.literal("Link cancelled").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    private void saveTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Links Microphones to Computers").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click Mic then Computer to link").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Sneak + right-click to clear or cancel").withStyle(ChatFormatting.DARK_GRAY));
    }
}
