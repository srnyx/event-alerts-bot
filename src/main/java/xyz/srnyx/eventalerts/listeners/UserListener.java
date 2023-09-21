package xyz.srnyx.eventalerts.listeners;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.user.UserActivityEndEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.eventalerts.EventAlerts;


public class UserListener extends ListenerAdapter {
    @NotNull private final EventAlerts eventAlerts;

    public UserListener(@NotNull EventAlerts eventAlerts) {
        this.eventAlerts = eventAlerts;
    }

    @Override
    public void onUserActivityStart(@NotNull UserActivityStartEvent event) {
        if (eventAlerts.config.advertisingStatus == null) return;
        final Activity activity = event.getNewActivity();
        if (activity.getType() != Activity.ActivityType.CUSTOM_STATUS || !activity.getName().toLowerCase().contains(eventAlerts.config.advertisingStatus)) return;
        final Guild guild = eventAlerts.config.guild.getGuild();
        if (guild == null) return;
        final Role role = guild.getRoleById(eventAlerts.config.guild.roles.advertising);
        if (role == null) return;
        final Member member = guild.retrieveMember(event.getUser()).complete();
        if (member != null && !member.getRoles().contains(role)) guild.addRoleToMember(member, role).queue();
    }

    @Override
    public void onUserActivityEnd(@NotNull UserActivityEndEvent event) {
        if (eventAlerts.config.advertisingStatus == null) return;
        final Activity activity = event.getOldActivity();
        if (activity.getType() != Activity.ActivityType.CUSTOM_STATUS || !activity.getName().toLowerCase().contains(eventAlerts.config.advertisingStatus)) return;
        final Guild guild = eventAlerts.config.guild.getGuild();
        if (guild == null) return;
        final Role role = guild.getRoleById(eventAlerts.config.guild.roles.advertising);
        if (role == null) return;
        final Member member = guild.retrieveMember(event.getUser()).complete();
        if (member != null && member.getRoles().contains(role)) guild.removeRoleFromMember(member, role).queue();
    }
}
