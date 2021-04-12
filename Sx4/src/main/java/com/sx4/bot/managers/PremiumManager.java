package com.sx4.bot.managers;

import com.mongodb.client.model.*;
import com.sx4.bot.core.Sx4;
import com.sx4.bot.database.Database;
import org.bson.Document;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PremiumManager {

	private final Map<Long, ScheduledFuture<?>> executors;
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private final Sx4 bot;

	public PremiumManager(Sx4 bot) {
		this.executors = new HashMap<>();
		this.bot = bot;
	}

	public ScheduledExecutorService getExecutor() {
		return this.executor;
	}

	public void schedulePremiumExpiry(long guildId, long seconds) {
		ScheduledFuture<?> oldExecutor = this.executors.get(guildId);
		if (oldExecutor != null && !oldExecutor.isDone()) {
			oldExecutor.cancel(true);
		}

		this.executors.put(guildId, this.executor.schedule(() -> this.endPremium(guildId), seconds, TimeUnit.SECONDS));
	}

	public void endPremium(long guildId) {
		UpdateOneModel<Document> model = this.endPremiumBulk(guildId);
		if (model != null) {
			this.bot.getDatabase().updateGuild(model).whenComplete(Database.exceptionally(this.bot.getShardManager()));
		}
	}

	public UpdateOneModel<Document> endPremiumBulk(long guildId) {
		// remove premium features

		return new UpdateOneModel<>(Filters.eq("_id", guildId), Updates.unset("premium"));
	}

	public void ensurePremiumExpiry() {
		List<WriteModel<Document>> bulkData = new ArrayList<>();

		this.bot.getDatabase().getGuilds(Filters.exists("premium.endAt"), Projections.include("premium.endAt")).forEach(data -> {
			long endAt = data.getEmbedded(List.of("premium", "endAt"), 0L), timeNow = Clock.systemUTC().instant().getEpochSecond();
			if (endAt != 0) {
				if (endAt - timeNow > 0) {
					this.schedulePremiumExpiry(data.getLong("_id"), endAt - timeNow);
				} else {
					bulkData.add(this.endPremiumBulk(data.getLong("_id")));
				}
			}
		});

		if (!bulkData.isEmpty()) {
			this.bot.getDatabase().bulkWriteGuilds(bulkData).whenComplete(Database.exceptionally(this.bot.getShardManager()));
		}
	}

}