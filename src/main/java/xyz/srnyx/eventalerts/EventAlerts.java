package xyz.srnyx.eventalerts;

import com.mongodb.client.model.Filters;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.eventalerts.listeners.UserListener;
import xyz.srnyx.eventalerts.mongo.EaMongo;
import xyz.srnyx.eventalerts.mongo.objects.Strike;

import xyz.srnyx.lazylibrary.LazyEmbed;
import xyz.srnyx.lazylibrary.LazyLibrary;
import xyz.srnyx.lazylibrary.settings.LazySettings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class EventAlerts extends LazyLibrary {
    @NotNull public final EaConfig config = new EaConfig(this);
    @NotNull public final EaMongo mongo = new EaMongo(config.mongo);
    @NotNull public final EaEmbed embeds = new EaEmbed(this);
    @NotNull public final Map<Long, Long> userEventCooldowns = new ConcurrentHashMap<>();

    public EventAlerts() {
        // Listeners
        jda.addEventListener(new UserListener(this));

        // Presence (status)
        jda.getPresence().setActivity(Activity.playing("oink oink"));

        // Check ended events & expired strikes
        mongo.getEndedEvents().forEach(event -> event.delete(this));
        mongo.strikeCollection.findMany(Filters.exists("expire")).stream()
                .filter(Strike::hasExpired)
                .forEach(strike -> strike.delete(this));

        // Start event checking
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> mongo.getEndedEvents().forEach(event -> event.notifySubscribers(this)), 1, 1, TimeUnit.MINUTES);

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
                .gatewayIntents(
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_PRESENCES)
                .disabledCacheFlags(
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
