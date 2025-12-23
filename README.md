# BossRace

Boat racing plugin for Paper/Spigot 1.21.8+

## Features

- Multiplayer boat races
- Checkpoint and lap system
- Automatic lobby countdown
- Start barriers
- Configurable rewards for 1st, 2nd and 3rd place
- Signs to join races

## Commands

| Command | Description |
|---------|-------------|
| `/race join` | Join the race |
| `/race leave` | Leave the race |
| `/race status` | View race status |

### Admin Commands

| Command | Description |
|---------|-------------|
| `/race create [laps]` | Create a race |
| `/race start` | Start the race |
| `/race forcestart` | Force start |
| `/race end` | End the race |
| `/race start1/start2` | Set start line |
| `/race finish1/finish2` | Set finish line |
| `/race checkpoint1/checkpoint2` | Add checkpoint |
| `/race addspawn` | Add spawn position |
| `/race clearspawns` | Clear spawns |
| `/race list` | View configuration |
| `/race reload` | Reload config |

## Permissions

- `bossrace.admin` - Access to admin commands

## Installation

1. Download the JAR from releases
2. Place in the `plugins/` folder
3. Restart the server
4. Configure lines and checkpoints using commands

## Configuration

Edit `config.yml` to adjust:
- Default laps
- Reward commands for each position
