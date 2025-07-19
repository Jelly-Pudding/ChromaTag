# ChromaTag Plugin

**ChromaTag** is a Minecraft Paper 1.21.8 plugin that lets players customise their name colors across chat, tab list, and in-game nametags.

## Features
- Set custom colors using hex codes (e.g. `#FF0000`, `FF0000`) or predefined color names (e.g. `red`, `dark_blue`).
- Colors appear in chat, tab list, and above player heads.
- Granular permissions for setting/resetting own or others' colors.
- Persistent color storage using SQLite.
- Simple API for other plugins to interact with player colors.

## Installation
1. Download the latest `.jar` file from the [Releases page](https://github.com/Jelly-Pudding/ChromaTag/releases/latest).
2. Place the `.jar` file in your Minecraft server's `plugins` folder.
3. Restart your server. The plugin will create a `plugins/ChromaTag/chromatag.db` file to store colors.

## Configuration
Player color data is stored in `plugins/ChromaTag/chromatag.db`.

## Commands
- `/chromatag <color|#hex> [player]`: Sets the target player's name color. If `[player]` is omitted, sets your own color.
  - Requires `chromatag.set.self` to set own color.
  - Requires `chromatag.set.other` to set another player's color.
- `/chromatag reset [player]`: Resets the target player's name color to default. If `[player]` is omitted, resets your own color.
  - Requires `chromatag.reset.self` to reset own color.
  - Requires `chromatag.reset.other` to reset another player's color.

Aliases: `/ct`

## Permissions
- `chromatag.use`: Allows using ChromaTag commands and seeing tab completions (default: op)
- `chromatag.set.self`: Allows setting own name color (default: op)
- `chromatag.set.other`: Allows setting other players' name colors (default: op)
- `chromatag.reset.self`: Allows resetting own name color to default (default: op)
- `chromatag.reset.other`: Allows resetting other players' name colors to default (default: op)

## API for Developers
Other plugins can interact with ChromaTag:

1.  Add `ChromaTag` to `depend` or `softdepend` in your `plugin.yml`.
2.  Get the API instance:
    ```java
    Plugin chromaTagPlugin = Bukkit.getPluginManager().getPlugin("ChromaTag");
    if (chromaTagPlugin instanceof com.jellypudding.chromaTag.ChromaTag api) {
        // Use API methods
    } else {
        // ChromaTag not found or disabled
    }
    ```
3.  Available methods:
    - `api.getPlayerColor(UUID playerUUID)`: Returns the `TextColor` or `null`.
    - `api.setPlayerColor(UUID playerUUID, TextColor color)`: Sets the player's color. Returns `boolean` (currently always true).
    - `api.resetPlayerColor(UUID playerUUID)`: Resets the player's color. Returns `true` if a color was removed, `false` otherwise.

## Predefined Color Names
`black`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_red`, `dark_purple`, `gold`, `gray`, `dark_gray`, `blue`, `green`, `aqua`, `red`, `light_purple`, `yellow`, `white`

## Support Me
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/K3K715TC1R)
