package xyz.srnyx.eventalerts.mongo.objects;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import org.jetbrains.annotations.NotNull;

import xyz.srnyx.eventalerts.EventAlerts;

import java.util.Set;


public class Booster {
    @BsonProperty(value = "_id") public ObjectId id;
    public Long user;
    public Set<Long> passes;

    public void removePasses(@NotNull EventAlerts eventAlerts) {
        final Role role = eventAlerts.config.guild.roles.getRole(eventAlerts.config.guild.roles.boosterPass);
        if (role != null) {
            final Guild guild = role.getGuild();
            passes.forEach(pass -> guild.retrieveMemberById(pass)
                    .flatMap(passMember -> guild.removeRoleFromMember(passMember, role))
                    .queue());
        }
        eventAlerts.mongo.boosterCollection.deleteOne("_id", id);
    }
}
