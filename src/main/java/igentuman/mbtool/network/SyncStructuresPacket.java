package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class SyncStructuresPacket implements CustomPacketPayload {
    public static final Type<SyncStructuresPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Mbtool.MODID, "sync_structures"));

    public record StructureData(ResourceLocation id, CompoundTag nbt, String name) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StructureData> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, StructureData::id,
            ByteBufCodecs.COMPOUND_TAG, StructureData::nbt,
            ByteBufCodecs.STRING_UTF8, StructureData::name,
            StructureData::new
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStructuresPacket> STREAM_CODEC = StreamCodec.composite(
        StructureData.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncStructuresPacket::structures,
        SyncStructuresPacket::new
    );

    private final List<StructureData> structures;
    
    public static SyncStructuresPacket of(List<MultiblockStructure> structures) {
        List<StructureData> data = new ArrayList<>();
        for (MultiblockStructure structure : structures) {
            data.add(new StructureData(
                structure.getId(),
                structure.getStructureNbt(),
                structure.getName()
            ));
        }
        return new SyncStructuresPacket(data);
    }
    
    // Private constructor for decoding
    private SyncStructuresPacket(List<StructureData> structures) {
        this.structures = structures;
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public List<StructureData> structures() {
        return structures;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // This runs on the client side
            List<MultiblockStructure> clientStructures = new ArrayList<>();
            
            for (StructureData structureData : this.structures) {
                MultiblockStructure structure = new MultiblockStructure(
                    structureData.id(), 
                    structureData.nbt(), 
                    structureData.name()
                );
                clientStructures.add(structure);
            }
            
            // Replace client structures with server structures
            MultiblocksProvider.setStructures(clientStructures);
        });
    }
}