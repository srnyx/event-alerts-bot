package xyz.srnyx.eventalerts;

import net.dv8tion.jda.api.entities.MessageEmbed;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.lazylibrary.LazyEmbed;

import java.awt.*;


public class EaEmbed {
    @NotNull private final EventAlerts eventAlerts;

    public EaEmbed(@NotNull EventAlerts eventAlerts) {
        this.eventAlerts = eventAlerts;
    }

    @NotNull
    public MessageEmbed noPermission() {
        return new LazyEmbed()
                .setColor(Color.RED)
                .setTitle("No permission!")
                .setDescription("You don't have permission to do that!")
                .build(eventAlerts);
    }

    @NotNull
    public MessageEmbed invalidArgument(@NotNull String argument, @NotNull Object value) {
        return new LazyEmbed()
                .setColor(Color.RED)
                .setTitle("Invalid argument!")
                .addField(argument, value.toString(), true)
                .build(eventAlerts);
    }
}
