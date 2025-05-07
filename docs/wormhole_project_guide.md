
## Running

If running in an IDE like intellij, you can use the usual run configuration.

If running outside of an IDE, the assets need to be unpacked and shipped external to the jar. This
can be achieved with the `gradle copyAssets` task, saving them to the `run/assets/` folder.

## Configuration

There are two main configs for modifying how the engine runs. These, for speed, are just Java constants.
See core -> EngineProperties for shared config entries.
See wormholes -> GameProperties for portal-project specific config entries.

## Controls

***Function Keys:***

| Key  | Action                                                         |
|------|----------------------------------------------------------------|
| ESC  | Un-captures mouse cursor                                       |
| END  | Exits the game                                                 |
| F1   | *On supported renderers,* toggles wireframe view               |
| F2   | Prints blue & orange portal bounding boxes to the log          |
| F3   | Prints current camera position, rotation, and facing direction |
| F4   | Toggles solid portal rendering                                 |
| F5   | Toggle player model rendering                                  |


***Portal Debugging***

Portals toggle with each key. If a portal of a colour is spawned, pressing *any* key for that colour will despawn it.
You can have at-most 1 blue and 1 orange portal at a time.

| Key | Portal | Location                                                        |
|-----|--------|-----------------------------------------------------------------|
| 1   | Orange | Main Room, elevated above the small plinth.                     |
| 2   |        | Lower corridor                                                  |
| 3   |        | Main Room, at ground level to the left of the small plinth      |
| 4   |        | Main Room, ceiling above small plinth.                          |
| 5   |        | N/A                                                             |
| 6   | Blue   | N/A                                                             |
| 7   |        | Main Room, floor on small plinth.                               |
| 8   |        | Main Room, at ground level next to the lower corridor entrance. |
| 9   |        | Main room, under balcony                                        |
| 0   |        | Lower corridor                                                  |


***Camera Controls***

As standard, WASD and a mouse can be used to control the camera with a first-person fly camera.

For the sake of fair testing, there are a few pre-saved camera angles:

| Key | Location                                                |
|-----|---------------------------------------------------------|
| i   | Void, far away from geometry.                           |
| o   | Origin point                                            |
| p   | Lower corridor, recursion (portals 2 & 0) facing.       |
| j   | Lower corridor, containing both portal targets (2 & 0). |
| k   | Upper Balcony, facing main room.                        |
| l   | Upper balcony, facing wall away from room.              |
