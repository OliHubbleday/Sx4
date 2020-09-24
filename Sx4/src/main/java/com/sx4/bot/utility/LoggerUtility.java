package com.sx4.bot.utility;

import club.minnced.discord.webhook.send.WebhookEmbed;
import com.sx4.bot.entities.management.logger.LoggerCategory;
import com.sx4.bot.entities.management.logger.LoggerEvent;
import net.dv8tion.jda.api.entities.*;

import java.util.*;
import java.util.stream.Collectors;

public class LoggerUtility {

    public static int getWebhookEmbedLength(WebhookEmbed embed) {
        int length = 0;

        String title = embed.getTitle() != null ? embed.getTitle().getText().trim() : null;
        if (title != null) {
            length += title.length();
        }

        String description = embed.getDescription() != null ? embed.getDescription().trim() : null;
        if (description != null) {
            length += description.length();
        }

        String author = embed.getAuthor() != null ? embed.getAuthor().getName().trim() : null;
        if (author != null) {
            length += author.length();
        }

        String footer = embed.getFooter() != null ? embed.getFooter().getText().trim() : null;
        if (footer != null) {
            length += footer.length();
        }

        for (WebhookEmbed.EmbedField field : embed.getFields()) {
            length += field.getName().trim().length() + field.getValue().trim().length();
        }

        return length;
    }

    public static int getWebhookEmbedLength(List<WebhookEmbed> embeds) {
        return embeds.stream()
            .mapToInt(LoggerUtility::getWebhookEmbedLength)
            .sum();
    }

    public static Set<LoggerCategory> getCommonCategories(LoggerEvent... events) {
        if (events.length == 1) {
            return new HashSet<>(Arrays.asList(events[0].getCategories()));
        }

        List<List<LoggerCategory>> eventCategories = Arrays.stream(events)
            .map(LoggerEvent::getCategories)
            .map(Arrays::asList)
            .collect(Collectors.toList());

        Set<LoggerCategory> common = new LinkedHashSet<>();
        if (!eventCategories.isEmpty()) {
            Iterator<List<LoggerCategory>> iterator = eventCategories.iterator();
            common.addAll(iterator.next());
            while (iterator.hasNext()) {
                common.retainAll(iterator.next());
            }
        }

        return common;
    }

    public static long getEntityIdFromType(String query, Guild guild, LoggerCategory loggerCategory) {
        switch (loggerCategory) {
            case AUDIT:
            case USER:
                Member member = SearchUtility.getMember(guild, query);
                return member == null ? 0L : member.getIdLong();
            case VOICE_CHANNEL:
                VoiceChannel voiceChannel = SearchUtility.getVoiceChannel(guild, query);
                return voiceChannel == null ? 0L : voiceChannel.getIdLong();
            case CATEGORY:
                Category category = SearchUtility.getCategory(guild, query);
                return category == null ? 0L : category.getIdLong();
            case TEXT_CHANNEL:
                TextChannel textChannel = SearchUtility.getTextChannel(guild, query);
                return textChannel == null ? 0L : textChannel.getIdLong();
            case ROLE:
                Role role = SearchUtility.getRole(guild, query);
                return role == null ? 0L : role.getIdLong();
            case STORE_CHANNEL:
                StoreChannel storeChannel = SearchUtility.getStoreChannel(guild, query);
                return storeChannel == null ? 0L : storeChannel.getIdLong();
        }

        return 0L;
    }

}