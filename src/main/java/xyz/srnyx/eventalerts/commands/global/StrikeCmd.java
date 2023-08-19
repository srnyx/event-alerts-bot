package xyz.srnyx.eventalerts.commands.global;

import com.freya02.botcommands.api.annotations.CommandMarker;
import com.freya02.botcommands.api.annotations.Dependency;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandScope;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GlobalSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.objects.Strike;

import xyz.srnyx.lazylibrary.LazyEmbed;
import xyz.srnyx.lazylibrary.LazyEmoji;
import xyz.srnyx.lazylibrary.utility.DurationParser;

import java.time.Duration;
import java.util.List;


@CommandMarker
public class StrikeCmd extends ApplicationCommand {
    @Dependency private EventAlerts eventAlerts;

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "strike",
            subcommand = "list",
            description = "List all strikes of a partner")
    public void list(@NotNull GlobalSlashEvent event,
                     @AppOption(description = "The partner") @NotNull Member partner) {
        if (!eventAlerts.config.guild.roles.checkIsMod(event)) return;
        final long partnerId = partner.getIdLong();
        final String mention = partner.getAsMention();

        // Check if partner
        if (!eventAlerts.config.guild.roles.isPartner(partnerId)) {
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
        event.replyEmbeds(generateEmbed(partner, strikes)).setEphemeral(true).queue();
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
        if (!eventAlerts.config.guild.roles.checkIsMod(event)) return;
        final long partnerId = partner.getIdLong();
        final String mention = partner.getAsMention();

        // Check if partner
        if (!eventAlerts.config.guild.roles.isPartner(partnerId)) {
            event.reply(LazyEmoji.NO + " " + mention + " is not a partner!").setEphemeral(true).queue();
            return;
        }

        // Parse expire
        Long expireTime = null;
        if (expire != null) {
            final Duration duration = DurationParser.parse(expire);
            if (duration == null) {
                event.reply(LazyEmoji.NO + " Invalid expire time: `" + expire + "`!").setEphemeral(true).queue();
                return;
            }
            expireTime = System.currentTimeMillis() + duration.toMillis();
        } else {
            expire = "never";
        }

        // Add strike
        event.reply(LazyEmoji.YES + " Added strike #" + new Strike(eventAlerts.mongo, partnerId, reason, event.getUser().getIdLong(), expireTime).strikeId + " to " + mention + " for \"" + reason + "\", it will expire in `" + expire + "`").setEphemeral(true).queue();
    }

    @NotNull
    private MessageEmbed generateEmbed(@NotNull Member partner, @NotNull List<Strike> strikes) {
        final LazyEmbed embed = new LazyEmbed(eventAlerts)
                .setTitle(partner.getEffectiveName() + "'s Strikes")
                .setDescription("**Total:** " + strikes.size())
                .setThumbnail(partner.getUser().getEffectiveAvatarUrl());
        for (final Strike strike : strikes) embed.addField(strike.toField());
        return embed.build();
    }
}
