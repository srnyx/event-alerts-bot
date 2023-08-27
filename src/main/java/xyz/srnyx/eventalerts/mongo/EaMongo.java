package xyz.srnyx.eventalerts.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.RestAction;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EaConfig;
import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.objects.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;


public class EaMongo {
    @NotNull public final EaCollection<Event> eventCollection;
    @NotNull public final EaCollection<Partner> partnerCollection;
    @NotNull public final EaCollection<Server> serverCollection;
    @NotNull public final EaCollection<Strike> strikeCollection;

    public EaMongo(@NotNull EaConfig.MongoNode node) {
        if (node.connection == null || node.database == null) throw new IllegalStateException("Mongo configuration is invalid!");
        final MongoDatabase database = MongoClients.create(node.connection).getDatabase(node.database).withCodecRegistry(CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())));
        eventCollection = new EaCollection<>(database, "events", Event.class);
        partnerCollection = new EaCollection<>(database, "partners", Partner.class);
        serverCollection = new EaCollection<>(database, "servers", Server.class);
        strikeCollection = new EaCollection<>(database, "strikes", Strike.class);
    }

    @NotNull
    public Stream<Event> getEndedEvents() {
        return eventCollection.findMany(Filters.exists("time")).stream().filter(Event::ended);
    }

    @NotNull
    public List<Strike> getStrikes(long user) {
        final List<Strike> list = strikeCollection.findMany(Filters.eq("user", user));
        list.sort(Comparator.comparingInt(strike -> strike.strikeId));
        return list;
    }

    @Nullable
    public static RestAction<Message> getMessage(@NotNull EventAlerts eventAlerts, long channel, @Nullable Long message) {
        final Guild guild = eventAlerts.config.guild.getGuild();
        if (guild == null) return null;
        final GuildMessageChannel messageChannel = guild.getChannelById(GuildMessageChannel.class, channel);
        return messageChannel == null || message == null ? null : messageChannel.retrieveMessageById(message);
    }
}
