package xyz.srnyx.eventalerts.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.objects.Partner;
import xyz.srnyx.eventalerts.mongo.objects.Server;
import xyz.srnyx.eventalerts.mongo.objects.Strike;

import java.util.Comparator;
import java.util.List;


public class EaMongo {
    @NotNull public final MongoClient client;
    @NotNull public final EaCollection<Partner> partnersCollection;
    @NotNull public final EaCollection<Server> serversCollection;
    @NotNull public final EaCollection<Strike> strikesCollection;

    public EaMongo(@NotNull EventAlerts eventAlerts) {
        if (eventAlerts.config.mongo == null) throw new IllegalStateException("Mongo configuration is null!");
        client = MongoClients.create(eventAlerts.config.mongo);
        final MongoDatabase database = client.getDatabase("eventalerts").withCodecRegistry(CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())));
        partnersCollection = new EaCollection<>(database, "partners", Partner.class);
        serversCollection = new EaCollection<>(database, "servers", Server.class);
        strikesCollection = new EaCollection<>(database, "strikes", Strike.class);
    }

    @Nullable
    public Strike getStrike(int strikeId) {
        return strikesCollection.findOne(Filters.eq("strike_id", strikeId));
    }

    @NotNull
    public List<Strike> getStrikes(long user) {
        final List<Strike> list = strikesCollection.findMany(Filters.eq("user", user));
        list.sort(Comparator.comparingInt(strike -> strike.strikeId));
        return list;
    }

    @Nullable
    public static CacheRestAction<User> getUser(@NotNull EventAlerts eventAlerts, @Nullable Long user) {
        return user == null ? null : eventAlerts.jda.retrieveUserById(user);
    }
}
