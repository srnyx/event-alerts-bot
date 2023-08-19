package xyz.srnyx.eventalerts.mongo.objects;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.EaMongo;

import java.util.HashSet;


public class Strike {
    @BsonProperty public ObjectId id;
    @BsonProperty(value = "strike_id") public Integer strikeId;
    @BsonProperty public Long user;
    @BsonProperty public Long striker;
    @BsonProperty public String reason;
    @BsonProperty public Long timestamp;
    @BsonProperty @Nullable public Long expire;

    public Strike() {}

    public Strike(@NotNull EaMongo mongo, @NotNull Long user, @NotNull String reason, @NotNull Long striker, @Nullable Long expire) {
        this.strikeId = mongo.strikesCollection.collection.find().into(new HashSet<>()).size() + 1;
        this.user = user;
        this.reason = reason;
        this.striker = striker;
        this.timestamp = System.currentTimeMillis();
        this.expire = expire;
        mongo.strikesCollection.collection.insertOne(this);
    }

    @Nullable
    public CacheRestAction<User> getUser(@NotNull EventAlerts eventAlerts) {
        return EaMongo.getUser(eventAlerts, user);
    }

    @Nullable
    public CacheRestAction<Member> getMember(@NotNull EventAlerts eventAlerts) {
        if (user == null) return null;
        final Guild guild = eventAlerts.config.guild.getGuild();
        return guild == null ? null : guild.retrieveMemberById(user);
    }

    @Nullable
    public CacheRestAction<User> getStriker(@NotNull EventAlerts eventAlerts) {
        return EaMongo.getUser(eventAlerts, striker);
    }

    @Nullable
    public CacheRestAction<Member> getStrikerMember(@NotNull EventAlerts eventAlerts) {
        if (striker == null) return null;
        final Guild guild = eventAlerts.config.guild.getGuild();
        return guild == null ? null : guild.retrieveMemberById(striker);
    }

    public boolean expired() {
        return expire != null && System.currentTimeMillis() > expire;
    }

    @NotNull
    public MessageEmbed.Field toField() {
        final StringBuilder builder = new StringBuilder();
        if (reason != null) builder.append(reason).append("\n");
        if (striker != null) builder.append("By <@").append(striker).append(">\n");
        if (expire != null) builder.append("Expires <t:").append(expire / 1000).append(":R>");
        return new MessageEmbed.Field(strikeId.toString(), builder.toString(), false);
    }
}
