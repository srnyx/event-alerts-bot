package xyz.srnyx.eventalerts.listeners;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.Booster;

import xyz.srnyx.lazylibrary.LazyListener;
import xyz.srnyx.lazylibrary.events.GuildMemberStopBoost;


public class GuildMemberListener extends LazyListener {
    @NotNull private final EventAlerts eventAlerts;

    public GuildMemberListener(@NotNull EventAlerts eventAlerts) {
        this.eventAlerts = eventAlerts;
    }

    @Override
    public void onGuildMemberStopBoosting(@NotNull GuildMemberStopBoost event) {
        final Booster booster = eventAlerts.getMongoCollection(Booster.class).findOne("user", event.getMember().getIdLong());
        if (booster != null) booster.removePasses(eventAlerts);
    }
}
