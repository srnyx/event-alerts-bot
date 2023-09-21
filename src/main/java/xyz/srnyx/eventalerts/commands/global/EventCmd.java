package xyz.srnyx.eventalerts.commands.global;

import com.freya02.botcommands.api.annotations.CommandMarker;
import com.freya02.botcommands.api.annotations.Dependency;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandScope;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GlobalSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.botcommands.api.components.Components;
import com.freya02.botcommands.api.components.annotations.JDAButtonListener;
import com.freya02.botcommands.api.components.event.ButtonEvent;
import com.freya02.botcommands.api.modals.Modals;
import com.freya02.botcommands.api.modals.annotations.ModalData;
import com.freya02.botcommands.api.modals.annotations.ModalHandler;
import com.freya02.botcommands.api.modals.annotations.ModalInput;
import com.freya02.botcommands.api.utils.ButtonContent;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.messages.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EaConfig;
import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.mongo.objects.Event;

import xyz.srnyx.javautilities.StringUtility;
import xyz.srnyx.javautilities.manipulation.DurationParser;

import xyz.srnyx.lazylibrary.LazyEmbed;
import xyz.srnyx.lazylibrary.LazyEmoji;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@CommandMarker
public class EventCmd extends ApplicationCommand {
    @NotNull private static final String MODAL_EVENT_DESCRIPTION = "EventCmd.modal.event.description";
    @NotNull private static final String BUTTON_SUBSCRIBE = "EventCmd.button.subscribe";
    @NotNull private static final String BUTTON_UNSUBSCRIBE = "EventCmd.button.unsubscribe";

