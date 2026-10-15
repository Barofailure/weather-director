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

- **Wind you can hear.** A soft breeze and a strong, gusty wind follow the same one wind as the clouds and the rain:
  silent in calm air, a breeze that comes and goes, roaring in a thunderstorm, louder in the open and up high, muffled indoors, silent underwater.
  The wind recordings are made for this mod and ship inside it.
- **Rain sounds follow the rain.** Drizzle is soft, heavy rain is loud. This also applies to the rain and snow sounds of
  AmbientSounds and Particle Rain.

## Works with (all optional)

| Mod | Adds |
|---|---|
| [Ecliptic Seasons](https://modrinth.com/mod/ecliptic-seasons) | the season picks the weather: spring showers, summer storms, autumn fog, winter snow |
| [Better Clouds](https://modrinth.com/mod/better-clouds) | cloud cover, grey storm clouds, shared drift, a cloud deck you can fly into |
| [Particle Rain](https://modrinth.com/mod/particle-rain) | wind-blown rain and snow, light drizzle, soft ground fog |
| [Distant Horizons](https://modrinth.com/mod/distanthorizons) | weather fog on the far terrain, no ring at the render distance |
| [AmbientSounds](https://modrinth.com/mod/ambientsounds) | its rain follows the rain strength; its own wind (which ignores the weather) is muted so you hear one wind |

Without them the weather still changes for everyone, only plainer. Set Ecliptic Seasons `UseSolarWeather = false`
so its own weather does not compete.

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
- **Client:** Mods, Weather Director, Config (or `config/weather_director-client.toml`): clouds, grey skies,
  seasonal skies, wind on rain, ground fog amount, particle budget, view fog, wind sound and its volume, rain sound
  following the rain, muting AmbientSounds' wind. The wind plays on the game's Weather volume slider.
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
