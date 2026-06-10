package com.darkiiraze.createvoicelink.link;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.foundation.utility.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

/**
 * Bridges voice commands to Create's Redstone Link network.
 * Implements IRedstoneLinkable so the voice-controlled computer
 * can participate in existing Redstone Link networks.
 * 
 * When a voice command is activated, this acts as a TRANSMITTER,
 * sending redstone power on the saved frequency pair.
 * When idle, it's inactive and draws nothing.
 */
public class VoiceRedstoneLink implements IRedstoneLinkable {
    
    private final LevelAccessor level;
    private final BlockPos position;
    private Couple<RedstoneLinkNetworkHandler.Frequency> key = Couple.create(null, null);
    
    public int receiveStrength = 0;
    public int transmitStrength = 0;
    private boolean registered = false;
    
    public VoiceRedstoneLink(LevelAccessor level, BlockPos position) {
        this.level = level;
        this.position = position;
    }
    
    /**
     * Set the frequency pair from ItemStacks.
     */
    public void setFrequency(RedstoneLinkNetworkHandler.Frequency first, RedstoneLinkNetworkHandler.Frequency second) {
        key = Couple.create(first, second);
        if (registered) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, this);
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, this);
        }
    }
    
    /**
     * Register this linkable with the Create network.
     * Call when the computer becomes powered.
     */
    public void register() {
        if (!registered) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, this);
            registered = true;
        }
    }
    
    /**
     * Unregister from the network.
     * Call when the computer loses power or is destroyed.
     */
    public void unregister() {
        if (registered) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, this);
            registered = false;
        }
    }
    
    /**
     * Push a signal update into the network.
     */
    public void update() {
        if (registered) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, this);
        }
    }
    
    // --- IRedstoneLinkable ---
    
    @Override
    public int getTransmittedStrength() {
        return transmitStrength;
    }
    
    @Override
    public void setReceivedStrength(int power) {
        this.receiveStrength = power;
    }
    
    @Override
    public boolean isListening() {
        return false; // We're a transmitter only when activated
    }
    
    @Override
    public boolean isAlive() {
        return true;
    }
    
    @Override
    public Couple<RedstoneLinkNetworkHandler.Frequency> getNetworkKey() {
        return key;
    }
    
    @Override
    public BlockPos getLocation() {
        return position;
    }
}
