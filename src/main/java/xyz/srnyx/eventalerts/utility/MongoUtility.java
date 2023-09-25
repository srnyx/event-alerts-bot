package xyz.srnyx.eventalerts.utility;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.RestAction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;


public class MongoUtility {
    @Nullable
    public static RestAction<Message> getMessage(@NotNull EventAlerts eventAlerts, long channel, @Nullable Long message) {
        final Guild guild = eventAlerts.config.guild.getGuild();
        if (guild == null) return null;
        final GuildMessageChannel messageChannel = guild.getChannelById(GuildMessageChannel.class, channel);
        return messageChannel == null || message == null ? null : messageChannel.retrieveMessageById(message);
    }
}
