# Weather Director

One weather for the whole world, chosen by the season and built up slowly, like real weather.
Weather Director decides what the sky does. Better Clouds, Particle Rain and Distant Horizons play it out when they are installed.

NeoForge 1.21.1. Made for the [Barofailure](https://barofailure.com) server. [Discord](https://discord.gg/29mfTen8f5)

## Weathers

| Weather | What you see |
|---|---|
| Clear, fair, cloudy | from blue sky to more and more clouds drifting with the wind |
| Overcast | a closed grey deck, sun and stars hidden |
| Mist | light white ground fog, mostly over water |
| Fog | dense grey ground fog on land, the view closes in |
| Drizzle | single light drops |
| Rain | steady rain, slanting with the wind |
| Heavy rain | dense and dark, thick streaks once it has built up |
| Thunderstorm | darkest and windiest, the distance turns slightly hazy |

The whole overworld has the same weather. It follows fronts (clear, clouds, rain, clearing) and builds up over minutes:
clouds first, rain only once they are thick. In winter it snows. Fog and mist come at dawn and lift again.

## Sound

- **Wind you can hear.** Real recorded wind (AmbientSounds' recordings, so AmbientSounds is needed for it) follows the
  same one wind as the clouds and the rain: silent in calm air, a breeze that comes and goes, roaring and howling in a
  thunderstorm, louder in the open and up high. Two recordings per kind blend slowly, so no loop is heard repeating.
- **The wind knows where you are.** In the open you hear it full; under a roof it is quieter and duller; indoors quiet
  and muffled. In caves, sealed rooms, deep underground and underwater it is silent. Works on airships too.
- **Rain sounds follow the rain.** Drizzle is soft, heavy rain is loud. This also applies to the rain and snow sounds of
  AmbientSounds and Particle Rain.
- **Rain and thunder indoors.** Under a roof and indoors rain and thunder are quieter, thunder sounds deeper inside, and
  deep underground both are silent. Thunder comes after the flash, later the further away the strike.

## Works with (all optional)

| Mod | Adds |
|---|---|
| [Ecliptic Seasons](https://modrinth.com/mod/ecliptic-seasons) | the season picks the weather: spring showers, summer storms, autumn fog, winter snow |
| [Better Clouds](https://modrinth.com/mod/better-clouds) | cloud cover, grey storm clouds, shared drift, a cloud deck you can fly into |
| [Particle Rain](https://modrinth.com/mod/particle-rain) | wind-blown rain and snow, light drizzle, soft ground fog |
| [Distant Horizons](https://modrinth.com/mod/distanthorizons) | weather fog on the far terrain, no ring at the render distance |
| [Iris](https://modrinth.com/mod/iris) | shader packs can read the weather (see Shader packs below); under a shader pack that draws its own clouds there is no cloud deck of ours, so rain falls at every height |
| [AmbientSounds](https://modrinth.com/mod/ambientsounds) | its rain follows the rain strength, its rain and storm get quieter indoors; the wind plays its recordings (needed for the wind sound); its own generic wind is replaced so you hear one wind, its badlands wind and wind in the leaves follow the weather's wind |

Without them the weather still changes for everyone, only plainer. Set Ecliptic Seasons `UseSolarWeather = false`
so its own weather does not compete.

## Shader packs

With Iris, every shader program gets these uniforms, so a shader pack can draw the same weather as Better Clouds does
without shaders. Declare the ones you use (`uniform float wdCover;`); packs that do not declare them are not affected.

| Uniform | Meaning |
|---|---|
| `float wdActive` | 1 while Weather Director drives the sky (overworld of a Weather Director server), else 0: fall back to your own weather then |
| `float wdCover` | cloud cover, 0 clear .. 1 a closed deck (clear 0.05, fair 0.5, cloudy 0.66, overcast 0.9, rain 0.95, storms 1) |
| `float wdGloom` | how grey sky, clouds and fog look, 0 .. 1, at least `rainStrength` (overcast 0.55, rain 0.7, storms 1) |
| `float wdLightGloom` | how much daylight is dimmed, 0 .. 1 (overcast dims less than rain) |
| `float wdFogEnd` | blocks at which weather fog (fog, thunderstorms) is opaque; 0 = no weather fog |
| `float wdGroundFog` | how much fog lies on the ground, 0 .. 1 (follows the ground fog setting; 0 above the clouds) |
| `float wdMist` | how much mist lies on the ground, 0 .. 1 (light and white; must not shorten the view much) |
| `vec2 wdCloudOffset` | how far the clouds have drifted with the wind, in blocks (x, z), wrapped every 36000 blocks |

The client setting "shader weather" turns them off (all 0). The Iris hook only runs for tested Iris versions (1.8.x).

## Server and client

- **Server:** required. It runs the weather and sends it with the normal game packets.
- **Client:** optional. Players without the mod can join and see vanilla rain at the right strength.
  With the mod they also get clouds, wind, fog and the seasonal skies.

## Commands (operators)

| Command | Does |
|---|---|
| `/wd` | current weather, clouds, rain, wind, season |
| `/wd set <weather> [minutes] [now]` | switch the weather; `now` skips the build-up |
| `/wd next` | roll the next weather |
| `/wd wind <km/h>` / `/wd wind auto` | hold the wind at a speed (testing) / give it back to the weather |
| `/wd reload` | re-read the config |

`/weatherdirector` is the long name. `gamerule doWeatherCycle false` pauses the weather.

## Settings

- **Server:** `config/weather_director.json`: how long each weather lasts, what follows it, how it looks, season weights.
- **Client:** Mods, Weather Director, Config (or `config/weather_director-client.toml`): clouds, shader weather, grey skies,
  seasonal skies, wind on rain, ground fog amount, particle budget, view fog, wind sound and its volume, indoor wind,
  rain sound following the rain, rain and thunder indoors, thunder after the flash. The wind plays on the game's Weather
  volume slider.
- Vanilla settings are respected: Particles scales the ground fog, a manual Clouds Off stays off,
  Hide Lightning Flashes works as usual.

## Performance

Measured on a full modpack: about 0.02 % of server CPU and under 1 % of a client frame.
Ground fog stops at a particle budget and backs off when the frame rate drops below 30.

## Planned

More weather and sky events: blizzard, rainbow, aurora, hail, sleet, sun showers, heat lightning, gales,
dust storms, shooting stars.

## Building

`./gradlew build` (JDK 21). The optional mods are compile-only dependencies from the Modrinth maven.

## License

MIT, see [LICENSE](LICENSE).
