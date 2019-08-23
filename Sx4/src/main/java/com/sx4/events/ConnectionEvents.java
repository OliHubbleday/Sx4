package com.sx4.events;

import java.time.Instant;

import com.sx4.cache.SteamCache;
import com.sx4.core.Sx4Bot;
import com.sx4.settings.Settings;
import com.sx4.utils.HelpUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.GatewayPingEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ResumedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ConnectionEvents extends ListenerAdapter {
	
	public static long lastGatewayPing = -1;
	
	public static long getLastGatewayPing() {
		return ConnectionEvents.lastGatewayPing;
	}
	
	private int readyEventsCalled = 0;
	
	public void onReady(ReadyEvent event) {
		this.readyEventsCalled++;
		if (this.readyEventsCalled == Sx4Bot.getShardManager().getShardsTotal()) {
			int availableGuilds = event.getGuildAvailableCount();
			int totalGuilds = event.getGuildTotalCount();
			System.out.println(String.format("Connected to %s with %,d/%,d available servers and %,d users", event.getJDA().getSelfUser().getAsTag(), availableGuilds, totalGuilds, Sx4Bot.getShardManager().getUsers().size()));

			SteamCache.getGames();
			HelpUtils.ensureAdvertisement();
			StatusEvents.initialize();
			ServerPostEvents.initializePosting();
			MuteEvents.ensureMuteRoles();
			StatsEvents.initializeBotLogs();
			StatsEvents.initializeGuildStats();
			ReminderEvents.ensureReminders();
			GiveawayEvents.ensureGiveaways();
			AwaitEvents.ensureAwaitData();
			MuteEvents.ensureMutes();
			AutoroleEvents.ensureAutoroles();
			
			System.gc();
		}
	}
	
	public void onGatewayPing(GatewayPingEvent event) {
		ConnectionEvents.lastGatewayPing = event.getOldPing();
	}
	
	public void onDisconnect(DisconnectEvent event) {
		TextChannel eventsChannel = Sx4Bot.getShardManager().getGuildById(Settings.SUPPORT_SERVER_ID).getTextChannelById(Settings.EVENTS_CHANNEL_ID);
		
		EmbedBuilder embed = new EmbedBuilder();
		embed.setAuthor(event.getJDA().getSelfUser().getAsTag(), null, event.getJDA().getSelfUser().getEffectiveAvatarUrl());
		embed.setTimestamp(event.getTimeDisconnected());
		embed.setColor(Settings.COLOR_RED);
		embed.addField("Shard", (event.getJDA().getShardInfo().getShardId() + 1) + "/" + event.getJDA().getShardInfo().getShardTotal(), false);
		if (event.getCloseCode() != null) {
			embed.addField("Reason", event.getCloseCode().getMeaning() + " [" + event.getCloseCode().getCode() + "]", false);
		}
		embed.setFooter("Disconnect", null);
		eventsChannel.sendMessage(embed.build()).queue();
	}
	
	public void onResume(ResumedEvent event) {
		TextChannel eventsChannel = Sx4Bot.getShardManager().getGuildById(Settings.SUPPORT_SERVER_ID).getTextChannelById(Settings.EVENTS_CHANNEL_ID);
		
		EmbedBuilder embed = new EmbedBuilder();
		embed.setAuthor(event.getJDA().getSelfUser().getAsTag(), null, event.getJDA().getSelfUser().getEffectiveAvatarUrl());
		embed.setTimestamp(Instant.now());
		embed.setColor(Settings.COLOR_GREEN);
		embed.addField("Shard", (event.getJDA().getShardInfo().getShardId() + 1) + "/" + event.getJDA().getShardInfo().getShardTotal(), true);
		embed.setFooter("Resume", null);
		eventsChannel.sendMessage(embed.build()).queue();
	}
	
	public void onReconnect(ReconnectedEvent event) {
		TextChannel eventsChannel = Sx4Bot.getShardManager().getGuildById(Settings.SUPPORT_SERVER_ID).getTextChannelById(Settings.EVENTS_CHANNEL_ID);
		
		EmbedBuilder embed = new EmbedBuilder();
		embed.setAuthor(event.getJDA().getSelfUser().getAsTag(), null, event.getJDA().getSelfUser().getEffectiveAvatarUrl());
		embed.setTimestamp(Instant.now());
		embed.setColor(Settings.COLOR_GREEN);
		embed.addField("Shard", (event.getJDA().getShardInfo().getShardId() + 1) + "/" + event.getJDA().getShardInfo().getShardTotal(), true);
		embed.setFooter("Reconnect", null);
		eventsChannel.sendMessage(embed.build()).queue();
	}
	
}
