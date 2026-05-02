
# Meteor Villager Roller

![checks](https://github.com/maxsupermanhd/meteor-villager-roller/actions/workflows/checks.yml/badge.svg)
![devbuild](https://github.com/maxsupermanhd/meteor-villager-roller/actions/workflows/devbuild.yml/badge.svg)

Addon that changes villager profession until the desired trade is found.

We have a [Discord server](https://discord.com/invite/DFsMKWJJPN).

## Versions

| Minecraft | Meteor           | Supported | Download                                                                                                                                      |
|-----------|------------------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| 26.1.x    | 26.1.2           | Yes       | [1.4.19](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.19)                                                         |
| 1.21.11   | 1.21.11          | No        | [1.4.18](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.18)                                                         |
| 1.21.10   | 1.21.10          | No        | [1.4.17](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.17)                                                         |
| 1.21.8    | 1.21.8           | No        | [1.4.16](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.16)                                                         |
| 1.21.7    | 1.21.7           | No        | [1.4.15](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.15)                                                         |
| 1.21.6    | 1.21.6           | No        | [1.4.14](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.14)                                                         |
| 1.21.5    | 1.21.5           | No        | [1.4.13](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.13)                                                         |
| 1.21.4    | 0.6.0-dev >2228  | No        | [1.4.10](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.10)                                                         |
| 1.21.3    | 0.5.9-dev >2202  | No        | [1.4.9](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.9)                                                           |
| 1.21.1    | 0.5.8-dev >2147  | No        | [1.4.8](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.8)                                                           |
| 1.20.6    | 0.5.7-dev >2072  | No        | [1.4.5](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.4.7/villager-roller-1.4.5+mc1.20.6-rev.e9d12e8-dirty.jar) |
| 1.20.4    | 0.5.6-dev >1983  | No        | [1.4.4](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.4)                                                           |
| 1.20.2    | 0.5.5-dev >1945  | No        | [1.4.3](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.4.3)                                                           |
| 1.20.1    | 0.5.4-dev >1846  | No        | [1.4.2](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.4.2/villager-roller-1.4.2+mc1.20.1-rev.ac89621.jar)       |
| 1.19.4    | 0.5.3-dev >1817  | No        | [1.4.2](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.4.2/villager-roller-1.4.2+mc1.19.4-rev.ac89621-dirty.jar) |
| 1.19.3    | 0.5.2-dev >1714  | No        | [1.3.6](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.3.6)                                                           |
| 1.19.2    | 0.5.1-dev >1573  | No        | [1.3.4](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.3.4)                                                           |
| 1.19.1    | 0.5.1-dev >1570  | No        | [1.3.2](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.3.2)                                                           |
| 1.19      | 0.5.0-dev >=1563 | No        | [1.3.1](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.3.1)                                                           |
| 1.19      | 0.5.0-dev <1563  | No        | [1.3](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.3)                                                               |
| 1.18.2    | 0.4.9            | No        | [1.3](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.3/villager-roller-1.3+mc1.18.2-rev.3d6f694.jar)             |
| 1.18.1    | ?                | No        | [1.2.1](https://github.com/maxsupermanhd/meteor-villager-roller/releases/tag/1.2.1)                                                           |

Download links for older versions are here only for archival reasons, do not use them.

If you want to roll villagers on servers running older Minecraft versions please use [ViaFabricPlus](https://github.com/ViaVersion/ViaFabricPlus).

## Solving use-case

Addon is targeted for lazy people that are playing on server and trying to find enchantment on librarians. 
No functionality about other trades/professions implemented and planned, you can pull request them if you really want to.

## How-to

1. Acquire villager (not nitwit)
2. Acquire *some* lecterns (32 will be good)
3. (Recommended, not required) Acquire axe, better quality - faster rolling
4. Confine yourself and a villager from step 1 in a space where no freely moving entities and blocks are present (except villager and you)
5. Block villager from wandering around (slabs, blocks, stairs, *not trapdoors* since those mess with pathfinding)
6. Place a lectern where villager can access it and ensure that hand-rolling of profession is possible
7. Configure module to your needs and enable it
8. Follow selection of block and villager from chat messages
9. Leave module running alone (switching focus from window will likely open pause menu and *pause* rolling if "Pause on screens" option is enabled, disable it if you know what you are doing)
10. Profit!

## Common issues

- It says "We got your villager" twice and stops \
  Addon is designed to run on servers. Singleplayer and plugins can interfere with how merchant screens behave, nothing that can be done about it.
- I can not interact with villager anymore \
  See above, screen desync can be fixed by rejoining server. Nothing that can be done about it.
- It does not place lecterns back \
  It does place lecterns back, if it failed it means that block is impossible to place because player, villager, other entity or block is already occupying that block. Rolling process will hang because of this but will resume after manual block placement.
- It closes chat/overlay/screens while rolling \
  Yes it does because there is no way to not to, when interaction with villager is requested server will force client to open merchant screen (will replace any screen that is present) and opening that screen is required to get list of trades from the villager. Toggle "Pause on screens" will prevent new interactions if any screen is open (to aboid force closing it), after you done what you wanted you can manually interact with villager to resume rolling.
- It does not collect lecterns and fails to place them when they run out \
  Searching, pathfinding and moving to specific item on the ground is difficult and messy, there always a chance that all lecterns will fly away from player and it will end up with empty hands. You can optimize it by having hopper-dropper setup or flowing water if you really want to. Feel free to implement non destructive moving towards dropped item if you want.
- It always rolls to best level of enchantment \
  Update Villager Roller to 1.2

## Copying and credit

Integrating Villager Roller into other clients without modifying underlying featureset/codebase is allowed only with prior agreement or credit inside module settings/description. (Example: `Villager Roller by FlexCoral`)

## License

GPL-3.0, see LICENSE in project root

## I did not give permission to publish this mod on CurseForge
