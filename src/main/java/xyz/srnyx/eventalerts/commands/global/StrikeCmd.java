package xyz.srnyx.eventalerts.commands.global;

import com.freya02.botcommands.api.annotations.CommandMarker;
import com.freya02.botcommands.api.annotations.Dependency;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandScope;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GlobalSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.objects.Strike;

import xyz.srnyx.lazylibrary.LazyEmbed;
import xyz.srnyx.lazylibrary.LazyEmoji;
import xyz.srnyx.lazylibrary.LazyLibrary;
import xyz.srnyx.lazylibrary.utility.DurationParser;

import java.time.Duration;
import java.util.List;


@CommandMarker
public class StrikeCmd extends ApplicationCommand {
    @NotNull private static final String AC_DELETE_ID = "StrikeCmd.ac.delete.id";

    @Dependency private EventAlerts eventAlerts;

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "strike",
            subcommand = "list",
            description = "List all strikes of a partner")
    public void list(@NotNull GlobalSlashEvent event,
                     @AppOption(description = "The partner") @NotNull Member partner) {
        if (eventAlerts.config.guild.roles.checkDontHaveRole(event, eventAlerts.config.guild.roles.mod)) return;
        final long partnerId = partner.getIdLong();
        final String mention = partner.getAsMention();

        // Check if partner
        if (!eventAlerts.config.guild.roles.hasRole(partnerId, eventAlerts.config.guild.roles.partner)) {
            event.reply(LazyEmoji.NO + " " + mention + " is not a partner!").setEphemeral(true).queue();
            return;
        }

        // Check if partner has strikes
        final List<Strike> strikes = eventAlerts.mongo.getStrikes(partnerId);
        if (strikes.isEmpty()) {
            event.reply(LazyEmoji.NO + " " + mention + " has no strikes!").setEphemeral(true).queue();
            return;
        }

        // Send message
        event.replyEmbeds(new LazyEmbed()
                        .setTitle(partner.getEffectiveName() + "'s Strikes")
                        .setDescription("**Total:** " + strikes.size())
                        .setThumbnail(partner.getUser().getEffectiveAvatarUrl()).addFields(strikes.stream()
                                .map(Strike::toField)
                                .toList())
                        .build(eventAlerts))
                .setEphemeral(true).queue();
    }

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "strike",
            subcommand = "add",
            description = "Add a strike to a partner")
    public void add(@NotNull GlobalSlashEvent event,
                    @AppOption(description = "The partner") @NotNull Member partner,
                    @AppOption(description = "The reason") @NotNull String reason,
                    @AppOption(description = "When to expire") @Nullable String expire) {
        if (eventAlerts.config.guild.roles.checkDontHaveRole(event, eventAlerts.config.guild.roles.mod)) return;
        final long partnerId = partner.getIdLong();
        final String mention = partner.getAsMention();

        // Check if partner
        if (!eventAlerts.config.guild.roles.hasRole(partnerId, eventAlerts.config.guild.roles.partner)) {
            event.reply(LazyEmoji.NO + " " + mention + " is not a partner!").setEphemeral(true).queue();
            return;
        }

        // Parse expire
        Long expireTime = null;
        if (expire != null) {
            final Duration duration = DurationParser.parse(expire);
            if (duration == null) {
                event.replyEmbeds(eventAlerts.embeds.invalidArgument("expire", expire)).setEphemeral(true).queue();
                return;
            }
            expireTime = System.currentTimeMillis() + duration.toMillis();
        }

        // Get strikes channel
        final GuildMessageChannel channel = eventAlerts.config.guild.channels.getStrikes();
        if (channel == null) {
            event.reply(LazyEmoji.NO + " Strikes channel not found!").setEphemeral(true).queue();
            return;
        }

        // Add strike
        final User partnerUser = partner.getUser();
        final String name = partnerUser.getName();
        final Strike strike = new Strike(eventAlerts, partnerId, reason, event.getUser().getIdLong(), expireTime);
        final int strikes = eventAlerts.mongo.getStrikes(partnerId).size();
        event.replyEmbeds(strike.toEmbed()
                        .setTitle("Strike #" + strikes + " for " + name)
                        .setDescription(LazyEmoji.YES + " Successfully added a strike to " + mention)
                        .build(eventAlerts))
                .setEphemeral(true)
                .flatMap(message -> channel.sendMessageEmbeds(strike.toEmbed()
                        .setTitle("Strike #" + strikes + " for " + name)
                        .build(eventAlerts)))
                .flatMap(message -> {
                    eventAlerts.mongo.strikeCollection.updateOne(Filters.eq("strike_id", strike.strikeId), Updates.set("message", message.getIdLong()));
                    return partnerUser.openPrivateChannel();
                })
                .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(strike.toEmbed()
                        .setTitle("Strike #" + strikes)
                        .build(eventAlerts)))
                .queue(s -> {}, e -> LazyLibrary.LOGGER.warn("Failed to send strike message for " + name, e));
    }

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "strike",
            subcommand = "delete",
            description = "Delete a strike")
    public void delete(@NotNull GlobalSlashEvent event,
                       @AppOption(description = "The ID of the strike to delete", autocomplete = AC_DELETE_ID) int id) {
        if (eventAlerts.config.guild.roles.checkDontHaveRole(event, eventAlerts.config.guild.roles.mod)) return;

        // Get strike
        final Strike strike = eventAlerts.mongo.strikeCollection.findOne("strike_id", id);
        if (strike == null) {
            event.reply(LazyEmoji.NO + " Strike not found!").setEphemeral(true).queue();
            return;
        }

        // Delete strike
        strike.delete(eventAlerts);
        event.reply(LazyEmoji.YES + " Strike **#" + id + "** for <@" + strike.user + "> was successfully deleted").setEphemeral(true).queue();
    }

    @AutocompletionHandler(name = AC_DELETE_ID) @NotNull
    public List<Command.Choice> acDeleteId(@NotNull CommandAutoCompleteInteractionEvent event) {
        return eventAlerts.mongo.strikeCollection.findMany(Filters.exists("strike_id")).stream()
                .map(strike -> new Command.Choice(strike.strikeId.toString(), strike.strikeId))
                .toList();
    }
}
