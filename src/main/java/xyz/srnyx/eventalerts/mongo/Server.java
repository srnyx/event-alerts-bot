package xyz.srnyx.eventalerts.mongo;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.RestAction;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.ServerTag;
import xyz.srnyx.eventalerts.utility.MongoUtility;

import xyz.srnyx.lazylibrary.LazyEmbed;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class Server {
    @BsonProperty(value = "_id") public ObjectId id;
    public Long user;
    @Nullable public Long message;
    public String name;
    @Nullable public String description;
    public String invite;
    @Nullable public Set<String> tags;
    @Nullable public List<Long> representatives;
    @Nullable public Integer color;
    @Nullable public String thumbnail;

    @Nullable
    public RestAction<Message> getMessage(@NotNull EventAlerts eventAlerts) {
        return MongoUtility.getMessage(eventAlerts, eventAlerts.config.guild.channels.servers, message);
    }

    @NotNull
    public LazyEmbed getEmbed() {
        final String inviteUrl = "https://discord.gg/" + invite;
        final LazyEmbed embed = new LazyEmbed()
                .setTitle(name, inviteUrl)
                .addField("Invite", inviteUrl, true);
        if (description != null) embed.setDescription(description);
        if (tags != null) tags.stream()
                .filter(Objects::nonNull)
                .map(string -> {
                    try {
                        return "`" + ServerTag.valueOf(string).name() + "`";
                    } catch (final IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + " " + b)
                .ifPresent(tagString -> embed.addField("Tags", tagString, true));
        final List<Long> allRepresentatives = new ArrayList<>();
        allRepresentatives.add(user);
        if (representatives != null) allRepresentatives.addAll(representatives);
        allRepresentatives.stream()
                .filter(Objects::nonNull)
                .map(representative -> "<@" + representative + ">")
                .reduce((a, b) -> a + ", " + b)
                .ifPresent(representativeString -> embed.addField("Representatives", representativeString, true));
        if (color != null) embed.setColor(color);
        if (thumbnail != null) embed.setThumbnail(thumbnail);
        return embed;
    }
}
