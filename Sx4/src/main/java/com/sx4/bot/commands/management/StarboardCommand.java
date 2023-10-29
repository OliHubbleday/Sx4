package com.sx4.bot.commands.management;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.mongodb.client.model.*;
import com.sx4.bot.annotations.argument.AlternativeOptions;
import com.sx4.bot.annotations.argument.ImageUrl;
import com.sx4.bot.annotations.argument.Limit;
import com.sx4.bot.annotations.command.*;
import com.sx4.bot.category.ModuleCategory;
import com.sx4.bot.core.Sx4Command;
import com.sx4.bot.core.Sx4CommandEvent;
import com.sx4.bot.database.mongo.model.Operators;
import com.sx4.bot.entities.argument.Alternative;
import com.sx4.bot.entities.interaction.ButtonType;
import com.sx4.bot.entities.interaction.CustomButtonId;
import com.sx4.bot.entities.webhook.WebhookChannel;
import com.sx4.bot.formatter.output.FormatterManager;
import com.sx4.bot.formatter.output.function.FormatterVariable;
import com.sx4.bot.managers.StarboardManager;
import com.sx4.bot.paged.MessagePagedResult;
import com.sx4.bot.utility.ExceptionUtility;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class StarboardCommand extends Sx4Command {

	public StarboardCommand() {
		super("starboard", 196);

		super.setDescription("Setup starboard in your server to favourite messages");
		super.setExamples("starboard toggle", "starboard channel", "starboard top");
		super.setCategoryAll(ModuleCategory.MANAGEMENT);
	}

	public void onCommand(Sx4CommandEvent event) {
		event.replyHelp().queue();
	}

	@Command(value="toggle", description="Toggle the state of starboard in the server")
	@CommandId(197)
	@Examples({"starboard toggle"})
	@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
	public void toggle(Sx4CommandEvent event) {
		List<Bson> update = List.of(Operators.set("starboard.enabled", Operators.cond("$starboard.enabled", Operators.REMOVE, true)));
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().projection(Projections.include("starboard.enabled")).returnDocument(ReturnDocument.AFTER).upsert(true);
		event.getMongo().findAndUpdateGuildById(event.getGuild().getIdLong(), update, options).whenComplete((data, exception) -> {
			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}

			event.replySuccess("Starboard is now " + (data.getEmbedded(List.of("starboard", "enabled"), false) ? "enabled" : "disabled")).queue();
		});
	}

	@Command(value="channel", description="Sets the channel for starboard messages to be sent in")
	@CommandId(198)
	@Examples({"starboard channel", "starboard channel #starboard", "starboard channel reset"})
	@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
	public void channel(Sx4CommandEvent event, @Argument(value="channel | reset", endless=true, nullDefault=true) @AlternativeOptions("reset") Alternative<WebhookChannel> option) {
		WebhookChannel channel = option.isAlternative() ? null : option.getValue();

		List<Bson> update = List.of(Operators.set("starboard.channelId", channel == null ? Operators.REMOVE : channel.getIdLong()), Operators.unset("starboard.webhook.id"), Operators.unset("starboard.webhook.token"));
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE).projection(Projections.include("starboard.webhook.id", "starboard.channelId")).upsert(true);

		event.getMongo().findAndUpdateGuildById(event.getGuild().getIdLong(), update, options).whenComplete((data, exception) -> {
			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}

			long channelId = data == null ? 0L : data.getEmbedded(List.of("starboard", "channelId"), 0L);
			event.getBot().getStarboardManager().removeWebhook(channelId);

			if ((channel == null ? 0L : channel.getIdLong()) == channelId) {
				event.replyFailure("The starboard channel is already " + (channel == null ? "unset" : "set to " + channel.getAsMention())).queue();
				return;
			}

			GuildMessageChannelUnion oldChannel = channelId == 0L ? null : event.getGuild().getChannelById(GuildMessageChannelUnion.class, channelId);
			long webhookId = data == null ? 0L : data.getEmbedded(List.of("starboard", "webhook", "id"), 0L);

			if (oldChannel != null && webhookId != 0L) {
				((IWebhookContainer) oldChannel).deleteWebhookById(Long.toString(webhookId)).queue(null, ErrorResponseException.ignore(ErrorResponse.UNKNOWN_WEBHOOK));
			}

			event.replySuccess("The starboard channel has been " + (channel == null ? "unset" : "set to " + channel.getAsMention())).queue();
		});
	}

	@Command(value="emote", aliases={"emoji"}, description="Sets the emote/emoji to be used for starboard")
	@CommandId(199)
	@Examples({"starboard emote ☝️", "starboard emote <:upvote:761345612079693865>", "starboard emote reset"})
	@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
	public void emote(Sx4CommandEvent event, @Argument(value="emote | reset", endless=true) @AlternativeOptions("reset") Alternative<EmojiUnion> option) {
		EmojiUnion emoji = option.getValue();
		boolean unicode = emoji instanceof UnicodeEmoji;

		List<Bson> update = emoji == null ? List.of(Operators.unset("starboard.emote")) : List.of(Operators.set("starboard.emote." + (unicode ? "name" : "id"), unicode ? emoji.getName() : emoji.asCustom().getIdLong()), Operators.unset("starboard.emote." + (unicode ? "id" : "name")));

		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE).upsert(true).projection(Projections.include("starboard.emote"));
		event.getMongo().findAndUpdateGuildById(event.getGuild().getIdLong(), update, options).whenComplete((data, exception) -> {
			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}

			Document emoteData = data == null ? null : data.getEmbedded(List.of("starboard", "emote"), Document.class);
			if ((emoji == null && emoteData == null) || (emoji != null && emoteData != null && (unicode ? emoji.getName().equals(emoteData.getString("name")) : emoteData.getLong("id") == emoji.asCustom().getIdLong()))) {
				event.replyFailure("Your starboard emote was already " + (emoji == null ? "unset" : "set to that")).queue();
				return;
			}

			event.replySuccess("Your starboard emote has been " + (emoji == null ? "unset" : "updated")).queue();
		});
	}

	@Command(value="delete", aliases={"remove"}, description="Deletes a starboard")
	@CommandId(204)
	@Examples({"starboard delete 5ff636647f93247aeb2ac429", "starboard delete all"})
	@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
	public void delete(Sx4CommandEvent event, @Argument(value="id | all") @AlternativeOptions("all") Alternative<ObjectId> option) {
		if (option.isAlternative()) {
			String acceptId = new CustomButtonId.Builder()
				.setType(ButtonType.STARBOARD_DELETE_CONFIRM)
				.setOwners(event.getAuthor().getIdLong())
				.setTimeout(60)
				.getId();

			String rejectId = new CustomButtonId.Builder()
				.setType(ButtonType.GENERIC_REJECT)
				.setOwners(event.getAuthor().getIdLong())
				.setTimeout(60)
				.getId();

			List<Button> buttons = List.of(Button.success(acceptId, "Yes"), Button.danger(rejectId, "No"));

			event.reply(event.getAuthor().getName() + ", are you sure you want to delete **all** starboards in this server?")
				.setActionRow(buttons)
				.queue();
		} else {
			ObjectId id = option.getValue();

			AtomicReference<Document> atomicData = new AtomicReference<>();
			event.getMongo().findAndDeleteStarboard(Filters.and(Filters.eq("_id", id), Filters.eq("guildId", event.getGuild().getIdLong()))).thenCompose(data -> {
				if (data == null) {
					return CompletableFuture.completedFuture(null);
				}

				atomicData.set(data);

				return event.getMongo().deleteManyStars(Filters.eq("messageId", data.getLong("originalMessageId")));
			}).whenComplete((result, exception) -> {
				if (ExceptionUtility.sendExceptionally(event, exception)) {
					return;
				}

				if (result == null) {
					event.replyFailure("I could not find that starboard").queue();
					return;
				}

				Document data = atomicData.get();

				WebhookClient<Message> webhook = event.getBot().getStarboardManager().getWebhook(data.getLong("channelId"));
				if (webhook != null) {
					webhook.deleteMessageById(data.getLong("messageId")).queue();
				}

				event.replySuccess("That starboard has been deleted").queue();
			});
		}
	}

	@Command(value="name", description="Set the name of the webhook that sends starboard messages")
	@CommandId(206)
	@Examples({"starboard name Starboard", "starboard name Stars"})
	@Premium
	@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
	public void name(Sx4CommandEvent event, @Argument(value="name", endless=true) String name) {
		event.getMongo().updateGuildById(event.getGuild().getIdLong(), Updates.set("starboard.webhook.name", name)).whenComplete((result, exception) -> {
			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}

			if (result.getModifiedCount() == 0 && result.getUpsertedId() == null) {
				event.replyFailure("Your starboard webhook name was already set to that").queue();
				return;
			}

			event.replySuccess("Your starboard webhook name has been updated, this only works with premium <https://patreon.com/Sx4>").queue();
		});
	}

	@Command(value="avatar", description="Set the avatar of the webhook that sends starboard messages")
	@CommandId(207)
	@Examples({"starboard avatar Shea#6653", "starboard avatar https://i.imgur.com/i87lyNO.png"})
	@Premium
	@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
	public void avatar(Sx4CommandEvent event, @Argument(value="avatar", endless=true, acceptEmpty=true) @ImageUrl String url) {
		event.getMongo().updateGuildById(event.getGuild().getIdLong(), Updates.set("starboard.webhook.avatar", url)).whenComplete((result, exception) -> {
			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}

			if (result.getModifiedCount() == 0 && result.getUpsertedId() == null) {
				event.replyFailure("Your starboard webhook avatar was already set to that").queue();
				return;
			}

			event.replySuccess("Your starboard webhook avatar has been updated, this only works with premium <https://patreon.com/Sx4>").queue();
		});
	}

	@Command(value="top", aliases={"list"}, description="View the top starred messages in the server")
	@CommandId(205)
	@Examples({"starboard top"})
	public void top(Sx4CommandEvent event) {
		Guild guild = event.getGuild();

		List<Document> starboards = event.getMongo().getStarboards(Filters.and(Filters.eq("guildId", guild.getIdLong()), Filters.ne("count", 0)), Projections.include("count", "channelId", "originalMessageId")).sort(Sorts.descending("count")).into(new ArrayList<>());

		MessagePagedResult<Document> paged = new MessagePagedResult.Builder<>(event.getBot(), starboards)
			.setIncreasedIndex(true)
			.setAuthor("Top Starboards", null, guild.getIconUrl())
			.setDisplayFunction(data -> {
				int count = data.getInteger("count");

				return String.format(
					"[%s](https://discord.com/channels/%d/%d/%d) - **%d** star%s",
					data.getObjectId("_id").toHexString(),
					guild.getIdLong(),
					data.getLong("channelId"),
					data.getLong("originalMessageId"),
					count,
					count == 1 ? "" : "s"
				);
			}).build();

		paged.execute(event);
	}

	@Command(value="formatters", aliases={"format", "formatting"}, description="Get all the formatters for starboard you can use")
	@CommandId(444)
	@Examples({"starboard formatters"})
	@BotPermissions(permissions={Permission.MESSAGE_EMBED_LINKS})
	public void formatters(Sx4CommandEvent event) {
		EmbedBuilder embed = new EmbedBuilder()
			.setAuthor("Starboard Formatters", null, event.getSelfUser().getEffectiveAvatarUrl());

		FormatterManager manager = FormatterManager.getDefaultManager();

		StringJoiner content = new StringJoiner("\n");
		for (FormatterVariable<?> variable : manager.getVariables(User.class)) {
			content.add("`{user." + variable.getName() + "}` - " + variable.getDescription());
		}

		for (FormatterVariable<?> variable : manager.getVariables(Member.class)) {
			content.add("`{member." + variable.getName() + "}` - " + variable.getDescription());
		}

		for (FormatterVariable<?> variable : manager.getVariables(Guild.class)) {
			content.add("`{server." + variable.getName() + "}` - " + variable.getDescription());
		}

		for (FormatterVariable<?> variable : manager.getVariables(TextChannel.class)) {
			content.add("`{channel." + variable.getName() + "}` - " + variable.getDescription());
		}

		for (FormatterVariable<?> variable : manager.getVariables(EmojiUnion.class)) {
			content.add("`{emote." + variable.getName() + "}` - " + variable.getDescription());
		}

		content.add("`{stars}` - Gets amount of stars the starboard has");
		content.add("`{stars.next}` - Gets the amount of stars the next milestone requires");
		content.add("`{stars.next.until}` - Gets the amount of stars needed to reach the next milestone");
		content.add("`{id}` - Gets the id for the starboard");

		embed.setDescription(content.toString());

		event.reply(embed.build()).queue();
	}

	public static class MessagesCommand extends Sx4Command {

		public MessagesCommand() {
			super("messages", 200);

			super.setDescription("Set the configuration of messages depending on the amount of stars");
			super.setExamples("starboard messages set", "starboard messages remove", "starboard messages list");
			super.setCategoryAll(ModuleCategory.MANAGEMENT);
		}

		public void onCommand(Sx4CommandEvent event) {
			event.replyHelp().queue();
		}

		@Command(value="set", description="Sets the message for a certain amount of stars")
		@CommandId(201)
		@Examples({"starboard messages set 1 Our first star!", "starboard messages set 20 We reached **{stars}** stars"})
		@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
		public void set(Sx4CommandEvent event, @Argument(value="stars") @Limit(min=1) int stars, @Argument(value="message", endless=true) String message) {
			Document config = new Document("stars", stars)
				.append("message", new Document("content", message));

			List<Bson> update = List.of(Operators.set("starboard.messages", Operators.let(new Document("config", Operators.ifNull("$starboard.messages", StarboardManager.DEFAULT_CONFIGURATION)), Operators.cond(Operators.gte(Operators.size("$$config"), 50), "$$config", Operators.concatArrays(Operators.filter("$$config", Operators.ne("$$this.stars", stars)), List.of(config))))));
			event.getMongo().updateGuildById(event.getGuild().getIdLong(), update).whenComplete((result, exception) -> {
				if (ExceptionUtility.sendExceptionally(event, exception)) {
					return;
				}

				if (result.getModifiedCount() == 0) {
					event.replyFailure("You can have no more than 50 different messages").queue();
					return;
				}

				event.replySuccess("When a starboard reaches **" + stars + "** stars it will now show that message").queue();
			});
		}

		@Command(value="remove", description="Removes a starboard message")
		@CommandId(202)
		@Examples({"starboard messages remove 1", "starboard messages remove 20"})
		@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
		public void remove(Sx4CommandEvent event, @Argument(value="stars") int stars) {
			List<Bson> update = List.of(Operators.set("starboard.messages", Operators.let(new Document("config", Operators.ifNull("$starboard.messages", StarboardManager.DEFAULT_CONFIGURATION)), Operators.cond(Operators.eq(Operators.size("$$config"), 1), "$$config", Operators.let(new Document("updatedConfig", Operators.filter("$$config", Operators.ne("$$this.stars", stars))), Operators.cond(Operators.eq(StarboardManager.DEFAULT_CONFIGURATION, "$$updatedConfig"), Operators.REMOVE, "$$updatedConfig"))))));
			FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().projection(Projections.include("starboard.messages")).returnDocument(ReturnDocument.BEFORE).upsert(true);
			event.getMongo().findAndUpdateGuildById(event.getGuild().getIdLong(), update, options).whenComplete((data, exception) -> {
				if (ExceptionUtility.sendExceptionally(event, exception)) {
					return;
				}

				List<Document> config = data == null ? StarboardManager.DEFAULT_CONFIGURATION : data.getEmbedded(List.of("starboard", "messages"), StarboardManager.DEFAULT_CONFIGURATION);
				if (config.size() == 1) {
					event.replyFailure("You have to have at least 1 starboard message").queue();
					return;
				}

				Document star = config.stream()
					.filter(d -> d.getInteger("stars") == stars)
					.findFirst()
					.orElse(null);

				if (star == null) {
					event.replyFailure("You don't have a starboard message for that amount of stars").queue();
					return;
				}

				event.replySuccess("You no longer have a starboard message for **" + stars + "** stars").queue();
			});
		}

		@Command(value="reset", description="Resets the configuration of your messages to the default")
		@CommandId(203)
		@Examples({"starboard messages reset"})
		@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
		public void reset(Sx4CommandEvent event) {
			event.getMongo().updateGuildById(event.getGuild().getIdLong(), Updates.unset("starboard.messages")).whenComplete((result, exception) -> {
				if (ExceptionUtility.sendExceptionally(event, exception)) {
					return;
				}

				if (result.getModifiedCount() == 0) {
					event.replyFailure("Your messages where already set to the default").queue();
					return;
				}

				event.replySuccess("Your starboard messages are now back to the default").queue();
			});
		}

		@Command(value="list", description="Lists your current starboard message configuration")
		@CommandId(436)
		@Examples({"starboard messages list"})
		public void list(Sx4CommandEvent event) {
			List<Document> messages = event.getMongo().getGuildById(event.getGuild().getIdLong(), Projections.include("starboard.messages")).getEmbedded(List.of("starboard", "messages"), new ArrayList<>(StarboardManager.DEFAULT_CONFIGURATION));
			messages.sort(Comparator.comparingInt(d -> d.getInteger("stars")));

			MessagePagedResult<Document> paged = new MessagePagedResult.Builder<>(event.getBot(), messages)
				.setAuthor("Starboard Messages", null, event.getSelfUser().getEffectiveAvatarUrl())
				.setIndexed(false)
				.setPerPage(10)
				.setSelect()
				.setDisplayFunction(data -> "Star #" + data.getInteger("stars") + ": `" + data.getEmbedded(List.of("message", "content"), String.class) + "`")
				.build();

			paged.execute(event);
		}

	}

}
