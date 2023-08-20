package xyz.srnyx.eventalerts.commands.global;

import com.freya02.botcommands.api.annotations.CommandMarker;
import com.freya02.botcommands.api.annotations.Dependency;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandScope;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GlobalSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.botcommands.api.components.Components;
import com.freya02.botcommands.api.components.event.ButtonEvent;
import com.freya02.botcommands.api.modals.Modals;
import com.freya02.botcommands.api.modals.annotations.ModalHandler;
import com.freya02.botcommands.api.modals.annotations.ModalInput;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.requests.RestAction;

import org.bson.conversions.Bson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.eventalerts.EventAlerts;
import xyz.srnyx.eventalerts.ServerTag;
import xyz.srnyx.eventalerts.mongo.objects.Server;

import xyz.srnyx.lazylibrary.LazyEmoji;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@CommandMarker
public class ServerCmd extends ApplicationCommand {
    @NotNull private static final String MODAL_SET_DESCRIPTION = "ServerCmd.modal.set.description";
    @NotNull private static final String MODAL_EDIT_NAME = "ServerCmd.modal.edit.name";
    @NotNull private static final String MODAL_EDIT_INVITE = "ServerCmd.modal.edit.invite";
    @NotNull private static final String MODAL_EDIT_COLOR = "ServerCmd.modal.edit.color";
    @NotNull private static final String MODAL_EDIT_DESCRIPTION = "ServerCmd.modal.edit.description";

    @NotNull private static final List<SelectOption> EDIT_OPTIONS = List.of(
            SelectOption.of("Name", "name").withEmoji(Emoji.fromUnicode("\uD83D\uDCDB")),
            SelectOption.of("Description", "description").withEmoji(Emoji.fromUnicode("\uD83D\uDCDD")),
            SelectOption.of("Invite", "invite").withEmoji(Emoji.fromUnicode("\uD83D\uDD17")),
            SelectOption.of("Tags", "tags").withEmoji(Emoji.fromUnicode("\uD83C\uDFF7")),
            SelectOption.of("Representatives", "representatives").withEmoji(Emoji.fromUnicode("\uD83D\uDC65")),
            SelectOption.of("Color", "color").withEmoji(Emoji.fromUnicode("\uD83C\uDFA8")));

