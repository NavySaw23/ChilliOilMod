# 🌶️ ChilliOilMod

[![Version](https://img.shields.io/badge/Version-1.0.0-blue.svg)](#)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-blue.svg)](https://fabricmc.net/)
[![Environment](https://img.shields.io/badge/Side-Pure%20Server--Side-orange.svg)](#pure-server-side)
[![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey.svg)](LICENSE)

**Current Version:** `1.0.0` _(compatible with 1.21.11 Java Fabric)_

**ChilliOil** is a lightweight, **100% pure server-side** quality-of-life (QoL) and administration mod for modern Minecraft servers. Built around simple commands and utilities, it delivers features purely server-side without requiring players to install client mods, resource packs, or custom loaders — vanilla clients connect and work normally.

- 🌐 **100% Pure Server-Side**: Zero client requirements. Vanilla, Bedrock (via Geyser), or custom client players can join and enjoy all features out of the box.

> [!NOTE]
> This is a personal mod created for my own server needs and shared publicly in case others find it useful. Features are added as needed, with equipment hiding being the first implemented feature.

---

## 📦 Features

### 🎨 Aesthetic

#### Server-Side Equipment & Armor Hiding (`/armor`)

Allows players and admins to toggle the 3D visibility of equipped gear without altering stats, enchantments, or inventory icons.

- **Native Equipment Asset Swapping**: Utilizes vanilla `DataComponents.EQUIPPABLE` to suppress 3D models while preserving complete item integrity.
- **Third-Person & Multiplayer Synced**: Hides models cleanly across first-person, third-person (F5), the survival inventory paper doll, and for other observing players.
- **Broad Gear Support**:
  - **Player Equipment**: Leather, chainmail, iron, golden, diamond, netherite, turtle shell, and elytra.
  - **Mounts & Pets**: Wolf armor, all tiers of horse armor, saddles, and dyed ghast harnesses.
- **Full GUI & Container Integrity**: Item icons remain 100% visible and interactive inside inventories and mount screens. No container desyncs.
- **Tooltips & Safety**: Tagged with an italicized _`Invisible`_ lore indicator. Fully reversible at any time via `/armor unhide`.
- **Configurable Database**: Managed via `config/ChilliOil/armor.json` with in-game admin commands.

---

## 🎮 Commands & Permissions

| Command                   | Permission          | Category  | Description                                                      |
| :------------------------ | :------------------ | :-------- | :--------------------------------------------------------------- |
| `/armor hide`             | Everyone            | Aesthetic | Hides the 3D model of the equipment item held in your main hand. |
| `/armor hide all`         | Everyone            | Aesthetic | Hides all currently equipped armor items on your character.      |
| `/armor unhide`           | Everyone            | Aesthetic | Restores the 3D model of the item held in your main hand.        |
| `/armor unhide all`       | Everyone            | Aesthetic | Restores all currently equipped armor items on your character.   |
| `/armor list`             | Operator (Level 2+) | Aesthetic | Lists all item IDs registered in `armor.json`.                   |
| `/armor add <item_id>`    | Operator (Level 2+) | Aesthetic | Adds a registered item ID to `armor.json`.                       |
| `/armor remove <item_id>` | Operator (Level 2+) | Aesthetic | Removes an item ID from `armor.json`.                            |

---

## 📦 Installation

### Dedicated Server (Recommended)

1. Ensure your server runs **Fabric Loader** (`>=0.18.4`) on **Minecraft 1.21.11**.
2. Place [Fabric API](https://modrinth.com/mod/fabric-api) into the server's `mods/` directory.
3. Drop `ChilliOil-<version>-mc1.21.11.jar` into `mods/`.
4. Start/restart the server. Players connecting with unmodified vanilla clients can immediately use the features.

### Singleplayer / LAN

- ChilliOil functions smoothly in singleplayer or LAN environments by simply placing the JAR into your Fabric `mods/` folder.

---

## ⚙️ Configuration

Module configuration files are located under:

```
config/ChilliOil/
```

- `armor.json`: Controls allowed hideable equipment IDs and auto-populates defaults on first run.

---

## 🛠️ Building from Source

```bash
git clone https://github.com/NavySaw23/ChilliOil.git
cd ChilliOil
./gradlew build
```

Compiled release JARs output to `builds/ChilliOil-<version>-mc1.21.11.jar`.

---

## 📄 License

This project is licensed under [Creative Commons Zero v1.0 Universal](LICENSE) (Public Domain).
