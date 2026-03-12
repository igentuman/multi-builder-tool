# NeoForge 1.21.1 Migration Plan for MultiBuilder Tool

This rule outlines the step-by-step plan for migrating the MultiBuilder Tool mod from NeoForge 1.20.x to NeoForge 1.21.1.

## Phase 1: Environment & Dependencies [COMPLETED]
1. **Update `gradle.properties`**: 
   - Set `minecraft_version=1.21.1`
   - Set `neoforge_version=21.1.65`
   - Set `java_version=21`
2. **Update `build.gradle`**:
   - Switched to `net.neoforged.moddev` plugin.
   - Updated toolchain to Java 21.
   - Updated core dependencies (Architectury, JEI, Mekanism, KubeJS).
   - Adjusted `ProcessResources` to target `neoforge.mods.toml`.
3. **Update `neoforge.mods.toml`**:
   - Renamed from `mods.toml`.
   - Updated `modLoader`, `loaderVersion`, and dependency ranges.

## Phase 2: Data Components Migration (Replacing NBT) [COMPLETED]
Minecraft 1.21+ uses **Data Components** instead of NBT for `ItemStack` data.
1. **Register Components**: Created `MbtoolDataComponents` class. Registered `STRUCTURE_RECIPE`, `STRUCTURE_ROTATION`, `RUNTIME_STRUCTURE`, `UUID`, `ENERGY`, `INVENTORY`, and `PLACED_STRUCTURES`.
2. **Update `MultibuilderItem`**: Replaced all NBT calls with Data Component accessors.
3. **Capability Handling**: Migrated to `RegisterCapabilitiesEvent`. Updated `CustomEnergyStorage` and `ItemInventoryHandler` to use Data Components for persistence. Removed `ItemCapabilityProvider` and `ItemEnergyHandler`.

## Phase 3: Network System Migration [COMPLETED]
Transitioned from `SimpleChannel` to `CustomPacketPayload`.
1. **Implemented `CustomPacketPayload`**: All packet classes (`SyncMultibuilderParamsPacket`, `SyncStructuresPacket`, `SyncSingleStructurePacket`, `SyncRuntimeStructurePacket`, `DismantleStructurePacket`) now implement `CustomPacketPayload`.
2. **Defined `Type` and `StreamCodec`**: Each packet defines its `Type<T>` and a `StreamCodec` for serialization.
3. **Registered Packets**: Use `RegisterPayloadHandlersEvent` in `NetworkHandler` with `PayloadRegistrar`.
4. **Updated Handlers**: Handlers now use `IPayloadContext` and run on the correct thread.
5. **Cleaned Up Dead Code**: Removed `MultibuilderContainerSetContentPacket` and `ItemStackFriendlyByteBuf` as they were unused or deprecated.

## Phase 4: Rendering System Updates [IN PROGRESS]
Minecraft 1.21 has significant changes in the rendering pipeline.
1. **Render Events**: Use `RenderLevelStageEvent` or `RenderGuiEvent` depending on where the preview should be.
2. **PreviewRenderer Adjustments**:
   - Update `PoseStack` usage if any method signatures changed.
   - Adjust `RenderType` for block previews. 1.21 might require specific `RenderType` handling for translucency.
   - Ensure `VertexConsumer` and `MultiBufferSource` are used correctly with the new batching logic.
3. **Shader Changes**: If custom shaders are used, update to the new core shader system.

## Phase 5: Code Cleanup & API Adjustments [PARTIALLY COMPLETED]
1. **ResourceLocation**: Updated most calls to `ResourceLocation.fromNamespaceAndPath(modid, path)`.
2. **Menu/GUI**: Replaced `NetworkHooks.openScreen` with `player.openMenu`.
3. **KubeJS/Integration**: Check for breaking changes in KubeJS 1.21 API.

## Phase 6: Testing
1. **Run GameTest**: Execute `./gradlew runGameTestServer` to verify core logic.
2. **Manual Verification**: Test structure selection, energy charging, and building in-game.
3. **Rendering Check**: Verify that the block preview is correctly rendered and doesn't cause z-fighting or transparency issues.
