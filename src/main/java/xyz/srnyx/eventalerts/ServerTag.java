package xyz.srnyx.eventalerts;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;


public enum ServerTag {
    CIVILIZATION(Emoji.fromUnicode("\uD83C\uDF3E")),
    FUN(Emoji.fromUnicode("\uD83C\uDF89")),
    HYPIXEL(Emoji.fromUnicode("\uD83C\uDFE0")),
    LUCK(Emoji.fromUnicode("\uD83C\uDF40")),
    MINEHUT(Emoji.fromUnicode("\uD83D\uDED6")),
    MONEY(Emoji.fromUnicode("\uD83D\uDCB5")),
    PVP(Emoji.fromUnicode("⚔")),
    SKILL(Emoji.fromUnicode("\uD83D\uDCDA")),
    STREAMS(Emoji.fromUnicode("\uD83D\uDD34")),
    VIDEOS(Emoji.fromUnicode("\uD83D\uDCF9"));

    @NotNull public static final List<SelectOption> OPTIONS = Arrays.stream(values())
            .map(tag -> {
                final String name = tag.name();
                return SelectOption.of(name, name).withEmoji(tag.emoji);
            })
            .toList();

    @NotNull public final UnicodeEmoji emoji;

    ServerTag(@NotNull UnicodeEmoji emoji) {
        this.emoji = emoji;
    }
}
