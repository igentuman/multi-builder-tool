package igentuman.mbtool.network;

import igentuman.mbtool.Mbtool;
import igentuman.mbtool.integration.jei.JEIPlugin;
import igentuman.mbtool.util.MultiblockStructure;
import igentuman.mbtool.util.MultiblocksProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static igentuman.mbtool.Mbtool.rl;

public class SyncSingleStructurePacket implements CustomPacketPayload {
    public static final Type<SyncSingleStructurePacket> TYPE = new Type<>(rl("sync_single_structure"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSingleStructurePacket> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SyncSingleStructurePacket::id,
        ByteBufCodecs.COMPOUND_TAG, SyncSingleStructurePacket::nbt,
        ByteBufCodecs.STRING_UTF8, SyncSingleStructurePacket::name,
        ByteBufCodecs.INT, SyncSingleStructurePacket::index,
        ByteBufCodecs.INT, SyncSingleStructurePacket::total,
        ByteBufCodecs.BOOL, SyncSingleStructurePacket::isLast,
        SyncSingleStructurePacket::new
    );

    private final Identifier id;
    private final CompoundTag nbt;
    private final String name;
    private final int index;
    private final int total;
    private final boolean isLast;
    
    public SyncSingleStructurePacket(MultiblockStructure structure, int index, int total) {
        this.id = structure.getId();
        this.nbt = structure.getStructureNbt();
        this.name = structure.getName();
        this.index = index;
        this.total = total;
        this.isLast = (index == total - 1);
    }
    
    // Private constructor for decoding
    private SyncSingleStructurePacket(Identifier id, CompoundTag nbt, String name, int index, int total, boolean isLast) {
        this.id = id;
        this.nbt = nbt;
        this.name = name;
        this.index = index;
        this.total = total;
        this.isLast = isLast;
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public Identifier id() {
        return id;
    }

    public CompoundTag nbt() {
        return nbt;
    }

    public String name() {
        return name;
    }

    public int index() {
        return index;
    }

    public int total() {
        return total;
    }

    public boolean isLast() {
        return isLast;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // This runs on the client side
            MultiblockStructure structure = new MultiblockStructure(
                this.id, 
                this.nbt, 
                this.name
            );
            
            // If this is the first structure, clear the list
            if (this.index == 0) {
                MultiblocksProvider.getStructures().clear();
            }
            
            // Add the structure to the client's list
            MultiblocksProvider.getStructures().add(structure);
            
            // Log progress
            System.out.println("Synced structure " + (this.index + 1) + "/" + this.total + ": " + this.name);
            
            if (this.isLast) {
                System.out.println("All structures synced successfully! Total: " + this.total);
                if (ModList.get().isLoaded("jei")) {
                    JEIPlugin.updateRecipes();
                }
            }
        });
    }
}