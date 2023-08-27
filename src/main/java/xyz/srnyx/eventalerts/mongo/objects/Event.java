package xyz.srnyx.eventalerts.mongo.objects;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.RestAction;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.EaMongo;

import xyz.srnyx.lazylibrary.LazyEmoji;

import java.util.HashSet;
import java.util.Set;


public class Event {
    @BsonProperty(value = "_id") public ObjectId id;
    public Long channel;
    public Long thread;
    public String title;
    public Long host;
    public Long time;
    public Set<Long> subscribers;

    public Event() {}

    public Event(long channel, long thread, @NotNull String title, long host, long time) {
        this.channel = channel;
        this.thread = thread;
        this.title = title;
        this.host = host;
        this.time = time;
        subscribers = new HashSet<>();
    }

    @Nullable
    public RestAction<ThreadChannel> getThread(@NotNull EventAlerts eventAlerts) {
        final RestAction<Message> messageAction = getMessage(eventAlerts);
        return messageAction == null ? null : messageAction.map(Message::getStartedThread);
    }

    @Nullable
    public RestAction<Message> getMessage(@NotNull EventAlerts eventAlerts) {
        return EaMongo.getMessage(eventAlerts, channel, thread);
    }

    @Nullable
    public String subscribersString() {
        return subscribers.stream()
                .map(userId -> "<@" + userId + ">")
                .reduce((a, b) -> a + " " + b)
                .orElse(null);
    }

    public boolean ended() {
        return System.currentTimeMillis() > time - 300000;
    }

    public void notifySubscribers(@NotNull EventAlerts eventAlerts) {
        delete(eventAlerts);

        // Get thread
        final RestAction<ThreadChannel> threadAction = getThread(eventAlerts);
        if (threadAction == null) return;

        // Send notification
        final String beforeTitle = LazyEmoji.WARNING + " **";
        final StringBuilder afterTitle = new StringBuilder(" is starting <t:" + (time / 1000) + ":R>!**");
        final String subscribersString = subscribersString();
        if (subscribersString != null) afterTitle.append("\n").append(subscribersString);
        threadAction
                .flatMap(threadChannel -> threadChannel.sendMessage(beforeTitle + "Event" + afterTitle))
                .flatMap(msg -> msg.editMessage(beforeTitle + title + afterTitle))
                .queue();
    }

    public void delete(@NotNull EventAlerts eventAlerts) {
        eventAlerts.mongo.eventCollection.deleteOne("_id", id);
        final RestAction<Message> messageAction = getMessage(eventAlerts);
        if (messageAction != null) messageAction
                .flatMap(msg -> msg.editMessageComponents(msg.getActionRows().stream()
                        .map(ActionRow::asDisabled)
                        .toList()))
                .queue();
    }
}
