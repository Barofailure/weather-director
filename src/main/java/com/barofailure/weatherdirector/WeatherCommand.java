package com.barofailure.weatherdirector;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * /weatherdirector (alias /wd)          status
 * /wd set <kind> [minutes] [now]        force a kind (default: a rolled duration); "now" skips the build-up
 * /wd next                              roll the next kind now
 * /wd reload                            re-read config/weather_director.json
 */
final class WeatherCommand {
    private WeatherCommand() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(Commands.literal("weatherdirector")
                .requires(s -> s.hasPermission(2))
                .executes(WeatherCommand::status)
                .then(Commands.literal("set")
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(WeatherKind.values()).map(k -> k.name().toLowerCase(Locale.ROOT)), b))
                                .executes(c -> set(c, -1, false))
                                .then(Commands.literal("now").executes(c -> set(c, -1, true)))
                                .then(Commands.argument("minutes", DoubleArgumentType.doubleArg(0.05, 100000))
                                        .executes(c -> set(c, DoubleArgumentType.getDouble(c, "minutes"), false))
                                        .then(Commands.literal("now").executes(c -> set(c, DoubleArgumentType.getDouble(c, "minutes"), true))))))
                .then(Commands.literal("next").executes(c -> {
                    ServerLevel ow = c.getSource().getServer().overworld();
                    if (!WeatherDirector.controls(ow)) return fail(c);
                    WeatherDirector.begin(ow, WeatherDirector.pickNext(ow), -1, "/wd next by " + c.getSource().getTextName());
                    return status(c);
                }))
                .then(Commands.literal("reload").executes(c -> {
                    String msg = WeatherDirector.reloadConfig();
                    c.getSource().sendSuccess(() -> Component.literal(msg), true);
                    return 1;
                })));
        dispatcher.register(Commands.literal("wd").requires(s -> s.hasPermission(2)).executes(WeatherCommand::status).redirect(root));
    }

    private static int set(CommandContext<CommandSourceStack> c, double minutes, boolean now) {
        ServerLevel ow = c.getSource().getServer().overworld();
        if (!WeatherDirector.controls(ow)) return fail(c);
        WeatherKind kind = WeatherKind.parse(StringArgumentType.getString(c, "kind"));
        if (kind == null) {
            c.getSource().sendFailure(Component.literal("Unknown kind. Kinds: " + Arrays.toString(WeatherKind.values()).toLowerCase(Locale.ROOT)));
            return 0;
        }
        WeatherDirector.begin(ow, kind, minutes, "/wd set by " + c.getSource().getTextName() + (now ? " (now)" : ""));
        if (now) WeatherDirector.snap(ow);
        else c.getSource().sendSuccess(() -> Component.literal(
                "Building up naturally (clouds ~8 min, rain once they are thick, fog/mist ~1.5 min). Add 'now' to skip."), false);
        return status(c);
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        ServerLevel ow = c.getSource().getServer().overworld();
        WeatherState s = WeatherDirector.state();
        if (s == null || !WeatherDirector.controls(ow)) return fail(c);
        String msg = String.format(Locale.ROOT,
                "Weather %s, %.1f min left | clouds %.2f | rain %.2f -> %.2f | thunder %.2f -> %.2f | wind %d km/h from %s | season %s%s",
                s.kind.name().toLowerCase(Locale.ROOT), s.ticksLeft / (double) WeatherDirector.TICKS_PER_MINUTE, WeatherDirector.coverNow(),
                ow.getRainLevel(1f), s.kind.rain, ow.getThunderLevel(1f) / Math.max(0.0001f, ow.getRainLevel(1f)), s.kind.thunder,
                Math.round(s.windSpeed * 60), compassFrom(s.windAngle),
                Seasons.current(ow).toLowerCase(Locale.ROOT),
                ow.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE) ? "" : " | PAUSED (doWeatherCycle false)");
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    /** Wind is named after where it comes FROM; angle is where it blows to (0 = east/+X, 90 = south/+Z). */
    private static String compassFrom(float towardRadians) {
        String[] names = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};
        double from = Math.toDegrees(towardRadians) + 180;
        int i = (int) Math.floorMod(Math.round(from / 45.0), 8L);
        return names[i];
    }

    private static int fail(CommandContext<CommandSourceStack> c) {
        c.getSource().sendFailure(Component.literal("Weather Director is not running"));
        return 0;
    }
}
