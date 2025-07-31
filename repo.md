# Multi Builder Tool

A Minecraft Forge mod for Minecraft 1.20.1 that adds a powerful tool for building multiblock structures with a single click.

## 📋 Project Information

- **Mod ID**: `mbtool`
- **Display Name**: Multibuilder Tool
- **Version**: 1.0.0
- **Minecraft Version**: 1.20.1
- **Forge Version**: 47.4.0
- **Java Version**: 17
- **License**: MIT
- **Author**: igentuman

## 🎯 Features

### Core Functionality
- **One-Click Multiblock Building**: Select a multiblock structure and build it instantly
- **Inventory Integration**: Automatically uses items from your inventory
- **Energy System**: Powered tool with configurable energy consumption
- **GUI Interface**: User-friendly screens for structure selection and building

### Supported Integrations
- **JEI (Just Enough Items)**: Recipe and structure viewing integration
- **Mekanism**: Full compatibility with Mekanism multiblocks
- **ComputerCraft**: API integration for automated building
- **GregTech Modern**: Support for GTM multiblock structures
- **NuclearCraft Neoteric**: Compatible with NC multiblocks
- **The One Probe**: Information display integration

### Technical Features
- **Mixin Support**: Advanced mod compatibility through mixins
- **Network Synchronization**: Client-server communication for multiplayer
- **Configuration System**: Customizable energy settings and behavior
- **Access Transformers**: Deep integration with Minecraft internals

## 🔧 Configuration

The mod includes configurable energy settings:

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
├── client/                     # Client-side code
├── common/                     # Common utilities
├── config/                     # Configuration handling
├── container/                  # GUI containers
├── integration/                # Mod integrations
│   └── jei/                   # JEI integration
├── item/                      # Item definitions
├── network/                   # Network packets
└── util/                      # Utility classes
```

## 🛠️ Development Setup

### Prerequisites
- Java 17 JDK
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
- **Minecraft Forge**: 47.4.0+
- **Minecraft**: 1.20.1+

### Optional Dependencies
- **JEI**: 15.20.0.105 (Recipe integration)
- **Mekanism**: 10.4.5.19 (Multiblock support)
- **ComputerCraft**: 1.109.5 (Automation API)
- **The One Probe**: 10.0.1-3 (Information display)
- **GregTech Modern**: 1.6.4 (GTM multiblocks)

### Development Dependencies
- **Parchment Mappings**: 2023.09.03
- **Mixin**: 0.8.5
- **JUnit Jupiter**: 5.9.2
- **Mockito**: 5.3.1

## 🎮 How to Use

1. **Craft the Multibuilder Tool** - Find it in the Tools & Utilities creative tab
2. **Select Structure** - Right-click to open the structure selection GUI
3. **Prepare Materials** - Ensure you have all required blocks in your inventory
4. **Build** - Click to place the entire multiblock structure instantly

## 🔗 Links

- **Issues**: [GitHub Issues](https://github.com/igentuman/multi-builder-tool/issues)
- **License**: [MIT License](LICENSE.md)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## 📝 Build Information

- **Gradle Version**: 8.x
- **ForgeGradle**: 6.0-6.2
- **Shadow Plugin**: 7.0.0
- **Mixin Plugin**: 0.7+
- **Parchment**: 1.+

## 🏷️ Version History

- **1.0.0**: Initial release with core multiblock building functionality

---

*This mod is designed to streamline the process of building complex multiblock structures in modded Minecraft, making it accessible and efficient for players of all skill levels.*