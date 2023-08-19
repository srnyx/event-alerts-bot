package xyz.srnyx.eventalerts.mongo.objects;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.EaMongo;


public class Partner {
    @BsonProperty public ObjectId id;
    @BsonProperty public Long user;
    @BsonProperty(value = "last_renewal") @Nullable public Long lastRenewal;

    public Partner() {}

    public Partner(@NotNull EaMongo mongo, @NotNull Long user) {
        this.user = user;
        mongo.partnersCollection.collection.insertOne(this);
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
}
