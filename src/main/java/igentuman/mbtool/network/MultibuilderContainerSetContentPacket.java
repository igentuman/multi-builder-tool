package igentuman.mbtool.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class MultibuilderContainerSetContentPacket extends ClientboundContainerSetContentPacket {
   private final int containerId;
   private final int stateId;
   private final List<ItemStack> items;
   private final ItemStack carriedItem;
   private int slotIndex = -1;
   private int dataIndex = -1;
   private int dataValue = -1;

   public MultibuilderContainerSetContentPacket(int pContainerId, int pStateId, NonNullList<ItemStack> pItems, ItemStack pCarriedItem) {
      super(pContainerId, pStateId, pItems, pCarriedItem);
       this.containerId = pContainerId;
      this.stateId = pStateId;
      this.items = NonNullList.withSize(pItems.size(), ItemStack.EMPTY);

      for(int i = 0; i < pItems.size(); ++i) {
         this.items.set(i, pItems.get(i).copy());
      }

      this.carriedItem = pCarriedItem.copy();
   }

   // Constructor for slot changes
   public MultibuilderContainerSetContentPacket(int pContainerId, int pStateId, int pSlotIndex, ItemStack pItemStack) {
      super(pContainerId, pStateId, NonNullList.create(), ItemStack.EMPTY);
      this.containerId = pContainerId;
      this.stateId = pStateId;
      this.slotIndex = pSlotIndex;
      this.items = NonNullList.create();
      this.carriedItem = pItemStack.copy();
   }

   // Constructor for data changes
   public MultibuilderContainerSetContentPacket(int pContainerId, int pDataIndex, int pDataValue) {
      super(pContainerId, 0, NonNullList.create(), ItemStack.EMPTY);
      this.containerId = pContainerId;
      this.stateId = 0;
      this.dataIndex = pDataIndex;
      this.dataValue = pDataValue;
      this.items = NonNullList.create();
      this.carriedItem = ItemStack.EMPTY;
   }

   public MultibuilderContainerSetContentPacket(FriendlyByteBuf pBuffer) {
       super(pBuffer);
      this.containerId = pBuffer.readUnsignedByte();
      this.stateId = pBuffer.readVarInt();
      ItemStackFriendlyByteBuf itemBuf = new ItemStackFriendlyByteBuf(pBuffer);
      this.items = pBuffer.readCollection(NonNullList::createWithCapacity, buf -> itemBuf.readItem());
      this.carriedItem = itemBuf.readItem();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      ItemStackFriendlyByteBuf itemBuf = new ItemStackFriendlyByteBuf(pBuffer);
      pBuffer.writeByte(this.containerId);
      pBuffer.writeVarInt(this.stateId);
      
      if (slotIndex >= 0) {
         // Slot change packet
         pBuffer.writeInt(slotIndex);
         itemBuf.writeItem(this.carriedItem);
      } else if (dataIndex >= 0) {
         // Data change packet
         pBuffer.writeInt(-1); // -1 to indicate data change
         pBuffer.writeInt(dataIndex);
         pBuffer.writeInt(dataValue);
      } else {
         // Full sync packet
         pBuffer.writeCollection(this.items, (buf, item) -> itemBuf.writeItemStack(item, false));
         itemBuf.writeItem(this.carriedItem);
      }
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleContainerContent(this);
   }

   public int getContainerId() {
      return this.containerId;
   }

   public List<ItemStack> getItems() {
      return this.items;
   }

   public ItemStack getCarriedItem() {
      return this.carriedItem;
   }

   public int getStateId() {
      return this.stateId;
   }

   public int getSlotIndex() {
      return this.slotIndex;
   }

   public int getDataIndex() {
      return this.dataIndex;
   }

   public int getDataValue() {
      return this.dataValue;
   }

   public static void encode(MultibuilderContainerSetContentPacket packet, FriendlyByteBuf pBuffer) {
      packet.write(pBuffer);
   }

   public static MultibuilderContainerSetContentPacket decode(FriendlyByteBuf pBuffer) {
      return new MultibuilderContainerSetContentPacket(pBuffer);
   }

   public static void handle(MultibuilderContainerSetContentPacket packet, Supplier<NetworkEvent.Context> context) {
      context.get().enqueueWork(() -> {
         // On client side, handle the packet
         ClientGamePacketListener listener = null;
         if (listener != null) {
            packet.handle(listener);
         }
      });
      context.get().setPacketHandled(true);
   }
}