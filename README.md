# Multi Builder Tool

A Minecraft NeoForge mod for Minecraft 1.21.1 that adds a powerful tool for building multiblock structures with a single click.

## 📋 Project Information

- **Mod ID**: `mbtool`
- **Display Name**: Multibuilder Tool
- **Version**: 1.1.22
- **Minecraft Version**: 1.21.1
- **NeoForge Version**: 21.1.199
- **Java Version**: 21
- **License**: MIT
- **Author**: igentuman

## 🎯 Features

### Core Functionality
- **One-Click Multiblock Building**: Select a multiblock structure and build it instantly
- **Inventory Integration**: Automatically uses items from the tool's internal inventory
- **Energy System**: Powered tool with configurable energy consumption (NeoForge Energy)
- **GUI Interface**: User-friendly screens for structure selection and building
- **Dismantle Mode**: Quickly dismantle structures and return blocks to inventory

### Supported Integrations
- **EMI**: Full recipe and multiblock structure viewing integration
- **JEI (Just Enough Items)**: Recipe and structure viewing integration
- **Mekanism & Mekanism Generators**: Full compatibility with Mekanism multiblocks
- **AE2 (Applied Energistics 2)**: Use ME storage network as a source of building materials
- **Immersive Engineering**: Compatibility with IE multiblocks
- **GregTech Modern**: Support for GTM multiblock structures
- **KubeJS**: Integration for custom scripts and events
- **ComputerCraft**: API integration for automated building
- **NuclearCraft Neoteric**: Compatible with NC multiblocks

### Technical Features
- **Mixin Support**: Advanced mod compatibility through mixins
- **Network Synchronization**: Client-server communication for multiplayer
- **Configuration System**: Customizable energy settings and behavior via NeoForge config
- **Data Components**: Uses modern Minecraft Data Components for item state

## 🎮 How to Use

1. **Craft the Multibuilder Tool** - Find it in the Tools & Utilities creative tab.
2. **Select Structure** - Use the tool to open the structure selection GUI.
3. **Prepare Materials** - Put required blocks into the tool's internal inventory.
4. **Charge Tool** - Ensure the tool has enough energy (FE/Forge Energy).
5. **Build** - Click on a block in the world to place the selected multiblock structure.

## 🔧 Configuration

The mod includes configurable energy settings (found in `serverconfig/mbtool-server.toml` or `commonconfig/mbtool-common.toml`):

```toml
[Energy Settings]
# Maximum energy capacity (1000-10000000 FE)
maxEnergy = 100000

# Energy transfer rate (1-100000 FE/tick)
energyTransferRate = 1000

# Energy cost per block (1-10000 FE)
energyPerBlock = 100
```

## 🏗️ Project Structure

```
src/main/java/igentuman/mbtool/
├── Mbtool.java                 # Main mod class
├── client/                     # Client-side code (Screens, Handlers)
├── common/                     # Common utilities
├── config/                     # Configuration handling
├── container/                  # GUI containers (Menus)
├── integration/                # Mod integrations (EMI, JEI)
├── item/                      # Item definitions
├── network/                   # Network packets
├── registration/              # Registry handling (Data Components)
└── util/                      # Utility classes (Multiblock Provider, Mod Util)
```

## 🛠️ Development Setup

### Prerequisites
- Java 21 JDK
- IntelliJ IDEA (recommended)
- Git

### Building
```bash
./gradlew build
```

### Running in Development
```bash
# Client
./gradlew runClient

# Server
./gradlew runServer

# Data Generation
./gradlew runData
```

### Testing
```bash
./gradlew test
```

## 📦 Dependencies

### Required Dependencies
- **NeoForge**: 21.1.199+
- **Minecraft**: 1.21.1+
- **Architectury**: 13.0.8+

### Optional/Integration Dependencies
- **EMI**: 1.1.x
- **JEI**: 19.27.0.336+
- **Mekanism**: 10.7.18.84+
- **KubeJS**: 2101.7.2+
- **ComputerCraft**: 1.109.5+
- **Immersive Engineering**: Latest
- **GregTech Modern**: 1.6.4+

## 🔗 Links

- **Issues**: [GitHub Issues](https://github.com/igentuman/multi-builder-tool/issues)
- **License**: [MIT License](LICENSE.md)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

---

*This mod is designed to streamline the process of building complex multiblock structures in modded Minecraft, making it accessible and efficient for players of all skill levels.*
