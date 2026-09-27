package net.chillioil.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface TrackedEntityInvoker {
    @Invoker("removePlayer")
    void callRemovePlayer(ServerPlayer player);

    @Invoker("updatePlayer")
    void callUpdatePlayer(ServerPlayer player);
}
