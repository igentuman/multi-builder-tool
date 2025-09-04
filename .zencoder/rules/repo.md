---
description: Repository Information Overview
alwaysApply: true
---

# MultiBuilder Tool Information

## Summary
MultiBuilder Tool (mbtool) is a Minecraft mod that adds a tool for building multiblock structures with a single click. The mod allows players to select structures, place required items in the tool's inventory, charge it, and then build complex structures instantly.

## Structure
- **src/main/java**: Core Java source code for the mod
- **src/main/resources**: Resource files, textures, and configuration
- **src/test**: Test framework (currently empty)
- **src/generated**: Generated resources
- **gradle**: Gradle wrapper and build configuration

## Language & Runtime
**Language**: Java
**Version**: Java 21
**Build System**: Gradle
**Package Manager**: Gradle/Maven
**Minecraft Version**: 1.21.8
**NeoForge Version**: 21.8.35

## Dependencies
**Main Dependencies**:
- NeoForge (21.8.35)
- Architectury API (6803291)
- JEI (6832478)

**Optional Integration Dependencies**:
- Immersive Engineering
- KubeJS
- Rhino
- NuclearCraft Neoteric
- EMI
- Mekanism
- Mekanism Generators
- Just Enough Mekanism Multiblocks
- GregTechCEu Modern

## Build & Installation
```bash
./gradlew build
```
The mod JAR file will be generated in the `build/libs` directory.

## Main Components

### Core Functionality
- **MultibuilderItem**: Main tool item implementation
- **MultiblocksProvider**: Provides multiblock structure data
- **NetworkHandler**: Handles network communication using NeoForge's packet system

### Data Management
- **MbtoolDataComponents**: Registry for data components (energy, recipe, rotation)
- **MbtoolConfig**: Configuration management

### User Interface
- **MultibuilderScreen**: Main UI for the tool
- **MultibuilderSelectStructureScreen**: UI for selecting structures

### Integration
- **KubeJS Integration**: Allows JavaScript customization of structures
- **Mod Compatibility**: Support for various tech mods' multiblocks

## Testing
**Framework**: NeoForge GameTest
**Test Location**: src/test
**Run Command**:
```bash
./gradlew runGameTestServer
```

## Migration Status
The project is currently being migrated from NeoForge 1.20.x to NeoForge 1.21. Key migration areas include:
- Network system (converted to CustomPacketPayload)
- DataComponents system (replacing NBT)
- Configuration files
- API compatibility updates