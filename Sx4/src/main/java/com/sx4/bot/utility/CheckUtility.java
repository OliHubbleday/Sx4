package com.sx4.bot.utility;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;

import com.mongodb.client.model.Projections;
import com.sx4.bot.core.Sx4Bot;
import com.sx4.bot.database.Database;
import com.sx4.bot.entities.settings.HolderType;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class CheckUtility {
	
	public static boolean hasPermissions(Member member, TextChannel channel, Permission... permissions) {
		return CheckUtility.hasPermissions(member, channel, permissions.length == 0 ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(Arrays.asList(permissions)));
	}

	public static boolean hasPermissions(Member member, TextChannel channel, EnumSet<Permission> permissions) {
		if (Sx4Bot.getCommandListener().isDeveloper(member.getIdLong()) || member.hasPermission(channel, permissions)) {
			return true;
		}
		
		List<Document> fakePermissions = Database.get().getGuildById(member.getGuild().getIdLong(), Projections.include("fakePermissions.holders")).getEmbedded(List.of("fakePermissions", "holders"), Collections.emptyList());
		
		Set<Long> roleIds = member.getRoles().stream()
			.map(Role::getIdLong)
			.collect(Collectors.toSet());
		
		long permissionsRaw = Permission.getRaw(member.getPermissions(channel)), permissionsNeededRaw = Permission.getRaw(permissions);
		for (Document fakePermission : fakePermissions) {
			long id = fakePermission.get("id", 0L);
			int type = fakePermission.get("type", 0);
			
			if (type == HolderType.ROLE.getType() && roleIds.contains(id)) {
				permissionsRaw |= fakePermission.get("permissions", 0L);
			} else if (type == HolderType.USER.getType() && member.getIdLong() == id) {
				permissionsRaw |= fakePermission.get("permissions", 0L);
			}
		}
		
		return (permissionsNeededRaw & permissionsRaw) == permissionsNeededRaw;
	}
	
}