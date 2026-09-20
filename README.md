# 🌶️ ChilliOilMod

[![Version](https://img.shields.io/badge/Version-1.1.0-blue.svg)](#)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-blue.svg)](https://fabricmc.net/)
[![Environment](https://img.shields.io/badge/Side-Pure%20Server--Side-orange.svg)](#pure-server-side)
[![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey.svg)](LICENSE)

**Current Version:** `1.1.0` _(compatible with 1.21.11 Java Fabric)_

**ChilliOil** is a lightweight, **100% pure server-side** quality-of-life (QoL) and administration mod for modern Minecraft servers. Built around simple commands, virtual chest GUIs, and utility systems, it delivers features purely on the server side without requiring players to install client mods, resource packs, or custom loaders — vanilla clients connect and work normally.

- 🌐 **100% Pure Server-Side**: Zero client requirements. Vanilla, Bedrock (via Geyser), or custom client players can join and enjoy all features out of the box.

---

## 📦 Features

<details>
<summary><strong>🎛️ Management — Server Control (<code>/chilliconfig</code>)</strong></summary>

<br/>

Server operators can manage mod modules in real time via an in-game chest menu or commands:

- **Interactive Configuration Menu (`/chilliconfig`)**:
  - Open a visual 27-slot chest GUI to toggle individual modules on and off with live status indicators.
  - One-click **Reload Configs** button to reload changes from disk instantly.
  - One-click **Reset to Defaults** button to restore initial module configurations.
- **Dynamic Module Control**:
  - Turn off unused features at any time; disabled commands politely inform players that the feature is turned off.
- **Command-Line Management**:
  - `/chilliconfig list`: View all modules and their active states (`[ON]` / `[OFF]`).
  - `/chilliconfig module <name> <true|false>`: Enable or disable modules from console or chat.
  - `/chilliconfig reload`: Reload config files from disk.
  - `/chilliconfig reset <module>`: Reset module settings to defaults.

</details>

<details>
<summary><strong>🎨 Aesthetic</strong></summary>

<br/>

<details>
<summary><strong>1. Equipment & Armor Hiding (<code>/armor</code>)</strong></summary>

<br/>

Hide the 3D models of equipped armor and gear while keeping all defense, stats, enchantments, and inventory icons completely intact:

- **Hide Any Worn Gear**:
  - Hide player gear including helmets, chestplates, leggings, boots, turtle shells, and elytra.
  - Hide pet and mount gear including wolf armor, horse armor, saddles, and dyed ghast harnesses.
- **Synced Everywhere**:
  - Gear is hidden across first-person, third-person (F5), the inventory paper doll, and for all other players watching you.
- **Quick Code Toggles (`/armor code <0|1>x4`)**:
  - Quickly set armor visibility with 4-digit binary codes from top to bottom (Helmet, Chestplate, Leggings, Boots). For example, `/armor code 0101` hides your helmet and leggings while showing your chestplate and boots.
- **Visual Clarity**:
  - Hidden items show an italicized _`Invisible`_ label on their lore in inventories.
  - Restore visibility at any time using `/armor unhide` or `/armor unhide all`.

</details>

<details>
<summary><strong>2. Skin Switcher & Personal Wardrobe (<code>/skin</code>, <code>/wardrobe</code>)</strong></summary>

<br/>

Change your character's skin on the fly and save your favorite outfits into a personal wardrobe:

- **Apply Skins from Any Player (`/skin user`)**:
  - Wear the skin of any Minecraft player by username with classic (Steve) or slim (Alex) arm models.
- **Apply Skins from Image URLs (`/skin web`)**:
  - Load and wear custom skin images directly from web image links.
- **Skin Info & Command Helper (`/skin info`)**:
  - Check your active skin model, source, and click the on-screen message to copy the exact command.
- **Arm Model Switcher (`/skin style`)**:
  - Switch between classic (4px) and slim (3px) arms on your active skin at any time.
- **Interactive Wardrobe Menu (`/wardrobe`)**:
  - Browse your outfits in a 54-slot chest interface showcasing 3D player heads with each outfit's real face.
  - **One-Click Equip**: Click any head in the wardrobe to put on that outfit immediately.
  - **`[+]` Add Outfit Button**: Click the `[+]` button to open an in-game naming prompt and save your current skin.
- **Persistent Across Relogs**:
  - Your chosen skin is saved automatically. When you disconnect and join back, you will log in wearing your skin without needing to re-apply it.
  - Use `/skin clear` whenever you want to return to your account's default skin.

</details>

</details>

---

## 🎮 Commands & Permissions

<details>
<summary><strong>🎛️ Management Commands (<code>/chilliconfig</code>)</strong></summary>

<br/>

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/chilliconfig` | Operator (Level 2+) | Opens the interactive module management chest GUI. |
| `/chilliconfig list` | Operator (Level 2+) | Lists all modules, operational states, and descriptions. |
| `/chilliconfig module <name> <true\|false>` | Operator (Level 2+) | Enables or disables an individual module at runtime. |
| `/chilliconfig reload` | Operator (Level 2+) | Reloads all configuration files from disk. |
| `/chilliconfig reset <module>` | Operator (Level 2+) | Resets configuration for a module to defaults. |

</details>

<details>
<summary><strong>🛡️ Armor & Equipment Commands (<code>/armor</code>)</strong></summary>

<br/>

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/armor hide` | Everyone | Hides the 3D model of the item held in your main hand. |
| `/armor hide all` | Everyone | Hides all currently equipped armor items. |
| `/armor unhide` | Everyone | Restores the 3D model of the item held in your main hand. |
| `/armor unhide all` | Everyone | Restores all currently equipped armor items. |
| `/armor code <0\|1>x4` | Everyone | Sets visibility for Helmet, Chestplate, Leggings, Boots (e.g. `0101`). |
| `/armor list` | Operator (Level 2+) | Lists all item IDs registered in `armor.json`. |
| `/armor add <item_id>` | Operator (Level 2+) | Adds a registered item ID to `armor.json`. |
| `/armor remove <item_id>` | Operator (Level 2+) | Removes an item ID from `armor.json`. |

</details>

<details>
<summary><strong>👔 Skin & Wardrobe Commands (<code>/skin</code>, <code>/wardrobe</code>)</strong></summary>

<br/>

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/skin user <classic\|slim> <username>` | Everyone | Applies a skin from a Mojang username. |
| `/skin web <classic\|slim> <url>` | Everyone | Generates and applies a skin from an image URL. |
| `/skin info` | Everyone | Displays current skin details with a click-to-copy command button. |
| `/skin style <classic\|slim>` | Everyone | Switches arm thickness between Steve and Alex. |
| `/skin clear` | Everyone | Resets skin back to your account's default registered skin. |
| `/wardrobe [page]` | Everyone | Opens the 54-slot personal Wardrobe GUI. |
| `/wardrobe wear <name>` | Everyone | Equips a saved outfit by name. |
| `/wardrobe add <name>` | Everyone | Saves your current skin to your wardrobe. |
| `/wardrobe remove <name>` | Everyone | Deletes a saved outfit from your wardrobe. |

</details>

---

## 📦 Installation

### Dedicated Server (Recommended)
1. Ensure your server runs **Fabric Loader** (`>=0.18.4`) on **Minecraft 1.21.11**.
2. Place [Fabric API](https://modrinth.com/mod/fabric-api) into the server's `mods/` directory.
3. Drop `ChilliOil-<version>-mc1.21.11.jar` into `mods/`.
4. Start the server. Players connecting with unmodified vanilla clients can immediately use all features.

### Singleplayer / LAN
- ChilliOil functions smoothly in singleplayer or LAN environments by simply placing the JAR into your Fabric `mods/` folder.

---

## ⚙️ Configuration

Module configuration files are located under:

```
config/ChilliOil/
```

- `modules.json`: Controls runtime module on/off states.
- `armor.json`: Controls allowed hideable equipment IDs.
- `skin-wardrobe.json`: Configures MineSkin API keys, switch cooldowns, and wardrobe limits.

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
