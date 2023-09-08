package xyz.srnyx.eventalerts.listeners;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.eventalerts.EventAlerts;

import java.util.List;


public class UserListener extends ListenerAdapter {
    @NotNull private final EventAlerts eventAlerts;

    public UserListener(@NotNull EventAlerts eventAlerts) {
        this.eventAlerts = eventAlerts;
    }

    @Override
    public void onUserUpdateActivities(@NotNull UserUpdateActivitiesEvent event) {
        if (eventAlerts.config.advertisingStatus == null) return;

        // Get oldStatus and newStatus
        List<Activity> oldActivities = event.getOldValue();
        List<Activity> newActivities = event.getNewValue();
        if (oldActivities == null || newActivities == null) return;
        oldActivities = oldActivities.stream()
                .filter(activity -> activity.getType() == Activity.ActivityType.CUSTOM_STATUS)
                .toList();
        newActivities = newActivities.stream()
                .filter(activity -> activity.getType() == Activity.ActivityType.CUSTOM_STATUS)
                .toList();
        if (oldActivities.isEmpty() || newActivities.isEmpty()) return;
        final String oldStatus = oldActivities.get(0).getName().toLowerCase();
        final String newStatus = newActivities.get(0).getName().toLowerCase();

        // Get other variables
        final Guild guild = eventAlerts.config.guild.getGuild();
        if (guild == null) return;
        final Role role = guild.getRoleById(eventAlerts.config.guild.roles.advertising);
        if (role == null) return;
        final Member member = guild.retrieveMember(event.getUser()).complete();
        if (member == null) return;

        // Get hadStatus and hasStatus
        final boolean hadStatus = oldStatus.contains(eventAlerts.config.advertisingStatus);
        final boolean hasStatus = newStatus.contains(eventAlerts.config.advertisingStatus);

        // Add or remove role
        if (hadStatus && !hasStatus) {
            guild.removeRoleFromMember(member, role).queue();
        } else if (!hadStatus && hasStatus) {
            guild.addRoleToMember(member, role).queue();
        }
    }
}
