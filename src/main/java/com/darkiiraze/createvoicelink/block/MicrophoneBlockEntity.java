package com.darkiiraze.createvoicelink.block;

import com.darkiiraze.createvoicelink.voice.VoiceNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MicrophoneBlockEntity extends BlockEntity {
    /** Computer blocks this microphone is linked to via wrench */
    private final Set<BlockPos> linkedComputers = new HashSet<>();
    private String name = "Microphone";

    public MicrophoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MICROPHONE.get(), pos, state);
    }

    public void tick() {
        // Microphone is passive — voice data flows through VoiceChatPlugin.
        // The server-side voice pipeline checks proximity to MicrophoneBlockEntities
        // and routes recognised commands to linked computers.
    }

    public void linkToComputer(BlockPos computerPos) {
        linkedComputers.add(computerPos);
        setChanged();
    }

    public void unlinkComputer(BlockPos computerPos) {
        linkedComputers.remove(computerPos);
        setChanged();
    }

    public Set<BlockPos> getLinkedComputers() {
        return linkedComputers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setChanged();
    }

    public boolean isPlayerInRange(BlockPos playerPos) {
        return playerPos.distSqr(worldPosition) <= 100; // 10 blocks
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Name", name);
        tag.putInt("LinkedComputerCount", linkedComputers.size());
        int i = 0;
        for (BlockPos pos : linkedComputers) {
            tag.putLong("LinkedComputer_" + i, pos.asLong());
            i++;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag) {
        super.loadAdditional(tag);
        name = tag.getString("Name");
        linkedComputers.clear();
        int count = tag.getInt("LinkedComputerCount");
        for (int i = 0; i < count; i++) {
            linkedComputers.add(BlockPos.of(tag.getLong("LinkedComputer_" + i)));
        }
    }
}
