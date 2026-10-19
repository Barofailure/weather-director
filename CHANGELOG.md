# Changelog

## 1.3.0

- The wind is now real recorded wind: AmbientSounds' recordings (two breezes, two strong winds and a storm howl),
  played by the weather so they follow the one global wind, its gusts and the height. Each pair slowly blends from one
  recording to the other, so no loop is heard repeating. Up on mountains it blows harder than in the valley.
- Where you are still counts: full in the open, quieter and duller under a roof, quiet and muffled indoors (a real
  low-pass filter), silent in caves, sealed rooms, deep underground and underwater.
- AmbientSounds' own generic wind is replaced (heard once, never twice); its badlands wind and wind in the leaves stay
  and follow the weather's wind too.
- The wind sound needs AmbientSounds now; without it the weather is silent apart from rain and thunder. The
  generated wind loops of 1.1.0 and 1.2.0 are gone, and with them the setting "Mute AmbientSounds' wind".

## 1.2.1

- Fixed: a crash when the wind faded out (for example on entering the End) while the game had no working sound output.

## 1.2.0

- The wind knows where you are. Every wind (breeze and strong wind, from a whisper to a storm) has three versions that
  play in step and blend as you move: in the open; sheltered under a roof with open sides (porch, shed, forest
  canopy, under a balloon): duller, the roof drums in the gusts; indoors: muffled pressure on the walls, a thump with
  each strong gust and a thin whistle, coming from the nearest opening. Deep inside, underground or in a sealed room
  it is silent. Works on airships too.
- Rain and thunder know where you are too. Under a roof the rain is a bit quieter, indoors about half as loud, in a cave
  or sealed room quieter again; thunder the same, and indoors it also sounds deeper. From about 10 blocks below the
  surface both fade out and are silent from about 30 blocks down.
- Thunder comes after the flash: the further away the strike, the later you hear it (speed of sound).
- Fixed: the rain sounds of the game itself (not only AmbientSounds) now really follow the rain strength.
- No wind in caves, however big: the wind needs sky light; at a cave mouth only a little.
- Fixed: above the clouds it now stops raining also while the cloud deck is still building or thinning out (light
  rain used to keep falling above it then, however high you flew).
- New client settings: indoor wind, rain and thunder indoors, thunder after the flash.

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
