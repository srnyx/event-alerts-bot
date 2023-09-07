package xyz.srnyx.eventalerts;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;


public class EaConfig {
    @NotNull private final EventAlerts eventAlerts;

    @NotNull public final MongoNode mongo;
    @NotNull public final GuildNode guild;

    public EaConfig(@NotNull EventAlerts eventAlerts) {
        this.eventAlerts = eventAlerts;
        final ConfigurationNode yaml = eventAlerts.settings.fileSettings.file.yaml;
        this.mongo = new MongoNode(yaml.node("mongo"));
        this.guild = new GuildNode(yaml.node("guild"));
    }

    public boolean checkIfNotOwner(@NotNull GenericCommandInteractionEvent event) {
        final boolean isntOwner = !eventAlerts.isOwner(event.getUser().getIdLong());
        if (isntOwner) event.replyEmbeds(eventAlerts.embeds.noPermission()).setEphemeral(true).queue();
        return isntOwner;
    }

    public static class MongoNode {
        @Nullable public final String connection;
        @Nullable public final String database;

        public MongoNode(@NotNull ConfigurationNode node) {
            this.connection = node.node("connection").getString();
            this.database = node.node("database").getString();
        }
    }

    public class GuildNode {
        public final long id;
        @NotNull public final RolesNode roles;
        @NotNull public final ChannelsNode channels;

        public GuildNode(@NotNull ConfigurationNode node) {
            this.id = node.node("id").getLong();
            this.roles = new RolesNode(node.node("roles"));
            this.channels = new ChannelsNode(node.node("channels"));
        }

        @Nullable
        public Guild getGuild() {
        return eventAlerts.jda.getGuildById(id);
    }

        public class RolesNode {
            public final long mod;
            public final long partner;
            public final long communityEvents;
            public final long boosterPass;
            public final long noHosting;
            @NotNull public final EventPingsNode eventPings;

            public RolesNode(@NotNull ConfigurationNode node) {
                this.mod = node.node("mod").getLong();
                this.partner = node.node("partner").getLong();
                this.communityEvents = node.node("community-events").getLong();
                this.boosterPass = node.node("booster-pass").getLong();
                this.noHosting = node.node("no-hosting").getLong();
                this.eventPings = new EventPingsNode(node.node("event-pings"));
            }

            @Nullable
            public Role getRole(long id) {
                final Guild jdaGuild = getGuild();
                return jdaGuild == null ? null : jdaGuild.getRoleById(id);
            }

            public boolean hasRole(long user, long role) {
                final Role jdaRole = getRole(role);
                if (jdaRole == null) return false;
                final Member member = jdaRole.getGuild().retrieveMemberById(user).complete();
                return member != null && member.getRoles().contains(jdaRole);
            }

            public boolean checkDontHaveRole(@NotNull GenericCommandInteractionEvent event, long role) {
                final boolean doesntHaverole = !hasRole(event.getUser().getIdLong(), role);
                if (doesntHaverole) event.replyEmbeds(eventAlerts.embeds.noPermission()).setEphemeral(true).queue();
                return doesntHaverole;
            }

            public static class EventPingsNode {
                public final long community;
                public final long eventAlerts;
                public final long money;
                public final long fun;
                public final long housing;
                public final long civilization;
                @NotNull public final List<SelectOption> options;

                public EventPingsNode(@NotNull ConfigurationNode node) {
                    community = node.node("community").getLong();
                    eventAlerts = node.node("event-alerts").getLong();
                    money = node.node("money").getLong();
                    fun = node.node("fun").getLong();
                    housing = node.node("housing").getLong();
                    civilization = node.node("civilization").getLong();
                    options = List.of(
                            SelectOption.of("Money", String.valueOf(money)).withEmoji(Emoji.fromUnicode("\uD83D\uDCB5")),
                            SelectOption.of("Fun", String.valueOf(fun)).withEmoji(Emoji.fromUnicode("\uD83C\uDF89")),
                            SelectOption.of("Housing", String.valueOf(housing)).withEmoji(Emoji.fromUnicode("\uD83C\uDFE0")),
                            SelectOption.of("Civilization", String.valueOf(civilization)).withEmoji(Emoji.fromUnicode("\uD83C\uDF3E")));
                }
            }
        }

        public class ChannelsNode {
            public final long strikes;
            public final long servers;
            public final long partnerEvents;
            public final long communityEvents;

            public ChannelsNode(@NotNull ConfigurationNode node) {
                this.strikes = node.node("strikes").getLong();
                this.servers = node.node("servers").getLong();
                this.partnerEvents = node.node("partner-events").getLong();
                this.communityEvents = node.node("community-events").getLong();
            }

            @Nullable
            public GuildMessageChannel getStrikes() {
                return getGuildMessageChannel(strikes);
            }

            @Nullable
            public GuildMessageChannel getServers() {
                return getGuildMessageChannel(servers);
            }

            @Nullable
            public GuildMessageChannel getPartnerEvents() {
                return getGuildMessageChannel(partnerEvents);
            }

            @Nullable
            public GuildMessageChannel getCommunityEvents() {
                return getGuildMessageChannel(communityEvents);
            }

            @Nullable
            private GuildMessageChannel getGuildMessageChannel(long id) {
                final Guild jdaGuild = getGuild();
                return jdaGuild == null ? null : jdaGuild.getChannelById(GuildMessageChannel.class, id);
            }
        }
    }
}
