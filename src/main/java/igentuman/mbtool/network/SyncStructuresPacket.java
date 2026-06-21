package igentuman.mbtool.network;

import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static igentuman.mbtool.Mbtool.rl;

public class SyncStructuresPacket {
    private final List<StructureData> structures;
    
    public SyncStructuresPacket(List<MultiblockStructure> structures) {
        this.structures = new ArrayList<>();
        for (MultiblockStructure structure : structures) {
            this.structures.add(new StructureData(
                structure.getId(),
                structure.getStructureNbt(),
                structure.getName(),
                structure.getGroup()
            ));
        }
    }
    
    // Private constructor for decoding
    private SyncStructuresPacket() {
        this.structures = new ArrayList<>();
    }
    
    public static void encode(SyncStructuresPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.structures.size());
        for (StructureData structure : packet.structures) {
            buffer.writeResourceLocation(Objects.requireNonNullElseGet(structure.id, () -> rl("unknown")));
            buffer.writeNbt(Objects.requireNonNullElseGet(structure.nbt, CompoundTag::new));
            buffer.writeUtf(Objects.requireNonNullElseGet(structure.name, () -> ""));
            buffer.writeUtf(Objects.requireNonNullElseGet(structure.group, () -> "default"));
        }
    }
    
    public static SyncStructuresPacket decode(FriendlyByteBuf buffer) {
        SyncStructuresPacket packet = new SyncStructuresPacket();
        int size = buffer.readInt();
        
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            CompoundTag nbt = buffer.readNbt();
            String name = buffer.readUtf();
            String group = buffer.readUtf();
            packet.structures.add(new StructureData(id, nbt, name, group));
        }
        
        return packet;
    }
    
    public static void handle(SyncStructuresPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // This runs on the client side
            List<MultiblockStructure> clientStructures = new ArrayList<>();
            
            for (StructureData structureData : packet.structures) {
                MultiblockStructure structure = new MultiblockStructure(
                    structureData.id, 
                    structureData.nbt, 
                    structureData.name,
                    structureData.group
                );
                clientStructures.add(structure);
            }
            
            // Replace client structures with server structures
            MultiblocksProvider.setStructures(clientStructures);
        });
        context.setPacketHandled(true);
    }
    
    private static class StructureData {
        final ResourceLocation id;
        final CompoundTag nbt;
        final String name;
        final String group;

        StructureData(ResourceLocation id, CompoundTag nbt, String name, String group) {
            this.id = id;
            this.nbt = nbt;
            this.name = name;
            this.group = group;
        }
    }
}