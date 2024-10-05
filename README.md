# ChromaTag Plugin

**ChromaTag** is a Minecraft Paper 1.21.1 plugin that lets players customise their name colors across chat, tab list, and in-game nametags.

## Features
- Set custom colors using hex codes or predefined color names
- Colors appear in chat, tab list, and above player heads
- Permissions-based color setting and resetting
- Persistent color storage across server restarts
- Seamless integration with Essentials if present, but fully functional as a standalone plugin

## Installation
1. Download the latest release [here](https://github.com/Jelly-Pudding/ChromaTag/releases/latest).
2. Place the `.jar` file in your Minecraft server's `plugins` folder.
3. Restart your server.

## Configuration
The plugin automatically saves player colors in `config.yml`. You don't need to manually edit this file unless you want to preset colors for players.

## Commands
- `/chromatag <color> [player]`: Set your own or another player's name color. Color can be a hex code (e.g., #FF0000) or a predefined color name (e.g., red, blue, dark_aqua).
- `/chromatag reset [player]`: Reset your own or another player's name color to default.

## Permissions
- `chromatag.use`: Allows use of the ChromaTag command (default: op)
- `chromatag.set`: Allows setting name colors (default: op)
- `chromatag.reset`: Allows resetting name colors to default (default: op)

## Predefined Color Names
Black, Dark Blue, Dark Green, Dark Aqua, Dark Red, Dark Purple, Gold, Gray, Dark Gray, Blue, Green, Aqua, Red, Light Purple, Yellow, White

## Essentials Integration
ChromaTag automatically detects and integrates with the Essentials plugin if it's present on your server. This integration ensures that player nicknames set through Essentials maintain the color assigned by ChromaTag.

For optimal functionality with ChromaTag, you may want to modify your Essentials configuration:

1. Open your Essentials `config.yml` file.
2. Find the `nickname-prefix` setting.
3. Change it to an empty string like this:
   ```yaml
   nickname-prefix: ''
   ```

## Support Me
Donations to my [Patreon](https://www.patreon.com/lolwhatyesme) will help with the development of this project.
