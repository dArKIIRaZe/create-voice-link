package com.darkiiraze.createvoicelink.item;

import com.darkiiraze.createvoicelink.block.ComputerBlockEntity;
import com.darkiiraze.createvoicelink.block.MicrophoneBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * The Wrench links Microphone blocks to Computer blocks.
 * Right-click a Microphone, then right-click a Computer to link them.
 * Sneak + right-click a Microphone to clear all its linked computers.
 * Sneak + right-click a Computer to open the command GUI (direct access).
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
            if (sneak) {
                // Sneak clears all links from this microphone
                int count = mic.getLinkedComputers().size();
                mic.getLinkedComputers().clear();
                mic.setChanged();
                ctx.getPlayer().displayClientMessage(
                    Component.literal("Cleared " + count + " computer link(s) from " + mic.getName())
                        .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.SUCCESS;
            } else {
                // Store this microphone position for next right-click
                var tag = stack.getOrCreateTag();
                tag.putLong("LinkSource", pos.asLong());
                tag.putBoolean("IsMic", true);
                ctx.getPlayer().displayClientMessage(
                    Component.literal("Linked from " + mic.getName() + " — right-click a Computer to complete link")
                        .withStyle(ChatFormatting.GREEN), true);
                return InteractionResult.SUCCESS;
            }
        }
        
        if (be instanceof ComputerBlockEntity comp) {
            var tag = stack.getOrCreateTag();
            if (tag.getBoolean("IsMic") && tag.contains("LinkSource")) {
                // Complete the link: Mic -> Computer
                BlockPos micPos = BlockPos.of(tag.getLong("LinkSource"));
                BlockEntity micBe = level.getBlockEntity(micPos);
                if (micBe instanceof MicrophoneBlockEntity mic) {
                    mic.linkToComputer(pos);
                    tag.remove("LinkSource");
                    tag.remove("IsMic");
                    ctx.getPlayer().displayClientMessage(
                        Component.literal(mic.getName() + " linked to " + comp.getComputerName())
                            .withStyle(ChatFormatting.GREEN), true);
                } else {
                    ctx.getPlayer().displayClientMessage(
                        Component.literal("Original block removed — link cancelled")
                            .withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.SUCCESS;
            } else if (sneak && tag.getBoolean("IsMic")) {
                // Sneak cancels the linking
                tag.remove("LinkSource");
                tag.remove("IsMic");
                ctx.getPlayer().displayClientMessage(
                    Component.literal("Link cancelled").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Links Microphones to Computers").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click Mic then Computer to link").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Sneak + right-click to clear or cancel").withStyle(ChatFormatting.DARK_GRAY));
    }
}
