package xyz.srnyx.eventalerts.commands.global;

import com.freya02.botcommands.api.annotations.CommandMarker;
import com.freya02.botcommands.api.annotations.Dependency;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandScope;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GlobalSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.Booster;

import xyz.srnyx.lazylibrary.LazyEmoji;


@CommandMarker
public class BoosterPassCmd extends ApplicationCommand {
    @Dependency private EventAlerts eventAlerts;

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "boosterpass",
            subcommand = "list",
            description = "List your current booster passes")
    public void commandList(@NotNull GlobalSlashEvent event) {
        // Get member
        final Guild guild = eventAlerts.config.guild.getGuild();
        if (guild == null) {
            event.reply(LazyEmoji.NO + " An unexpected error occurred!").setEphemeral(true).queue();
            return;
        }
        final Member member = guild.retrieveMember(event.getUser()).complete();
        if (member == null) {
            event.reply(LazyEmoji.NO + " An unexpected error occurred!").setEphemeral(true).queue();
            return;
        }

        // Check if still boosting
        final Booster booster = eventAlerts.getMongoCollection(Booster.class).findOne("user", member.getIdLong());
        if (!member.isBoosting()) {
            if (booster != null) booster.removePasses(eventAlerts);
            event.reply(LazyEmoji.NO + " You aren't boosting the server!").setEphemeral(true).queue();
            return;
        }

        // Check if user has any passes
        if (booster == null || booster.passes.isEmpty()) {
            if (booster != null) eventAlerts.getMongoCollection(Booster.class).deleteOne("_id", booster.id);
            event.reply(LazyEmoji.NO + " You don't have any booster passes!").setEphemeral(true).queue();
            return;
        }

        // Send passes
        final StringBuilder passes = new StringBuilder();
        booster.passes.forEach(pass -> passes.append("<@").append(pass).append("> & "));
        passes.delete(passes.length() - 3, passes.length());
        event.reply(LazyEmoji.YES + " You've given **" + booster.passes.size() + "/2** of your passes to: " + passes).setEphemeral(true).queue();
    }

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "boosterpass",
            subcommand = "give",
            description = "Give a booster pass to a user")
    public void commandGive(@NotNull GlobalSlashEvent event,
                            @AppOption(description = "The user to give the pass to") @NotNull User user) {
        final CheckData data = runChecks(event, user);
        if (data == null) return;

        // Check if user has already given 2 passes
        if (data.booster != null && data.booster.passes.size() >= 2) {
            event.reply(LazyEmoji.NO + " You've already used all of your passes (2)!").setEphemeral(true).queue();
            return;
        }

        // Check if user is giving to self
        final long userId = event.getUser().getIdLong();
        final long targetId = user.getIdLong();
        if (targetId == userId) {
            event.reply(LazyEmoji.NO + " You can't give a pass to yourself!").setEphemeral(true).queue();
            return;
        }

        // Check if member already has a pass
        final String mention = user.getAsMention();
        if (data.member.getRoles().contains(data.role)) {
            event.reply(LazyEmoji.NO + " " + mention + " already has a pass!").setEphemeral(true).queue();
            return;
        }

        // Give pass
        eventAlerts.getMongoCollection(Booster.class).collection.updateOne(Filters.eq("user", userId), Updates.addToSet("passes", targetId), new UpdateOptions().upsert(true));
        data.guild.addRoleToMember(data.member, data.role)
                .flatMap(v -> event.reply(LazyEmoji.YES + " You've given a pass to " + mention + "!").setEphemeral(true))
                .queue();
    }

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "boosterpass",
            subcommand = "remove",
            description = "Remove a booster pass from a user")
    public void commandRemove(@NotNull GlobalSlashEvent event,
                              @AppOption(description = "The user to remove the pass from") @NotNull User user) {
        final CheckData data = runChecks(event, user);
        if (data == null) return;

        // Check if user has any passes
        if (data.booster == null || data.booster.passes.isEmpty()) {
            if (data.booster != null) eventAlerts.getMongoCollection(Booster.class).deleteOne("_id", data.booster.id);
            event.reply(LazyEmoji.NO + " You aren't using any of your passes!").setEphemeral(true).queue();
            return;
        }

        // Check if user is removing from self
        final long userId = event.getUser().getIdLong();
        final long targetId = user.getIdLong();
        if (targetId == userId) {
            event.reply(LazyEmoji.NO + " You can't remove a pass from yourself!").setEphemeral(true).queue();
            return;
        }

        // Check if member has a pass
        final String mention = user.getAsMention();
        if (!data.member.getRoles().contains(data.role)) {
            event.reply(LazyEmoji.NO + " " + mention + " doesn't have a pass!").setEphemeral(true).queue();
            return;
        }

        // Remove pass
        eventAlerts.getMongoCollection(Booster.class).collection.updateOne(Filters.eq("user", userId), Updates.pull("passes", targetId));
        data.guild.removeRoleFromMember(data.member, data.role)
                .flatMap(v -> event.reply(LazyEmoji.YES + " You've removed a pass from " + mention + "!").setEphemeral(true))
                .queue();
    }

    @Nullable
    private CheckData runChecks(@NotNull GlobalSlashEvent event, @NotNull User user) {
        // Get guild
        final Guild guild = eventAlerts.config.guild.getGuild();
        if (guild == null) {
            event.reply(LazyEmoji.NO + " An unexpected error occurred!").setEphemeral(true).queue();
            return null;
        }

        // Get role
        final Role role = guild.getRoleById(eventAlerts.config.guild.roles.boosterPass);
        if (role == null) {
            event.reply(LazyEmoji.NO + " An unexpected error occurred!").setEphemeral(true).queue();
            return null;
        }

        // Get member
        final Member member = guild.retrieveMember(user).complete();
        if (member == null) {
            event.reply(LazyEmoji.NO + " " + user.getAsMention() + " isn't in the server!").setEphemeral(true).queue();
            return null;
        }

        // Check if still boosting
        final long userId = event.getUser().getIdLong();
        final Booster booster = eventAlerts.getMongoCollection(Booster.class).findOne("user", userId);
        if (!guild.retrieveMemberById(userId).complete().isBoosting()) {
            if (booster != null) booster.removePasses(eventAlerts);
            event.reply(LazyEmoji.NO + " You aren't boosting the server!").setEphemeral(true).queue();
            return null;
        }

        return new CheckData(booster, guild, member, role);
    }

    private record CheckData(@Nullable Booster booster, @NotNull Guild guild, @NotNull Member member, @NotNull Role role) {}
}
