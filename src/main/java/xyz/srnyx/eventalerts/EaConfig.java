package xyz.srnyx.eventalerts;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.spongepowered.configurate.ConfigurationNode;


public class EaConfig {
    @NotNull private final EventAlerts eventAlerts;

    @Nullable public final String mongo;
    @NotNull public final GuildNode guild;

    public EaConfig(@NotNull EventAlerts eventAlerts) {
        this.eventAlerts = eventAlerts;

        final ConfigurationNode yaml = eventAlerts.settings.fileSettings.file.yaml;
        this.mongo = yaml.node("mongo").getString();
        this.guild = new GuildNode(yaml.node("guild"));
    }

    public boolean checkIsOwner(@NotNull GenericCommandInteractionEvent event) {
        final boolean isOwner = eventAlerts.isOwner(event.getUser().getIdLong());
        if (!isOwner) event.replyEmbeds(eventAlerts.embeds.noPermission()).setEphemeral(true).queue();
        return isOwner;
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
            public final long partner;
            public final long mod;
            public final long communityEvents;

            public RolesNode(@NotNull ConfigurationNode node) {
                this.partner = node.node("partner").getLong();
                this.mod = node.node("mod").getLong();
                this.communityEvents = node.node("community-events").getLong();
            }

            @Nullable
            public Role getPartner() {
                final Guild jdaGuild = getGuild();
                return jdaGuild == null ? null : jdaGuild.getRoleById(partner);
            }

            @Nullable
            public Role getMod() {
                final Guild jdaGuild = getGuild();
                return jdaGuild == null ? null : jdaGuild.getRoleById(mod);
            }

            public boolean isPartner(long user) {
                final Guild jdaGuild = getGuild();
                if (jdaGuild == null) return false;
                final Role role = getPartner();
                final Member member = jdaGuild.retrieveMemberById(user).complete();
                return role != null && member != null && member.getRoles().contains(role);
            }

            public boolean checkIsPartner(@NotNull GenericCommandInteractionEvent event) {
                final boolean isPartner = isPartner(event.getUser().getIdLong());
                if (!isPartner) event.replyEmbeds(eventAlerts.embeds.noPermission()).setEphemeral(true).queue();
                return isPartner;
            }

            public boolean isMod(long user) {
                final Guild jdaGuild = getGuild();
                if (jdaGuild == null) return false;
                final Role role = getMod();
                final Member member = jdaGuild.retrieveMemberById(user).complete();
                return role != null && member != null && member.getRoles().contains(role);
            }

            public boolean checkIsMod(@NotNull GenericCommandInteractionEvent event) {
                final boolean isMod = isMod(event.getUser().getIdLong());
                if (!isMod) event.replyEmbeds(eventAlerts.embeds.noPermission()).setEphemeral(true).queue();
                return isMod;
            }
        }

        public class ChannelsNode {
            public final long servers;
            public final long communityEvents;

            public ChannelsNode(@NotNull ConfigurationNode node) {
                this.servers = node.node("servers").getLong();
                this.communityEvents = node.node("community-events").getLong();
            }

            @Nullable
            public GuildMessageChannel getServers() {
                final Guild jdaGuild = getGuild();
                return jdaGuild == null ? null : jdaGuild.getChannelById(GuildMessageChannel.class, servers);
            }

            @Nullable
            public GuildMessageChannel getCommunityEvents() {
                final Guild jdaGuild = getGuild();
                return jdaGuild == null ? null : jdaGuild.getChannelById(GuildMessageChannel.class, communityEvents);
            }
        }
    }
}
