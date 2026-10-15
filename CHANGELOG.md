# Changelog

## 1.2.0

- The wind knows where you are. Every wind (breeze and strong wind, from a whisper to a storm) has three versions that
  play in step and blend as you move: in the open; sheltered under a roof with open sides (porch, shed, forest
  canopy, under a balloon): duller, the roof drums in the gusts; indoors: muffled pressure on the walls, a thump with
  each strong gust and a thin whistle, coming from the nearest opening. Deep inside, underground or in a sealed room
  it is silent. Works on airships too.
- Strong wind rattles the nearest window and makes nearby wood creak.
- New client settings: indoor wind, rattling windows and creaking wood.

## 1.1.0

- Wind sound: a soft breeze and a strong, gusty wind that follow the one global wind. Calm air is silent, a light
  breeze only comes and goes, storms roar. Louder in the open and up high,
  muffled indoors and underground, silent underwater. Plays on the Weather volume slider; own volume setting.
- Rain sounds follow the rain strength: drizzle is soft, rain medium, heavy rain full. Covers the game's rain and the
  rain and snow sounds of AmbientSounds and Particle Rain.
- With AmbientSounds installed, its own wind (based only on height) is muted by a built-in resource pack, so the wind
  is heard once. Can be turned off in the settings.
- `/wd wind <km/h>` holds the wind at a speed for testing, `/wd wind auto` gives it back to the weather.
- New client settings: wind sound, wind volume, rain sound follows the rain, mute AmbientSounds' wind.

## 1.0.1

- Night mist and fog no longer turn black. They are moonlit blue-grey now, a little lighter than the night sky.
- Storm clouds stay dark grey at night instead of going pitch black, also on new-moon nights.

## 1.0.0

First public release.

- One global weather with ten kinds: clear, fair, cloudy, overcast, mist, fog, drizzle, rain, heavy rain, thunderstorm.
- Season weights through Ecliptic Seasons, fronts, slow build-up, dawn fog and mist.
- Client part: weather-driven Better Clouds, wind-blown Particle Rain, ground fog, Distant Horizons far fog,
  grey and seasonal skies, cloud deck with fog inside and no rain above.
- Client settings screen, vanilla settings respected, particle budget and frame rate guard.
- Optional mods are version checked: an untested major version only switches that mod's part off.
- Players without the mod can join.
