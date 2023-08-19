package xyz.srnyx.eventalerts.apps;

import com.freya02.botcommands.api.annotations.CommandMarker;
import com.freya02.botcommands.api.annotations.Dependency;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandScope;
import com.freya02.botcommands.api.application.context.annotations.JDAMessageCommand;
import com.freya02.botcommands.api.application.context.message.GlobalMessageEvent;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.commands.global.MockCmd;

import xyz.srnyx.lazylibrary.utility.LazyUtilities;


@CommandMarker
public class MockContext extends ApplicationCommand {
    @Dependency private EventAlerts eventAlerts;

    @JDAMessageCommand(
            scope = CommandScope.GLOBAL,
            name = "Mock")
    public void mock(@NotNull GlobalMessageEvent event) {
        if (Boolean.FALSE.equals(LazyUtilities.userHasChannelPermission(event, Permission.MESSAGE_SEND))) {
            event.replyEmbeds(eventAlerts.embeds.noPermission()).setEphemeral(true).queue();
            return;
        }

        // Reply to target message with mockified message
        final Message message = event.getTarget();
        message.reply(MockCmd.mockify(message.getContentRaw())).mentionRepliedUser(false)
                .flatMap(msg -> event.deferReply(true))
                .flatMap(InteractionHook::deleteOriginal)
                .queue();
    }
}
