package xyz.srnyx.eventalerts;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.lazylibrary.LazyListener;


public class EaListener extends LazyListener {
    @NotNull protected final EventAlerts eventAlerts;

    public EaListener(@NotNull EventAlerts eventAlerts) {
        this.eventAlerts = eventAlerts;
    }

    @Override @NotNull
    public EventAlerts getBot() {
        return eventAlerts;
    }
}
