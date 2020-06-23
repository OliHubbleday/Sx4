package com.sx4.bot.commands.settings;

import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.sx4.bot.annotations.command.AuthorPermissions;
import com.sx4.bot.annotations.command.Examples;
import com.sx4.bot.category.Category;
import com.sx4.bot.core.Sx4Command;
import com.sx4.bot.core.Sx4CommandEvent;
import com.sx4.bot.database.model.Operators;
import com.sx4.bot.entities.argument.All;
import com.sx4.bot.utility.ExceptionUtility;
import com.sx4.bot.waiter.Waiter;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class FakePermissionsCommand extends Sx4Command {

	public FakePermissionsCommand() {
		super("fake permissions");
		
		super.setDescription("Setup permissions for user or roles which only work within the bot");
		super.setAliases("fakepermissions", "fake perms", "fakeperms");
		super.setExamples("fake permissions add", "fake permissions remove", "fake permissions list");
		super.setCategory(Category.SETTINGS);
	}
	
	public void onCommand(Sx4CommandEvent event) {
		event.replyHelp().queue();
	}
	
	@Command(value="add", description="Adds permissions to a user or role within the bot")
	@Examples({"fake permissions add @Shea#6653 message_manage", "fake permissions add @Mods kick_members ban_members"})
	@AuthorPermissions(permissions={Permission.ADMINISTRATOR})
	public void add(Sx4CommandEvent event, @Argument(value="user | role") IPermissionHolder holder, @Argument(value="permissions") Permission... permissions) {
		long rawPermissions = Permission.getRaw(permissions);
		boolean role = holder instanceof Role;
		
		Document data = new Document("id", holder.getIdLong())
			.append("type", role ? 1 : 0)
			.append("permissions", rawPermissions);
		
		Bson filter = Operators.filter("$fakePermissions.holders", Operators.eq("$$this.id", holder.getIdLong()));
		List<Bson> update = List.of(Operators.set("fakePermissions.holders", Operators.cond(Operators.extinct("$fakePermissions.holders"), List.of(data), Operators.cond(Operators.isEmpty(filter), Operators.concatArrays("$fakePermissions.holders", List.of(data)), Operators.concatArrays(Operators.filter("$fakePermissions.holders", Operators.ne("$$this.id", holder.getIdLong())), List.of(new Document(data).append("permissions", Operators.toLong(Operators.bitwiseOr(Operators.first(Operators.map(filter, "$$this.permissions")), rawPermissions)))))))));
		this.database.updateGuildById(event.getGuild().getIdLong(), update).whenComplete((result, exception) -> {
			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}
			
			if (result.getModifiedCount() == 0) {
				event.reply("That " + (role ? "role" : "user") + " already has all those permissions " + this.config.getFailureEmote()).queue();
				return;
			}
			
			event.reply((role ? ((Role) holder).getAsMention() : "**" + ((Member) holder).getUser().getAsTag() + "**") + " now has those permissions " + this.config.getSuccessEmote()).queue();
		});
	}
	
	@Command(value="remove", description="Removes permissions from a user or role within the bot")
	@Examples({"fake permissions remove @Shea#6653 message_manage", "fake permissions remove @Mods kick_members ban_members", "fake permissions remove @Mods all"})
	@AuthorPermissions(permissions={Permission.ADMINISTRATOR})
	public void remove(Sx4CommandEvent event, @Argument(value="user | role") IPermissionHolder holder, @Argument(value="permissions") Permission... permissions) {
		long rawPermissions = Permission.getRaw(permissions);
		boolean role = holder instanceof Role;
		
		Document data = new Document("id", holder.getIdLong())
			.append("type", role ? 1 : 0);

		Bson filter = Operators.filter("$fakePermissions.holders", Operators.eq("$$this.id", holder.getIdLong()));
		Bson withoutHolder = Operators.filter("$fakePermissions.holders", Operators.ne("$$this.id", holder.getIdLong()));
		Bson newPermissions = Operators.toLong(Operators.bitwiseAnd(Operators.first(Operators.map(filter, "$$this.permissions")), ~rawPermissions));
		
		List<Bson> update = List.of(Operators.set("fakePermissions.holders", Operators.cond(Operators.or(Operators.extinct("$fakePermissions.holders"), Operators.isEmpty(filter)), "$fakePermissions.holders", Operators.cond(Operators.eq(newPermissions, 0L), withoutHolder, Operators.concatArrays(withoutHolder, List.of(data.append("permissions", newPermissions)))))));
		this.database.updateGuildById(event.getGuild().getIdLong(), update).whenComplete((result, exception) -> {
			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}
			
			if (result.getModifiedCount() == 0) {
				event.reply("That " + (role ? "role" : "user") + " doesn't have any of those permissions " + this.config.getFailureEmote()).queue();
				return;
			}
			
			event.reply((role ? ((Role) holder).getAsMention() : "**" + ((Member) holder).getUser().getAsTag() + "**") + " no longer has those permissions " + this.config.getSuccessEmote()).queue();
		});
	}
	
	@Command(value="delete", description="Deletes fake permissions for a user or role")
	@Examples({"fake permissions delete @Shea#6653", "fake permissions delete @Mods", "fake permissions delete all"})
	@AuthorPermissions(permissions={Permission.ADMINISTRATOR})
	public void delete(Sx4CommandEvent event, @Argument(value="user | role | all", endless=true) All<IPermissionHolder> all) {
		if (all.isAll()) {
			event.reply(event.getAuthor().getName() + ", are you sure you want to delete **all** fake permissions data? (Yes or No)").queue($ -> {
				Waiter<GuildMessageReceivedEvent> waiter = new Waiter<>(GuildMessageReceivedEvent.class)
					.setPredicate(messageEvent -> messageEvent.getAuthor().getIdLong() == event.getAuthor().getIdLong() && messageEvent.getChannel().getIdLong() == event.getChannel().getIdLong() && messageEvent.getMessage().getContentRaw().equalsIgnoreCase("yes"))
					.setCancelPredicate(messageEvent -> messageEvent.getAuthor().getIdLong() == event.getAuthor().getIdLong() && messageEvent.getChannel().getIdLong() == event.getChannel().getIdLong() && !messageEvent.getMessage().getContentRaw().equalsIgnoreCase("yes"))
					.setTimeout(30);
					
				waiter.onTimeout(() -> event.reply("Response timed out :stopwatch:").queue());
				
				waiter.onCancelled(() -> event.reply("Cancelled " + this.config.getSuccessEmote()).queue());
				
				waiter.future()
					.thenCompose(messageEvent -> this.database.updateGuildById(event.getGuild().getIdLong(), Updates.unset("fakePermissions.holders")))
					.whenComplete((result, exception) -> {
						if (ExceptionUtility.sendExceptionally(event, exception)) {
							return;
						}
						
						event.reply("All fake permission data has been deleted in this server " + this.config.getSuccessEmote()).queue();
					});
				
				waiter.start();
			});
		} else {
			IPermissionHolder holder = all.getValue();
			boolean role = holder instanceof Role;
			
			this.database.updateGuildById(event.getGuild().getIdLong(), Updates.pull("fakePermissions.holders", Filters.eq("id", holder.getIdLong()))).whenComplete((result, exception) -> {
				if (ExceptionUtility.sendExceptionally(event, exception)) {
					return;
				}
				
				if (result.getModifiedCount() == 0) {
					event.reply("That " + (role ? "role" : "user") + " doesn't have any fake permissions " + this.config.getFailureEmote()).queue();
					return;
				}
				
				event.reply((role ? ((Role) holder).getAsMention() : "**" + ((Member) holder).getUser().getAsTag() + "**") + " no longer has any fake permissions " + this.config.getSuccessEmote()).queue();
			});
		}
	}
	
}