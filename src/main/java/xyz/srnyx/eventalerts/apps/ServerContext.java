package xyz.srnyx.eventalerts.apps;

import com.freya02.botcommands.api.annotations.CommandMarker;
import com.freya02.botcommands.api.annotations.Dependency;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandScope;
import com.freya02.botcommands.api.application.context.annotations.JDAUserCommand;
import com.freya02.botcommands.api.application.context.user.GlobalUserEvent;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.objects.Server;

import xyz.srnyx.lazylibrary.LazyEmoji;


@CommandMarker
public class ServerContext extends ApplicationCommand {
    @Dependency private EventAlerts eventAlerts;

    @JDAUserCommand(
            scope = CommandScope.GLOBAL,
            name = "Server")
    public void server(@NotNull GlobalUserEvent event) {
        final long target = event.getTarget().getIdLong();

        // Check if partner
        if (!eventAlerts.config.guild.roles.hasRole(target, eventAlerts.config.guild.roles.partner)) {
            event.reply(LazyEmoji.NO + " <@" + target + "> is not a partner!").setEphemeral(true).queue();
            return;
        }

        // Get server
        final Server server = eventAlerts.mongo.serverCollection.findOne("user", target);
        if (server == null) {
            event.reply(LazyEmoji.NO + " <@" + target + "> has not set a server!").setEphemeral(true).queue();
            return;
        }

        // Send server
        event.replyEmbeds(server.getEmbed().build(eventAlerts)).setEphemeral(true).queue();
    }
}
