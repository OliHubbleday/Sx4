package com.sx4.bot.entities.management;

import java.util.Collection;
import java.util.EnumSet;

public enum LoggerEvent {

    // Usable ids 36, 37, 38

    // Member events
    MEMBER_KICKED(0, LoggerCategory.USER, LoggerCategory.AUDIT),
    MEMBER_JOIN(3, LoggerCategory.USER),
    MEMBER_ROLE_ADD(4, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    MEMBER_ROLE_REMOVE(5, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    MEMBER_NICKNAME_UPDATE(6, LoggerCategory.USER, LoggerCategory.AUDIT),
    MEMBER_LEAVE(7, LoggerCategory.USER),
    MEMBER_BANNED(8, LoggerCategory.USER, LoggerCategory.AUDIT),
    MEMBER_UNBANNED(9, LoggerCategory.USER, LoggerCategory.AUDIT),
    MEMBER_SERVER_VOICE_MUTE(10, LoggerCategory.USER, LoggerCategory.VOICE_CHANNEL, LoggerCategory.AUDIT),
    MEMBER_SERVER_VOICE_UNMUTE(46, LoggerCategory.USER, LoggerCategory.VOICE_CHANNEL, LoggerCategory.AUDIT),
    MEMBER_SERVER_VOICE_DEAFEN(11, LoggerCategory.USER, LoggerCategory.VOICE_CHANNEL, LoggerCategory.AUDIT),
    MEMBER_SERVER_VOICE_UNDEAFEN(47, LoggerCategory.USER, LoggerCategory.VOICE_CHANNEL, LoggerCategory.AUDIT),
    MEMBER_VOICE_JOIN(12, LoggerCategory.USER, LoggerCategory.VOICE_CHANNEL),
    MEMBER_VOICE_LEAVE(13, LoggerCategory.USER, LoggerCategory.VOICE_CHANNEL),
    MEMBER_VOICE_MOVE(14, LoggerCategory.USER, LoggerCategory.AUDIT),
    MEMBER_VOICE_DISCONNECT(32, LoggerCategory.USER, LoggerCategory.VOICE_CHANNEL, LoggerCategory.AUDIT),

    // Emote events
    EMOTE_CREATE(48, LoggerCategory.AUDIT),
    EMOTE_DELETE(49, LoggerCategory.AUDIT),
    EMOTE_NAME_UPDATE(50, LoggerCategory.AUDIT),
    EMOTE_ROLES_UPDATE(51, LoggerCategory.AUDIT),

    // Guild events
    BOT_ADDED(31, LoggerCategory.USER, LoggerCategory.AUDIT),

    // Message events
    MESSAGE_DELETE(1, LoggerCategory.USER, LoggerCategory.TEXT_CHANNEL),
    MESSAGE_UPDATE(2, LoggerCategory.USER, LoggerCategory.TEXT_CHANNEL),

    // Role events
    ROLE_CREATE(27, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    ROLE_DELETE(28, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    ROLE_NAME_UPDATE(29, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    ROLE_PERMISSION_UPDATE(30, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    ROLE_COLOUR_UPDATE(45, LoggerCategory.ROLE, LoggerCategory.AUDIT),

    // Channel events
    THREAD_CHANNEL_DELETE(15, LoggerCategory.TEXT_CHANNEL, LoggerCategory.AUDIT),
    THREAD_CHANNEL_CREATE(16, LoggerCategory.TEXT_CHANNEL, LoggerCategory.AUDIT),
    THREAD_CHANNEL_NAME_UPDATE(17, LoggerCategory.TEXT_CHANNEL, LoggerCategory.AUDIT),
    VOICE_CHANNEL_DELETE(18, LoggerCategory.VOICE_CHANNEL, LoggerCategory.AUDIT),
    VOICE_CHANNEL_CREATE(19, LoggerCategory.VOICE_CHANNEL, LoggerCategory.AUDIT),
    VOICE_CHANNEL_NAME_UPDATE(20, LoggerCategory.VOICE_CHANNEL, LoggerCategory.AUDIT),
    TEXT_CHANNEL_DELETE(21, LoggerCategory.TEXT_CHANNEL, LoggerCategory.AUDIT),
    TEXT_CHANNEL_CREATE(22, LoggerCategory.TEXT_CHANNEL, LoggerCategory.AUDIT),
    TEXT_CHANNEL_NAME_UPDATE(23, LoggerCategory.TEXT_CHANNEL, LoggerCategory.AUDIT),
    CATEGORY_DELETE(24, LoggerCategory.CATEGORY, LoggerCategory.AUDIT),
    CATEGORY_CREATE(25, LoggerCategory.CATEGORY, LoggerCategory.AUDIT),
    CATEGORY_NAME_UPDATE(26, LoggerCategory.CATEGORY, LoggerCategory.AUDIT),
    TEXT_CHANNEL_OVERRIDE_UPDATE(33, LoggerCategory.TEXT_CHANNEL, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    TEXT_CHANNEL_OVERRIDE_DELETE(34, LoggerCategory.TEXT_CHANNEL, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    TEXT_CHANNEL_OVERRIDE_CREATE(35, LoggerCategory.TEXT_CHANNEL, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    VOICE_CHANNEL_OVERRIDE_UPDATE(39, LoggerCategory.VOICE_CHANNEL, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    VOICE_CHANNEL_OVERRIDE_DELETE(40, LoggerCategory.VOICE_CHANNEL, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    VOICE_CHANNEL_OVERRIDE_CREATE(41, LoggerCategory.VOICE_CHANNEL, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    CATEGORY_OVERRIDE_UPDATE(42, LoggerCategory.CATEGORY, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    CATEGORY_OVERRIDE_DELETE(43, LoggerCategory.CATEGORY, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT),
    CATEGORY_OVERRIDE_CREATE(44, LoggerCategory.CATEGORY, LoggerCategory.USER, LoggerCategory.ROLE, LoggerCategory.AUDIT);

    public static final long ALL = LoggerEvent.getRaw(LoggerEvent.values());
    private static final LoggerEvent[] EMPTY = new LoggerEvent[0];

    private final long raw;
    private final LoggerCategory[] categories;

    private LoggerEvent(int offset, LoggerCategory... categories) {
        this.raw = 1L << offset;
        this.categories = categories;
    }

    public long getRaw() {
        return this.raw;
    }

    public LoggerCategory[] getCategories() {
        return this.categories;
    }

    public boolean containsCategory(LoggerCategory category) {
        for (LoggerCategory eventCategory : this.categories) {
            if (eventCategory == category) {
                return true;
            }
        }

        return false;
    }

    public static EnumSet<LoggerEvent> getEvents(long raw) {
        if (raw == 0L) {
            return EnumSet.noneOf(LoggerEvent.class);
        }

        EnumSet<LoggerEvent> events = EnumSet.noneOf(LoggerEvent.class);
        for (LoggerEvent event : LoggerEvent.values()) {
            if ((raw & event.getRaw()) == event.getRaw()) {
                events.add(event);
            }
        }

        return events;
    }

    public static long getRaw(LoggerEvent... events) {
        long raw = 0;
        for (LoggerEvent event : events) {
            raw |= event.getRaw();
        }

        return raw;
    }

    public static long getRaw(Collection<LoggerEvent> events) {
        return LoggerEvent.getRaw(events.toArray(LoggerEvent.EMPTY));
    }

}