    @Dependency private EventAlerts eventAlerts;

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "server",
            subcommand = "get",
            description = "Get a partner's server")
    public void get(@NotNull GlobalSlashEvent event,
                    @AppOption(description = "The partner") @NotNull Member partner) {
        final long partnerId = partner.getIdLong();
        final String mention = partner.getAsMention();

        // Check if partner
        if (!eventAlerts.config.guild.roles.isPartner(partnerId)) {
            event.reply(LazyEmoji.NO + " " + mention + " is not a partner!").setEphemeral(true).queue();
            return;
        }

        // Check if server is set
        final Server server = eventAlerts.mongo.serversCollection.findOne("user", partnerId);
        if (server == null) {
            event.reply(LazyEmoji.NO + " " + mention + " has not set a server!").setEphemeral(true).queue();
            return;
        }

        // Send server
        event.replyEmbeds(server.getEmbed(eventAlerts).build()).setEphemeral(true).queue();
    }

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "server",
            subcommand = "set",
            description = "Set your server")
    public void set(@NotNull GlobalSlashEvent event,
                    @AppOption(description = "The name of your event server") @NotNull String name,
                    @AppOption(description = "The invite link to your server") @NotNull String invite,
                    @AppOption(description = "Any other representatives of your server (mention them, separate with spaces)") @Nullable String representatives,
                    @AppOption(description = "The hex color that your server embed should be") @Nullable String color,
                    @AppOption(description = "The thumbnail/icon image URL of your server") @Nullable String thumbnail) {
        if (!eventAlerts.config.guild.roles.checkIsPartner(event)) return;
        final User user = event.getUser();
        final Bson filter = Filters.eq("user", user.getIdLong());

        // Check if server is already set
        if (eventAlerts.mongo.serversCollection.findOne(filter) != null) {
            event.reply(LazyEmoji.NO + " You have already set a server! Use </server edit:1140888072897183776> to modify it").setEphemeral(true).queue();
            return;
        }

        // Check lengths
        final long nameLength = name.length();
        if (nameLength > 256) {
            event.reply(LazyEmoji.NO + " Your server name is too long (`" + nameLength + "/256`)!").setEphemeral(true).queue();
            return;
        }
        final long inviteLength = invite.length();
        if (inviteLength > 1007) {
            event.reply(LazyEmoji.NO + " Your server invite is too long (`" + inviteLength + "/1007`)!").setEphemeral(true).queue();
            return;
        }

        // Check thumbnail
        if (thumbnail != null) {
            final long thumbnailLength = thumbnail.length();
            if (thumbnailLength > 2000) {
                event.reply(LazyEmoji.NO + " Your server thumbnail is too long (`" + thumbnailLength + "/2000`)!").setEphemeral(true).queue();
                return;
            }
            if (!EmbedBuilder.URL_PATTERN.matcher(thumbnail).matches()) {
                event.reply(LazyEmoji.NO + " Your thumbnail is not a valid URL; `" + thumbnail + "`").setEphemeral(true).queue();
                return;
            }
        }

        // Extract invite code
        if (invite.startsWith("https://discord.gg/")) {
            invite = invite.substring(19);
        } else if (invite.startsWith("discord.gg/")) {
            invite = invite.substring(11);
        }

        // Check if invite URL is valid
        final String inviteUrl = "https://discord.gg/" + invite;
        if (!EmbedBuilder.URL_PATTERN.matcher(inviteUrl).matches()) {
            event.reply(LazyEmoji.NO + " Your invite is not a valid Discord invite; `" + inviteUrl + "`").setEphemeral(true).queue();
            return;
        }

        // Get color
        Integer colorInt = null;
        if (color != null) {
            if (color.startsWith("#")) color = color.substring(1);
            if (color.length() != 6) {
                event.reply(LazyEmoji.NO + " Your color must be a valid hex color (6 characters)!").setEphemeral(true).queue();
                return;
            }
            try {
                colorInt = Integer.parseInt(color, 16);
            } catch (final NumberFormatException e) {
                event.reply(LazyEmoji.NO + " Your color must be a valid hex color!").setEphemeral(true).queue();
                return;
            }
        }

        // Get representatives
        List<Long> representativeList = null;
        if (representatives != null) {
            final long representativesLength = representatives.length() + user.getId().length();
            if (representativesLength > 1020) {
                event.reply(LazyEmoji.NO + " You have too many representatives (`" + representativesLength + "/1024`)!").setEphemeral(true).queue();
                return;
            }
            representativeList = new ArrayList<>();
            for (final String representative : representatives.split(" ")) if (representative.startsWith("<@") && representative.endsWith(">")) {
                String id = representative.substring(2, representative.length() - 1);
                if (id.startsWith("!")) id = id.substring(1);
                try {
                    representativeList.add(Long.parseLong(id));
                } catch (final NumberFormatException e) {
                    event.reply(LazyEmoji.NO + " One of your representatives is not a valid user (`" + representative + "`)!").setEphemeral(true).queue();
                    return;
                }
            }
        }

        // Set server
        Bson update = Updates.combine(
                Updates.set("name", name),
                Updates.set("invite", invite));
        if (representativeList != null) update = Updates.combine(update, Updates.set("representatives", representativeList));
        if (colorInt != null) update = Updates.combine(update, Updates.set("color", colorInt));
        if (thumbnail != null) update = Updates.combine(update, Updates.set("thumbnail", thumbnail));
        final Server server = eventAlerts.mongo.serversCollection.findOneAndUpsert(filter, update);
        if (server == null) {
            event.reply(LazyEmoji.NO + " An error occurred while setting your server!").setEphemeral(true).queue();
            return;
        }

        // Tag setting
        final Set<String> tags = new HashSet<>();
        event.reply(LazyEmoji.YES + " Use the dropdown menu to select your tags!")
                .setEmbeds(server.getEmbed(eventAlerts).build())
                .addActionRow(
                        Components.stringSelectionMenu(menuEvent -> {
                            menuEvent.deferEdit().queue();
                            tags.clear();
                            tags.addAll(menuEvent.getSelectedOptions().stream()
                                    .map(SelectOption::getValue)
                                    .collect(Collectors.toSet()));
                        }).setMaxValues(5).addOptions(ServerTag.OPTIONS).build())
                .addActionRow(
                        Components.successButton(buttonEvent -> {
                            eventAlerts.mongo.serversCollection.updateOne(filter, Updates.set("tags", tags));

                            // Description setting
                            buttonEvent.replyModal(Modals.create("Server Description", MODAL_SET_DESCRIPTION)
                                    .addActionRow(Modals.createTextInput("description", "Description", TextInputStyle.PARAGRAPH)
                                            .setPlaceholder("Enter your server description here")
                                            .setMaxLength(4000).build()).build()).queue();
                        }).build(LazyEmoji.YES_CLEAR.getButtonContent("Done")))
                .setEphemeral(true)
                .queue();

    }

    @JDASlashCommand(
            scope = CommandScope.GLOBAL,
            name = "server",
            subcommand = "edit",
            description = "Edit your server")
    public void edit(@NotNull GlobalSlashEvent event) {
        if (!eventAlerts.config.guild.roles.checkIsPartner(event)) return;

        final long userId = event.getUser().getIdLong();
        final Server server = eventAlerts.mongo.serversCollection.findOne("user", userId);
        if (server == null) {
            event.reply(LazyEmoji.NO + " You have not set a server!").setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(server.getEmbed(eventAlerts).build())
                .addActionRow(Components.stringSelectionMenu(menuEvent -> {
                    final String value = menuEvent.getSelectedOptions().get(0).getValue();

                    // name
                    if (value.equals("name")) {
                        menuEvent.replyModal(Modals.create("Server Name", MODAL_EDIT_NAME)
                                .addActionRow(Modals.createTextInput("name", "Name", TextInputStyle.SHORT)
                                        .setPlaceholder("Enter your server's name here")
                                        .setMaxLength(256).build()).build()).queue();
                        return;
                    }

                    // description
                    if (value.equals("description")) {
                        menuEvent.replyModal(Modals.create("Server Description", MODAL_EDIT_DESCRIPTION)
                                .addActionRow(Modals.createTextInput("description", "Description", TextInputStyle.PARAGRAPH)
                                        .setPlaceholder("Enter your server's description here")
                                        .setMaxLength(4000).build()).build()).queue();
                        return;
                    }

                    // invite
                    if (value.equals("invite")) {
                        menuEvent.replyModal(Modals.create("Server Invite", MODAL_EDIT_INVITE)
                                .addActionRow(Modals.createTextInput("invite", "Invite", TextInputStyle.SHORT)
                                        .setPlaceholder("Enter your server's Discord invite here")
                                        .setMaxLength(1007).build()).build()).queue();
                        return;
                    }

                    // tags
                    if (value.equals("tags")) {
                        menuEvent.editMessage(LazyEmoji.NO + " Tag editing is not yet implemented, try again later!").queue();
                        //TODO
                        return;
                    }

                    // representatives
                    if (value.equals("representatives")) {
                        menuEvent.editMessage(LazyEmoji.NO + " Representative editing is not yet implemented, try again later!").queue();
                        //TODO
                        return;
                    }

                    // color
                    if (value.equals("color")) {
                        menuEvent.replyModal(Modals.create("Server Color", MODAL_EDIT_COLOR)
                                .addActionRow(Modals.createTextInput("color", "Color", TextInputStyle.SHORT)
                                        .setPlaceholder("Enter your server's color hex here")
                                        .setMaxLength(7).build()).build()).queue();
                        return;
                    }

                    // Unknown
                    menuEvent.reply(LazyEmoji.NO + " An unknown error occurred!").setEphemeral(true).queue();
                }).setMaxValues(1).addOptions(EDIT_OPTIONS).build())
                .addActionRow(
                        Components.successButton(buttonEvent -> {
                            final Server updatedServer = eventAlerts.mongo.serversCollection.findOne("user", userId);
                            if (updatedServer == null) {
                                buttonEvent.reply(LazyEmoji.NO + " An error occurred while updating your server!").setEphemeral(true).queue();
                                return;
                            }
                            sendEditMessage(buttonEvent, updatedServer, message -> buttonEvent.editMessage(LazyEmoji.YES + " Your server has been updated, see it here: " + message.getJumpUrl()).setComponents(Collections.emptySet()));
                        }).build(LazyEmoji.YES_CLEAR.getButtonContent("Done!")))
                .setEphemeral(true)
                .queue();
    }

    @ModalHandler(name = MODAL_SET_DESCRIPTION)
    public void modalSetDescription(@NotNull ModalInteractionEvent event,
                                    @ModalInput(name = "description") @NotNull String description) {
        event.deferEdit().queue();
        final Bson filter = Filters.eq("user", event.getUser().getIdLong());
        final Server server = eventAlerts.mongo.serversCollection.findOneAndUpdate(filter, Updates.set("description", description));
        if (server == null) {
            event.getHook().editOriginal(LazyEmoji.NO + " An error occurred while setting your description!")
                    .setEmbeds(Collections.emptySet())
                    .setComponents(Collections.emptySet()).queue();
            return;
        }

        // Edit embed
        event.getHook().editOriginal(LazyEmoji.YES + " Your description has been set, you're all done! Does everything look good?")
                .setEmbeds(server.getEmbed(eventAlerts).build())
                .setActionRow(
                        Components.successButton(buttonEvent -> sendEditMessage(buttonEvent, server, message -> buttonEvent.editMessage(LazyEmoji.YES + " Your server has been set, see it here: " + message.getJumpUrl())
                                        .setEmbeds(Collections.emptySet())
                                        .setComponents(Collections.emptySet())))
                                .build(LazyEmoji.YES_CLEAR.getButtonContent("All good!")),
                        Components.dangerButton(buttonEvent -> {
                            eventAlerts.mongo.serversCollection.deleteOne(filter);
                            buttonEvent.editMessage(LazyEmoji.NO + " **Yikes!** You'll have to restart the process, here's a copy of your description: ```" + description + "```")
                                    .setEmbeds(Collections.emptySet())
                                    .setComponents(Collections.emptySet()).queue();
                        }).build(LazyEmoji.NO_CLEAR_DARK.getButtonContent("Something's wrong!")))
                .queue();
    }

    @ModalHandler(name = MODAL_EDIT_NAME)
    public void modalEditName(@NotNull ModalInteractionEvent event,
                              @ModalInput(name = "name") @NotNull String name) {
        event.deferEdit().queue();
        final InteractionHook hook = event.getHook();
        final Server server = eventAlerts.mongo.serversCollection.findOneAndUpdate(Filters.eq("user", event.getUser().getIdLong()), Updates.set("name", name));
        if (server == null) {
            hook.editOriginal(LazyEmoji.NO + " An error occurred while setting your name!")
                    .setEmbeds(Collections.emptySet())
                    .queue();
            return;
        }

        // Edit embed
        hook.editOriginal(LazyEmoji.YES + " Successfully changed your server's name!")
                .setEmbeds(server.getEmbed(eventAlerts).build())
                .queue();
    }

    @ModalHandler(name = MODAL_EDIT_DESCRIPTION)
    public void modalEditDescription(@NotNull ModalInteractionEvent event,
                                     @ModalInput(name = "description") @NotNull String description) {
        event.deferEdit().queue();
        final InteractionHook hook = event.getHook();
        final Server server = eventAlerts.mongo.serversCollection.findOneAndUpdate(Filters.eq("user", event.getUser().getIdLong()), Updates.set("description", description));
        if (server == null) {
            hook.editOriginal(LazyEmoji.NO + " An error occurred while setting your description!")
                    .setEmbeds(Collections.emptySet())
                    .queue();
            return;
        }

        // Edit embed
        hook.editOriginal(LazyEmoji.YES + " Successfully changed your server's description!")
                .setEmbeds(server.getEmbed(eventAlerts).build())
                .queue();
    }

    @ModalHandler(name = MODAL_EDIT_INVITE)
    public void modalEditInvite(@NotNull ModalInteractionEvent event,
                                @ModalInput(name = "invite") @NotNull String invite) {
        event.deferEdit().queue();
        final InteractionHook hook = event.getHook();
        final Server server = eventAlerts.mongo.serversCollection.findOneAndUpdate(Filters.eq("user", event.getUser().getIdLong()), Updates.set("invite", invite));
        if (server == null) {
            hook.editOriginal(LazyEmoji.NO + " An error occurred while setting your invite!")
                    .setEmbeds(Collections.emptySet())
                    .queue();
            return;
        }

        // Edit embed
        hook.editOriginal(LazyEmoji.YES + " Successfully changed your server's invite!")
                .setEmbeds(server.getEmbed(eventAlerts).build())
                .queue();
    }

    @ModalHandler(name = MODAL_EDIT_COLOR)
    public void modalEditColor(@NotNull ModalInteractionEvent event,
                               @ModalInput(name = "color") @NotNull String color) {
        event.deferEdit().queue();
        final InteractionHook hook = event.getHook();

        // Format color
        if (color.startsWith("#")) color = color.substring(1);
        if (color.length() != 6) {
            hook.editOriginal(LazyEmoji.NO + " Your color must be a valid hex color (6 characters)!")
                    .setEmbeds(Collections.emptySet())
                    .queue();
            return;
        }

        // Parse color
        final int colorInt;
        try {
            colorInt = Integer.parseInt(color, 16);
        } catch (final NumberFormatException e) {
            hook.editOriginal(LazyEmoji.NO + " Your color must be a valid hex color!")
                    .setEmbeds(Collections.emptySet())
                    .queue();
            return;
        }

        // Update color
        final Server server = eventAlerts.mongo.serversCollection.findOneAndUpdate(Filters.eq("user", event.getUser().getIdLong()), Updates.set("color", colorInt));
        if (server == null) {
            hook.editOriginal(LazyEmoji.NO + " An error occurred while setting your color!")
                    .setEmbeds(Collections.emptySet())
                    .queue();
            return;
        }

        // Edit embed
        hook.editOriginal(LazyEmoji.YES + " Successfully changed your server's color!")
                .setEmbeds(server.getEmbed(eventAlerts).build())
                .queue();
    }

    private void sendEditMessage(@NotNull ButtonEvent event, @NotNull Server server, @NotNull Function<Message, RestAction<InteractionHook>> consumer) {
        final RestAction<Message> serverMessage = server.getMessage(eventAlerts);
        if (serverMessage != null) {
            serverMessage
                    .flatMap(message -> message.editMessageEmbeds(server.getEmbed(eventAlerts).build()))
                    .flatMap(consumer)
                    .queue(s -> {}, e -> sendMessage(event, server, consumer));
            return;
        }
        sendMessage(event, server, consumer);
    }

    private void sendMessage(@NotNull ButtonEvent event, @NotNull Server server, @NotNull Function<Message, RestAction<InteractionHook>> consumer) {
        final GuildMessageChannel channel = eventAlerts.config.guild.channels.getServers();
        if (channel != null) channel.sendMessageEmbeds(server.getEmbed(eventAlerts).build())
                .flatMap(message -> {
                    eventAlerts.mongo.serversCollection.updateOne(Filters.eq("user", event.getUser().getIdLong()), Updates.set("message", message.getIdLong()));
                    return consumer.apply(message);
                })
                .queue();
    }
}
