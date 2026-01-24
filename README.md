# Natural-Coal-Ore
Natural Coal Ore is a hytale mod

Link to mod: https://www.curseforge.com/hytale/mods/naturalcoalore

Natural Coal Ore

Natural Coal Ore adds natural coal ore spawning to your Hytale world. Coal veins generate automatically in newly explored chunks with smaller, scattered veins instead of massive clusters. Ore appears between Y=10 and Y=120 by default.

Fully configurable - adjust vein size, spawn rates, and more through a simple JSON config file. Includes creative mode commands for manual ore spawning and testing.

This mod also add new recipes for charcoal and new items like Crushed Coal that can be made in salvage workbench.

Installation:

add the jar file to UserData/Mods
Configuration

Configuration Setup

Navigate to your Hytale UserData folder:

   %APPDATA%\Hytale\UserData\Mods\NaturalCoalOre\Config\

Create a file named CoalOre.json with the following content:

{
  "MinY": 10,
  "MaxY": 120,
  "VeinsPerChunk": 20,
  "MinVeinSize": 6,
  "MaxVeinSize": 17,
  "SpawnChance": 0.85,
  "EnableNaturalGeneration": true,
  "CustomOres": []
}

Works for version 0.4.0

Default (no custom ores):

"CustomOres": []

To add a custom ore:
 

"CustomOres": [
  {
    "OreName": "Ore_Lead_Volcanic",
    "ReplacesBlocks": [
      "Rock_Stone",
      "Rock_Stone_Cobble",
      "Rock_Stone_Mossy",
      "Soil_Mud_Dry",
      "Rock_Volcanic_Cracked_Lava",
      "Rock_Volcanic"
    ],
    "MinY": 26,
    "MaxY": 50,
    "VeinsPerChunk": 15,
    "SpawnChance": 0.65,
    "MinVeinSize": 3,
    "MaxVeinSize": 4
  }
]

You can also add multiple custom ores by adding more entries to the array!

Start the game - the config will be loaded automatically
if you didnt create custom folder and file it will use the default values.
Setting 	Description
MinY / MaxY 	Depth range where coal spawns
VeinsPerChunk 	Number of veins per chunk
MinVeinSize / MaxVeinSize 	Blocks per vein (min-max)
SpawnChance 	Chance for chunk to contain coal (0.0 - 1.0)
EnableNaturalGeneration 	Toggle ore generation on/off

 
Commands (Creative Mode)

    /coalore spawn [size] - Spawn a vein at your location
    /coalore generate [radius] [count] - Generate multiple veins in an area
    /coalore fill [radius] - Fill underground area with veins
    /coalore reload - Reload config without restartings

