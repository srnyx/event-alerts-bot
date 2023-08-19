package xyz.srnyx.eventalerts;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.eventalerts.mongo.EaMongo;

import xyz.srnyx.lazylibrary.LazyEmbed;
import xyz.srnyx.lazylibrary.LazyLibrary;
import xyz.srnyx.lazylibrary.settings.LazySettings;

import java.util.Random;
import java.util.function.Consumer;


public class EventAlerts extends LazyLibrary {
    @NotNull public static final Random RANDOM = new Random();

    @NotNull public final EaConfig config = new EaConfig(this);
    @NotNull public final EaMongo mongo = new EaMongo(this);
    @NotNull public final EaEmbed embeds = new EaEmbed(this);

    public EventAlerts() {
        // Register listeners
        jda.addEventListener();

        // Presence (status)
        jda.getPresence().setActivity(Activity.playing("oink oink"));

        // Status log message
        LOGGER.info("Event Alerts has finished starting!");
    }

    @Override @NotNull
    public Consumer<LazySettings> getSettings() {
        return newSettings -> newSettings
                .searchPaths(
                        "xyz.srnyx.eventalerts.apps",
                        "xyz.srnyx.eventalerts.commands",
                        "xyz.srnyx.eventalerts.components")
                .disabledCacheFlags(
                        CacheFlag.ACTIVITY,
                        CacheFlag.EMOJI,
                        CacheFlag.VOICE_STATE,
                        CacheFlag.STICKER,
                        CacheFlag.CLIENT_STATUS,
                        CacheFlag.ONLINE_STATUS,
                        CacheFlag.SCHEDULED_EVENTS)
                .embedDefault(LazyEmbed.Key.COLOR, 10977346)
                .embedDefault(LazyEmbed.Key.FOOTER_TEXT, "Event Alerts")
                .embedDefault(LazyEmbed.Key.FOOTER_ICON, "https://us-east-1.tixte.net/uploads/media.srnyx.com/eventalerts.png");
    }

    public static void main(@NotNull String[] arguments) {
        new EventAlerts();
    }
}
