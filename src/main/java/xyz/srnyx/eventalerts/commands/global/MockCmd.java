package xyz.srnyx.eventalerts.commands.global;

import com.freya02.botcommands.api.annotations.CommandMarker;
import com.freya02.botcommands.api.annotations.Dependency;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandScope;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GlobalSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.eventalerts.EventAlerts;


@CommandMarker
public class MockCmd extends ApplicationCommand {
    @Dependency private EventAlerts eventAlerts;

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "mock",
            description = "Convert normal text to mock text (randomly capitalize)")
    public void mock(@NotNull GlobalSlashEvent event,
                     @AppOption(description = "The text to mock") @NotNull String text) {
        event.reply(mockify(text)).setEphemeral(true).queue();
    }

    // STATICS

    @NotNull
    public static String mockify(@NotNull String text) {
        return text.chars()
                .mapToObj(c -> (char) c)
                .map(c -> EventAlerts.RANDOM.nextBoolean() ? Character.toUpperCase(c) : Character.toLowerCase(c))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}
