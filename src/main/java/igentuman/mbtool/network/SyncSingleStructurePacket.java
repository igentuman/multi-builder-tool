package igentuman.mbtool.network;

import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

import static igentuman.mbtool.Mbtool.rl;

public class SyncSingleStructurePacket {
    private final ResourceLocation id;
    private final CompoundTag nbt;
    private final String name;
    private final String group;
    private final int index;
    private final int total;
    private final boolean isLast;
    
    public SyncSingleStructurePacket(MultiblockStructure structure, int index, int total) {
        this.id = structure.getId();
        this.nbt = structure.getStructureNbt();
        this.name = structure.getName();
        this.group = structure.getGroup();
        this.index = index;
        this.total = total;
        this.isLast = (index == total - 1);
    }
    
    // Private constructor for decoding
    private SyncSingleStructurePacket(ResourceLocation id, CompoundTag nbt, String name, String group, int index, int total, boolean isLast) {
        this.id = id;
        this.nbt = nbt;
        this.name = name;
        this.group = group;
        this.index = index;
        this.total = total;
        this.isLast = isLast;
    }
    
    public static void encode(SyncSingleStructurePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(Objects.requireNonNullElseGet(packet.id, () -> rl("unknown")));
        buffer.writeNbt(Objects.requireNonNullElseGet(packet.nbt, CompoundTag::new));
        buffer.writeUtf(Objects.requireNonNullElseGet(packet.name, () -> ""));
        buffer.writeUtf(Objects.requireNonNullElseGet(packet.group, () -> "default"));
        buffer.writeInt(packet.index);
        buffer.writeInt(packet.total);
        buffer.writeBoolean(packet.isLast);
    }
    
    public static SyncSingleStructurePacket decode(FriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        CompoundTag nbt = buffer.readNbt();
        String name = buffer.readUtf();
        String group = buffer.readUtf();
        int index = buffer.readInt();
        int total = buffer.readInt();
        boolean isLast = buffer.readBoolean();
        
        return new SyncSingleStructurePacket(id, nbt, name, group, index, total, isLast);
    }
    
    public static void handle(SyncSingleStructurePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // This runs on the client side
            MultiblockStructure structure = new MultiblockStructure(
                packet.id, 
                packet.nbt, 
                packet.name,
                packet.group
            );
            
            if (packet.index == 0) {
                MultiblocksProvider.getStructures().clear();
            }
            
            MultiblocksProvider.getStructures().add(structure);
            
            if (packet.isLast) {
                MultiblocksProvider.notifyStructuresChanged();
            }
        });
        context.setPacketHandled(true);
    }
}