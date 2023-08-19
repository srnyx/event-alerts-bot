package xyz.srnyx.eventalerts.mongo.objects;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.ServerTag;
import xyz.srnyx.eventalerts.mongo.EaMongo;

import xyz.srnyx.lazylibrary.LazyEmbed;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class Server {
    @BsonProperty public ObjectId id;
    @BsonProperty public Long user;
    @BsonProperty(value = "message") @Nullable public Long message;
    @BsonProperty public String name;
    @BsonProperty public String invite;
    @BsonProperty @Nullable public List<Long> representatives;
    @BsonProperty @Nullable public Integer color;
    @BsonProperty @Nullable public Set<String> tags;
    @BsonProperty @Nullable public String description;

    @Nullable
    public CacheRestAction<User> getUser(@NotNull EventAlerts eventAlerts) {
        return EaMongo.getUser(eventAlerts, user);
    }

    @Nullable
    public RestAction<Message> getMessage(@NotNull EventAlerts eventAlerts) {
        if (message == null) return null;
        final Guild guild = eventAlerts.config.guild.getGuild();
        if (guild == null) return null;
        final GuildMessageChannel serverChannel = eventAlerts.config.guild.channels.getServers();
        return serverChannel == null ? null : serverChannel.retrieveMessageById(message);
    }

    @NotNull
    public LazyEmbed getEmbed(@NotNull EventAlerts eventAlerts) {
        final String inviteUrl = "https://discord.gg/" + invite;
        final LazyEmbed embed = new LazyEmbed(eventAlerts)
                .setTitle(name, inviteUrl)
                .addField("Invite", inviteUrl, true);
        if (description != null) embed.setDescription(description);
        if (color != null) embed.setColor(color);
        if (tags != null) {
            tags.stream()
                    .map(string -> {
                        if (string == null) return null;
                        try {
                            return ServerTag.valueOf(string).getDisplayName();
                        } catch (final IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .reduce((a, b) -> a + ", " + b)
                    .ifPresent(tagString -> embed.addField("Tags", tagString, true));
        }
        final List<Long> allRepresentatives = new ArrayList<>();
        allRepresentatives.add(user);
        if (representatives != null) allRepresentatives.addAll(representatives);
        allRepresentatives.stream()
                .filter(Objects::nonNull)
                .map(representative -> "<@" + representative + ">")
                .reduce((a, b) -> a + ", " + b)
                .ifPresent(representativeString -> embed.addField("Representatives", representativeString, true));
        return embed;
    }
}
