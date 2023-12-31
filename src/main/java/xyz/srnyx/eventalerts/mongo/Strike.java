package xyz.srnyx.eventalerts.mongo;

import com.mongodb.client.model.Filters;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;

import org.bson.BsonValue;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.utility.MongoUtility;

import xyz.srnyx.lazylibrary.LazyCollection;
import xyz.srnyx.lazylibrary.LazyEmbed;


public class Strike {
    @NotNull private static final ErrorHandler IGNORE_UNKNOWN_MESSAGE = new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE);

    @BsonProperty(value = "_id") public ObjectId id;
    @BsonProperty(value = "strike_id") public Integer strikeId;
    public Long user;
    public Long striker;
    public String reason;
    public Long timestamp;
    @Nullable public Long expire;
    @Nullable public Long message;

    public Strike() {}

    public Strike(@NotNull EventAlerts eventAlerts, @NotNull Long user, @NotNull String reason, @NotNull Long striker, @Nullable Long expire) {
        final LazyCollection<Strike> collection = eventAlerts.getMongoCollection(Strike.class);
        this.strikeId = collection.findMany(Filters.exists("strike_id")).stream()
                .mapToInt(strike -> strike.strikeId)
                .max()
                .orElse(0) + 1;
        this.user = user;
        this.reason = reason;
        this.striker = striker;
        this.timestamp = System.currentTimeMillis();
        this.expire = expire;
        final BsonValue newId = collection.collection.insertOne(this).getInsertedId();
        if (newId != null) this.id = newId.asObjectId().getValue();
    }

    @Nullable
    public RestAction<Message> getMessage(@NotNull EventAlerts eventAlerts) {
        return MongoUtility.getMessage(eventAlerts, eventAlerts.config.guild.channels.strikes, message);
    }

    public boolean hasExpired() {
        return expire != null && System.currentTimeMillis() >= expire;
    }

    public void delete(@NotNull EventAlerts eventAlerts) {
        final RestAction<Message> messageAction = getMessage(eventAlerts);
        if (messageAction != null) messageAction.flatMap(Message::delete).queue(s -> {}, IGNORE_UNKNOWN_MESSAGE);
        eventAlerts.getMongoCollection(Strike.class).collection.deleteOne(Filters.eq("_id", id));
    }

    @NotNull
    public MessageEmbed.Field toField() {
        final StringBuilder builder = new StringBuilder(reason + "\n");
        if (striker != null) builder.append("By <@").append(striker).append(">\n");
        if (expire != null) builder.append("Expires <t:").append(expire / 1000).append(":R>");
        return new MessageEmbed.Field(strikeId.toString(), builder.toString(), true);
    }

    @NotNull
    public LazyEmbed toEmbed() {
        final LazyEmbed embed = new LazyEmbed();
        embed.addField("ID", strikeId.toString(), true);
        embed.addField("User", "<@" + user + ">", true);
        if (striker != null) embed.addField("Striker", "<@" + striker + ">", true);
        if (expire != null) embed.addField("Expires", "<t:" + (expire / 1000) + ":R>", true);
        embed.addField("Reason", reason, true);
        return embed;
    }
}
