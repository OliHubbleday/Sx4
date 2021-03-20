package com.sx4.bot.commands.mod;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.sx4.bot.annotations.argument.Limit;
import com.sx4.bot.annotations.argument.Options;
import com.sx4.bot.annotations.command.AuthorPermissions;
import com.sx4.bot.annotations.command.BotPermissions;
import com.sx4.bot.annotations.command.CommandId;
import com.sx4.bot.annotations.command.Examples;
import com.sx4.bot.core.Sx4Command;
import com.sx4.bot.core.Sx4CommandEvent;
import com.sx4.bot.entities.argument.Alternative;
import com.sx4.bot.paged.PagedResult;
import com.sx4.bot.utility.ExceptionUtility;
import com.sx4.bot.waiter.Waiter;
import com.sx4.bot.waiter.exception.CancelException;
import com.sx4.bot.waiter.exception.TimeoutException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

public class TemplateCommand extends Sx4Command {

	public TemplateCommand() {
		super("template", 254);

		super.setDescription("Setup templates to be used as shortcuts in moderation reasons");
		super.setAliases("templates");
		super.setExamples("template add", "template remove", "template list");
	}

	public void onCommand(Sx4CommandEvent event) {
		event.replyHelp().queue();
	}

	@Command(value="add", description="Add a template in the current server")
	@CommandId(255)
	@Examples({"template add tos Broke ToS", "template add spam Spamming excessively"})
	@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
	public void add(Sx4CommandEvent event, @Argument(value="template") @Limit(max=100) String template, @Argument(value="reason", endless=true) String reason) {
		Document data = new Document("template", template)
			.append("reason", reason)
			.append("guildId", event.getGuild().getIdLong());

		event.getDatabase().insertTemplate(data).whenComplete((result, exception) -> {
			Throwable cause = exception == null ? null : exception.getCause();
			if (cause instanceof MongoWriteException && ((MongoWriteException) cause).getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
				event.replyFailure("You already have a template with that name").queue();
				return;
			}

			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}

			event.replySuccess("That template has been added with id `" + result.getInsertedId().asObjectId().getValue().toHexString() + "`").queue();
		});
	}

	@Command(value="delete", aliases={"remove"}, description="Deletes a template from the current server")
	@CommandId(256)
	@Examples({"template delete 6006ff6b94c9ed0f764ada83", "template delete all"})
	@AuthorPermissions(permissions={Permission.MANAGE_SERVER})
	public void delete(Sx4CommandEvent event, @Argument(value="id | all") @Options("all") Alternative<ObjectId> option) {
		if (option.isAlternative()) {
			event.reply(event.getAuthor().getName() + ", are you sure you want to delete **all** the templates in this server? (Yes or No)").submit()
				.thenCompose(message -> {
					return new Waiter<>(event.getBot(), MessageReceivedEvent.class)
						.setPredicate(messageEvent -> messageEvent.getMessage().getContentRaw().equalsIgnoreCase("yes"))
						.setOppositeCancelPredicate()
						.setTimeout(30)
						.setUnique(event.getAuthor().getIdLong(), event.getChannel().getIdLong())
						.start();
				})
				.thenCompose(messageEvent -> event.getDatabase().deleteManyTemplates(Filters.eq("guildId", event.getGuild().getIdLong())))
				.whenComplete((result, exception) -> {
					Throwable cause = exception instanceof CompletionException ? exception.getCause() : exception;
					if (cause instanceof CancelException) {
						event.replySuccess("Cancelled").queue();
						return;
					} else if (cause instanceof TimeoutException) {
						event.reply("Timed out :stopwatch:").queue();
						return;
					} else if (ExceptionUtility.sendExceptionally(event, cause)) {
						return;
					}

					if (result.getDeletedCount() == 0) {
						event.replySuccess("There are no templates in this server").queue();
						return;
					}

					event.replySuccess("All templates have been deleted in this server").queue();
				});
		} else {
			event.getDatabase().deleteTemplateById(option.getValue()).whenComplete((result, exception) -> {
				if (ExceptionUtility.sendExceptionally(event, exception)) {
					return;
				}

				if (result.getDeletedCount() == 0) {
					event.replyFailure("I could not find that template").queue();
					return;
				}

				event.replySuccess("That template has been deleted").queue();
			});
		}
	}

	@Command(value="list", description="Lists all the templates in the current server")
	@CommandId(257)
	@Examples({"template list"})
	@BotPermissions(permissions={Permission.MESSAGE_EMBED_LINKS})
	public void list(Sx4CommandEvent event) {
		List<Document> triggers = event.getDatabase().getTemplates(Filters.eq("guildId", event.getGuild().getIdLong()), Projections.include("template", "reason")).into(new ArrayList<>());
		if (triggers.isEmpty()) {
			event.replyFailure("There are no templates setup in this server").queue();
			return;
		}

		PagedResult<Document> paged = new PagedResult<>(event.getBot(), triggers)
			.setAuthor("Templates", null, event.getGuild().getIconUrl())
			.setDisplayFunction(data -> "`" + data.getObjectId("_id").toHexString() + "` - " + data.getString("template"));

		paged.onSelect(select -> event.reply(select.getSelected().getString("reason")).queue());

		paged.execute(event);
	}

}