    @Dependency private EventAlerts eventAlerts;

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "event",
            description = "Post an alert for your event")
    public void commandEvent(@NotNull GlobalSlashEvent event,
                             @AppOption(description = "The title of the event") @NotNull String title,
                             @AppOption(description = "The start time of the event") @NotNull String time,
                             @AppOption(description = "The server IP for the event") @NotNull String ip,
                             @AppOption(description = "The version of the server") @NotNull String version,
                             @AppOption(description = "The prize of the event") @Nullable String prize) {
        // Check if user has No Hosting
        final EaConfig.GuildNode.RolesNode roles = eventAlerts.config.guild.roles;
        final long userId = event.getUser().getIdLong();
        if (roles.hasRole(userId, roles.noHosting)) {
            event.replyEmbeds(eventAlerts.embeds.noPermission()).setEphemeral(true).queue();
            return;
        }

        // Check if user has Partner or Community Events
        final EventData data = new EventData(eventAlerts, title, ip, version, userId, prize);
        if (!data.isPartner && !roles.hasRole(userId, roles.communityEvents)) {
            event.replyEmbeds(eventAlerts.embeds.noPermission()).setEphemeral(true).queue();
            return;
        }
        final long now = System.currentTimeMillis();

        // Check cooldown
        final Long cooldown = eventAlerts.userEventCooldowns.get(userId);
        if (cooldown != null) {
            if (cooldown > now) {
                event.reply(LazyEmoji.NO + " **You're on cooldown!** You can post a new event **<t:" + (cooldown / 1000) + ":R>**!").setEphemeral(true).queue();
                return;
            }
            eventAlerts.userEventCooldowns.remove(userId);
        }

        // Set time
        long timeLong;
        try {
            timeLong = Long.parseLong(time) * 1000L;
            if (timeLong < now) {
                event.reply(LazyEmoji.NO + " That time has already passed!").setEphemeral(true).queue();
                return;
            }
        } catch (final NumberFormatException e) {
            final Duration duration = DurationParser.parse(time);
            if (duration == null) {
                event.reply(LazyEmoji.NO + " **Invalid time format!** Please use one of the formats below\n### Epoch/Unix timestamp\n- [Epoch Converter](<https://epochconverter.com>)\n- [HammerTime](<https://hammertime.cyou>)\n### Duration/relative\n- `s` = seconds\n- `m` = minutes\n- `h` = hours\n- `d` = days\n- `w` = weeks\n- `mo` = months\n- `y` = years\n***Example:** `2h30m10s`*").setEphemeral(true).queue();
                return;
            }
            data.time = now + duration.toMillis();
        }

        // Send message
        data.roles.add(data.isPartner ? roles.eventPings.eventAlerts : roles.eventPings.community);
        event.reply(data.getMessage(new MessageCreateBuilder(), true)).setEphemeral(true).queue();
    }

    @ModalHandler(name = MODAL_EVENT_DESCRIPTION)
    public void modalEventDescription(@NotNull ModalInteractionEvent event,
                                      @ModalData @NotNull List<Long> roles,
                                      @ModalData @NotNull String title,
                                      @ModalData long time,
                                      @ModalData @NotNull String ip,
                                      @ModalData @NotNull String version,
                                      @ModalData @NotNull String prize,
                                      @ModalInput(name = "description") @Nullable String description) {
        event.editMessage(new EventData(eventAlerts, roles, title, time, ip, version, event.getUser().getIdLong(), prize.isEmpty() ? null : prize, description).getMessage(new MessageEditBuilder(), true)).queue();
    }

    @JDAButtonListener(name = BUTTON_SUBSCRIBE)
    public void buttonSubscribe(@NotNull ButtonEvent event) {
        final Long userId = getSubscribeUserId(event, true, "You are already following this event!");
        if (userId == null) return;
        eventAlerts.mongo.eventCollection.collection.updateOne(Filters.eq("thread", event.getMessageIdLong()), Updates.addToSet("subscribers", userId));
        event.reply(LazyEmoji.YES + " **You are now following this event!** You will be notified ~5 minutes before it starts").setEphemeral(true)
                .flatMap(msg -> {
                    final Message message = event.getMessage();
                    final Button button = event.getComponent();
                    final String label = button.getLabel();
                    return message.editMessageComponents(ActionRow.of(
                            button.withLabel("Follow (" + (Integer.parseInt(label.substring(8, label.length() - 1)) + 1) + ")"),
                            message.getButtons().get(1)));
                })
                .queue();
    }

    @JDAButtonListener(name = BUTTON_UNSUBSCRIBE)
    public void buttonUnsubscribe(@NotNull ButtonEvent event) {
        final Long userId = getSubscribeUserId(event, false, "You are not following this event!");
        if (userId == null) return;
        eventAlerts.mongo.eventCollection.collection.updateOne(Filters.eq("thread", event.getMessageIdLong()), Updates.pull("subscribers", userId));
        event.reply(LazyEmoji.YES + " **You are no longer following this event!** You will no longer be notified").setEphemeral(true)
                // subtract 1 from the subscriber count in button name
                .flatMap(msg -> {
                    final Message message = event.getMessage();
                    final Button button = message.getButtons().get(0);
                    final String label = button.getLabel();
                    return message.editMessageComponents(ActionRow.of(
                            button.withLabel("Follow (" + (Integer.parseInt(label.substring(8, label.length() - 1)) - 1) + ")"),
                            event.getComponent()));
                })
                .queue();
    }

    @Nullable
    private Long getSubscribeUserId(@NotNull ButtonEvent event, boolean containsCheck, @NotNull String errorMessage) {
        // Get event
        final Event eventObj = eventAlerts.mongo.eventCollection.findOne("thread", event.getMessageIdLong());
        if (eventObj == null) {
            event.reply(LazyEmoji.NO + " Event not found!").setEphemeral(true).queue();
            return null;
        }

        // Get user ID
        final long userId = event.getUser().getIdLong();
        if (containsCheck == eventObj.subscribers.contains(userId)) {
            event.reply(LazyEmoji.NO + " " + errorMessage).setEphemeral(true).queue();
            return null;
        }
        return userId;
    }

    private static final class EventData {
        @NotNull private final EventAlerts eventAlerts;
        private final boolean isPartner;
        @NotNull private final List<Long> roles;
        @NotNull private final String title;
        @Nullable private Long time;
        @NotNull private final String ip;
        @NotNull private final String version;
        private final long host;
        @Nullable private final String prize;
        @Nullable private String description;

        private EventData(@NotNull EventAlerts eventAlerts, @NotNull List<Long> roles, @NotNull String title, @Nullable Long time, @NotNull String ip, @NotNull String version, long host, @Nullable String prize, @Nullable String description) {
            this.eventAlerts = eventAlerts;
            this.isPartner = eventAlerts.config.guild.roles.hasRole(host, eventAlerts.config.guild.roles.partner);
            this.roles = roles;
            this.title = title;
            this.time = time;
            this.ip = ip;
            this.version = version;
            this.host = host;
            this.prize = prize;
            this.description = description;
        }

        private EventData(@NotNull EventAlerts eventAlerts, @NotNull String title, @NotNull String ip, @NotNull String version, long host, @Nullable String prize) {
            this(eventAlerts, new ArrayList<>(), title, null, ip, version, host, prize, null);
        }

        @NotNull
        private String getRolesString() {
            return roles.stream()
                    .map(role -> "<@&" + role + ">")
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
        }

        @NotNull
        private String getTimeString() {
            if (time == null) return "N/A";
            final String string = "<t:" + (time / 1000L);
            return string + "> (" + string + ":R>)";
        }

        @NotNull
        private <T, R extends AbstractMessageBuilder<T, R>> T getMessage(@NotNull AbstractMessageBuilder<T, R> builder, boolean includeActionRows) {
            if (includeActionRows) builder.setComponents(getActionRows());
            return builder.setContent(getContent(true)).build();
        }

        @NotNull
        private String getContent(boolean includeDescription) {
            final StringBuilder builder = new StringBuilder("__**" + title.toUpperCase() + "**__");
            final String rolesString = getRolesString();
            if (!rolesString.isEmpty()) builder.append(" | ").append(rolesString);
            if (includeDescription && description != null) builder.append("\n").append(description);
            builder.append("\n\n⏰ **Time:** ").append(getTimeString()).append("\n");
            if (prize != null) builder.append("🎁 **Prize:** ").append(prize).append("\n");
            builder.append("\n\uD83D\uDCCC **IP:** `").append(ip).append("`\n");
            builder.append("\uD83D\uDD04 **Version:** ").append(version).append("\n");
            builder.append("\n\uD83E\uDD73 **Host:** <@").append(host).append(">");
            if (isPartner) builder.append("\n*Use </server get:1142607814905315469> for their server*");

            // Check length
            final int length = builder.length();
            if (!includeDescription || description == null || length <= 2000) return builder.toString();
            // Remove end of description so that total length is 2000
            description = description.substring(0, description.length() - (length - 2000));
            return getContent(true);
        }

        @NotNull
        private <T, R extends AbstractMessageBuilder<T, R>> T getEmbed(@NotNull AbstractMessageBuilder<T, R> builder) {
            // Get rolesString
            final String rolesString = getRolesString();
            builder.setContent(rolesString);

            // Create embed
            final LazyEmbed embed = new LazyEmbed().setTitle(title);
            if (prize != null) embed.addField("\uD83C\uDF81 Prize", prize, false);
            embed
                    .addField("\uD83D\uDCCC IP", "`" + ip + "`", false)
                    .addField("\uD83D\uDD04 Version", version, false)
                    .addField("⏰ Time", getTimeString(), false);
            if (!rolesString.isEmpty()) embed.addField("\uD83D\uDD14 Pings", rolesString, false);
            embed.addField("\uD83E\uDD73 Host", "<@" + host + ">" + (isPartner ? " *Use </server get:1142607814905315469> for their server*" : ""), false);
            if (description != null) embed.setDescription(description);

            // Return message
            return builder.setEmbeds(embed.build(eventAlerts)).build();
        }

        @NotNull
        private List<ActionRow> getActionRows() {
            final EaConfig.GuildNode guild = eventAlerts.config.guild;
            final EaConfig.GuildNode.RolesNode.EventPingsNode eventPings = guild.roles.eventPings;

            // Get first ActionRow
            final List<Button> buttons = new ArrayList<>();
            buttons.add(Components.secondaryButton(buttonEvent -> buttonEvent.replyModal(Modals.create("Event Description", MODAL_EVENT_DESCRIPTION, roles, title, time, ip, version, prize == null ? "" : prize)
                    .addActionRow(Modals.createTextInput("description", "Event description", TextInputStyle.PARAGRAPH)
                            .setPlaceholder("Enter a description for your event")
                            .setValue(description)
                            .setMaxLength(1999 - getContent(false).length())
                            .setRequired(false).build()).build()).queue()).build(ButtonContent.withEmoji("Edit description", Emoji.fromUnicode("✏️"))));
            if (isPartner) buttons.add(Components.secondaryButton(buttonEvent -> {
                final List<Long> oldRoles = new ArrayList<>(roles);
                buttonEvent.editComponents(
                        ActionRow.of(Components.stringSelectionMenu(menuEvent -> {
                            roles.clear();
                            final List<Long> values = menuEvent.getValues().stream()
                                    .map(Long::parseLong)
                                    .toList();
                            if (!values.contains(eventPings.housing) && !values.contains(eventPings.civilization)) roles.add(eventPings.eventAlerts);
                            roles.addAll(menuEvent.getValues().stream()
                                    .map(Long::parseLong)
                                    .toList());
                            menuEvent.editMessage(getMessage(new MessageEditBuilder(), false)).queue();
                        }).addOptions(eventPings.options).setMaxValues(2).build()),
                        ActionRow.of(
                                Components.successButton(doneEvent -> doneEvent.editMessage(getMessage(new MessageEditBuilder(), true)).queue()).build(LazyEmoji.YES_CLEAR.getButtonContent("Done")),
                                Components.dangerButton(cancelEvent -> {
                                    roles.clear();
                                    roles.addAll(oldRoles);
                                    cancelEvent.editMessage(getMessage(new MessageEditBuilder(), true)).queue();
                                }).build(LazyEmoji.NO_CLEAR_DARK.getButtonContent("Cancel")))).queue();
            }).build(ButtonContent.withEmoji("Edit roles", Emoji.fromUnicode("👥"))));

            // Get ActionRows
            final List<ActionRow> actionRows = new ArrayList<>();
            actionRows.add(ActionRow.of(buttons));
            actionRows.add(ActionRow.of(
                    Components.successButton(buttonEvent -> {
                        // Get channel
                        final GuildMessageChannel channel = isPartner ? guild.channels.getPartnerEvents() : guild.channels.getCommunityEvents();
                        if (channel == null) {
                            buttonEvent.editMessage(LazyEmoji.NO + " The channel for this event is not set!").setEmbeds().setComponents().queue();
                            return;
                        }
                        buttonEvent.deferEdit().queue();
                        eventAlerts.userEventCooldowns.put(host, System.currentTimeMillis() + 900000L);
                        // Get reactions
                        final List<Emoji> reactions = new ArrayList<>();
                        if (isPartner) {
                            if (roles.contains(eventPings.eventAlerts)) reactions.add(Emoji.fromUnicode("🔔"));
                            if (roles.contains(eventPings.money)) reactions.add(Emoji.fromUnicode("\uD83D\uDCB5"));
                            if (roles.contains(eventPings.fun)) reactions.add(Emoji.fromUnicode("\uD83C\uDF89"));
                            if (roles.contains(eventPings.housing)) reactions.add(Emoji.fromUnicode("🏠"));
                            if (roles.contains(eventPings.civilization)) reactions.add(Emoji.fromUnicode("\uD83C\uDF3E"));
                        } else if (roles.contains(eventPings.community)) {
                            reactions.add(Emoji.fromUnicode("👥"));
                        }
                        // Send/edit messages
                        final boolean notification = time != null && time - System.currentTimeMillis() > 300000;
                        final String rolesString = getRolesString();
                        channel.sendMessage(rolesString.isEmpty() ? "**Loading...** please wait" : rolesString)
                                .flatMap(msg -> {
                                    final MessageEditAction editAction = msg.editMessage(getMessage(new MessageEditBuilder(), false));
                                    if (notification) {
                                        eventAlerts.mongo.eventCollection.collection.insertOne(new Event(channel.getIdLong(), msg.getIdLong(), title.toUpperCase(), host, time));
                                        editAction.setActionRow(
                                            Components.successButton(BUTTON_SUBSCRIBE).build(LazyEmoji.YES_CLEAR.getButtonContent("Follow (0)")),
                                            Components.dangerButton(BUTTON_UNSUBSCRIBE).build(LazyEmoji.NO_CLEAR_DARK.getButtonContent("Unfollow")));
                                    }
                                    return editAction;
                                })
                                .flatMap(msg -> {
                                    RestAction<?> action = msg.createThreadChannel(StringUtility.shorten(title, 100))
                                            .flatMap(thread -> buttonEvent.getHook().editOriginal(LazyEmoji.YES + " **Event posted!** See it [here](<" + msg.getJumpUrl() + ">)").setComponents());
                                    if (!reactions.isEmpty()) for (final Emoji reaction : reactions) action = action.flatMap(reply -> msg.addReaction(reaction));
                                    return action;
                                })
                                .queue();
                    }).build(LazyEmoji.YES_CLEAR.getButtonContent("Post event")),
                    Components.dangerButton(buttonEvent -> buttonEvent.editMessage(LazyEmoji.YES + " Event successfully cancelled").setComponents().queue()).build(LazyEmoji.NO_CLEAR_DARK.getButtonContent("Cancel"))));

            // Return ActionRows
            return actionRows;
        }
    }
}
