package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record SyncStructuresPacket(List<StructureData> structures) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncStructuresPacket> TYPE = 
        new CustomPacketPayload.Type<>(Mbtool.rl("sync_structures"));
    
    public static final StreamCodec<FriendlyByteBuf, SyncStructuresPacket> STREAM_CODEC = 
        StreamCodec.of(SyncStructuresPacket::encode, SyncStructuresPacket::decode);
    
    public SyncStructuresPacket(List<MultiblockStructure> structures) {
        this(structures.stream()
            .map(structure -> new StructureData(
                structure.getId(),
                structure.getStructureNbt(),
                structure.getName()
            ))
            .toList());
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    private static void encode(SyncStructuresPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.structures.size());
        for (StructureData structure : packet.structures) {
            buffer.writeResourceLocation(Objects.requireNonNullElseGet(structure.id, () -> Mbtool.rl("unknown")));
            buffer.writeNbt(Objects.requireNonNullElseGet(structure.nbt, CompoundTag::new));
            buffer.writeUtf(Objects.requireNonNullElseGet(structure.name, () -> ""));
        }
    }
    
    private static SyncStructuresPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<StructureData> structures = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            CompoundTag nbt = buffer.readNbt();
            String name = buffer.readUtf();
            structures.add(new StructureData(id, nbt, name));
        }
        
        return new SyncStructuresPacket(structures);
    }
    
    public static void handle(SyncStructuresPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // This runs on the client side
            List<MultiblockStructure> clientStructures = new ArrayList<>();
            
            for (StructureData structureData : packet.structures()) {
                MultiblockStructure structure = new MultiblockStructure(
                    structureData.id, 
                    structureData.nbt, 
                    structureData.name
                );
                clientStructures.add(structure);
            }
            
            // Replace client structures with server structures
            MultiblocksProvider.setStructures(clientStructures);
        });
    }
    
    public record StructureData(ResourceLocation id, CompoundTag nbt, String name) {}
}