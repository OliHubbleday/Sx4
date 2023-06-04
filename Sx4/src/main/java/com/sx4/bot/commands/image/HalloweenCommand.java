package com.sx4.bot.commands.image;

import com.jockie.bot.core.argument.Argument;
import com.sx4.bot.annotations.argument.ImageUrl;
import com.sx4.bot.category.ModuleCategory;
import com.sx4.bot.core.Sx4Command;
import com.sx4.bot.core.Sx4CommandEvent;
import com.sx4.bot.entities.image.ImageRequest;
import com.sx4.bot.http.HttpCallback;
import com.sx4.bot.utility.ImageUtility;
import net.dv8tion.jda.api.Permission;
import okhttp3.Request;

public class HalloweenCommand extends Sx4Command {

	public HalloweenCommand() {
		super("halloween", 14);

		super.setDescription("Creates a halloween version of a specific image");
		super.setExamples("halloween", "halloween Shea#6653", "halloween https://example.com/image.png");
		super.setCategoryAll(ModuleCategory.IMAGE);
		super.setBotDiscordPermissions(Permission.MESSAGE_ATTACH_FILES);
		super.setCooldownDuration(5);
	}

	public void onCommand(Sx4CommandEvent event, @Argument(value="image url", endless=true, acceptEmpty=true) @ImageUrl String imageUrl) {
		Request request = new ImageRequest(event.getConfig().getImageWebserverUrl("halloween"))
			.addQuery("image", imageUrl)
			.build(event.getConfig().getImageWebserver());

		event.getHttpClient().newCall(request).enqueue((HttpCallback) response -> ImageUtility.sendImageMessage(event, response).queue());
	}

}
