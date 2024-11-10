package dev.fileeditor.votl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import dev.fileeditor.votl.App;
import dev.fileeditor.votl.objects.CaseType;
import dev.fileeditor.votl.utils.database.DBUtil;
import dev.fileeditor.votl.utils.logs.GuildLogger;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class GroupHelper {

	private final JDA JDA;
	private final GuildLogger logger;
	private final DBUtil db;

	public GroupHelper(App bot) {
		this.JDA = bot.JDA;
		this.logger = bot.getLogger();
		this.db = bot.getDBUtil();
	}

	private void banUser(int groupId, Guild executedGuild, User user, String reason) {
		final List<Long> guildIds = new ArrayList<>();
		for (long guildId : db.group.getGroupManagers(groupId)) {
			for (int subGroupId : db.group.getOwnedGroups(guildId)) {
				guildIds.addAll(db.group.getGroupMembers(subGroupId));
			}
		}
		guildIds.addAll(db.group.getGroupMembers(groupId));
		if (guildIds.isEmpty()) return;

		final int maxCount = guildIds.size();
		final List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
		final String newReason = "Sync #"+groupId+": "+reason;
		for (long guildId : guildIds) {
			final Guild guild = JDA.getGuildById(guildId);
			if (guild == null) continue;
			// fail-safe check if the user has temporal ban (to prevent auto unban)
			db.cases.setInactiveByType(user.getIdLong(), guildId, CaseType.BAN);

			completableFutures.add(guild.ban(user, 0, TimeUnit.SECONDS).reason(newReason).submit());
		}

		CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
			.whenComplete((done, exception) -> {
				int banned = 0;
				for (CompletableFuture<Void> future : completableFutures) {
					if (!future.isCompletedExceptionally()) banned++;
				}
				// Log in server where 
				logger.mod.onHelperSyncBan(groupId, executedGuild, user, reason, banned, maxCount);
			});
	}

	private void unbanUser(int groupId, Guild master, User user, String reason) {
		final List<Long> guildIds = new ArrayList<>();
		for (long guildId : db.group.getGroupManagers(groupId)) {
			for (int subGroupId : db.group.getOwnedGroups(guildId)) {
				guildIds.addAll(db.group.getGroupMembers(subGroupId));
			}
		}
		guildIds.addAll(db.group.getGroupMembers(groupId));
		if (guildIds.isEmpty()) return;

		final int maxCount = guildIds.size();
		final List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
		final String newReason = "Sync #"+groupId+": "+reason;
		for (long guildId : guildIds) {
			final Guild guild = JDA.getGuildById(guildId);
			if (guild == null) continue;
			// Remove temporal ban case
			db.cases.setInactiveByType(user.getIdLong(), guildId, CaseType.BAN);

			completableFutures.add(guild.unban(user).reason(newReason).submit());
		}

		CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
			.whenComplete((done, exception) -> {
				int unbanned = 0;
				for (CompletableFuture<Void> future : completableFutures) {
					if (!future.isCompletedExceptionally()) unbanned++;
				}
				logger.mod.onHelperSyncUnban(groupId, master, user, reason, unbanned, maxCount);
			});
	}

	private void kickUser(int groupId, Guild master, User user, String reason) {
		final List<Long> guildIds = new ArrayList<>();
		for (long guildId : db.group.getGroupManagers(groupId)) {
			for (int subGroupId : db.group.getOwnedGroups(guildId)) {
				guildIds.addAll(db.group.getGroupMembers(subGroupId));
			}
		}
		guildIds.addAll(db.group.getGroupMembers(groupId));
		if (guildIds.isEmpty()) return;

		final int maxCount = guildIds.size();
		final List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
		final String newReason = "Sync #"+groupId+": "+reason;
		for (long guildId : guildIds) {
			final Guild guild = JDA.getGuildById(guildId);
			if (guild == null) continue;

			completableFutures.add(guild.kick(user).reason(newReason).submit());
		}

		CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
			.whenComplete((done, exception) -> {
				int kicked = 0;
				for (CompletableFuture<Void> future : completableFutures) {
					if (!future.isCompletedExceptionally()) kicked++;
				}
				logger.mod.onHelperSyncKick(groupId, master, user, reason, kicked, maxCount);
			});
	}

	public void runBan(int groupId, Guild executedGuild, User user, String reason) {
		CompletableFuture.runAsync(() -> {
			banUser(groupId, executedGuild, user, Optional.ofNullable(reason).orElse("none"));
		});
	}

	public void runUnban(int groupId, Guild master, User user, String reason) {
		CompletableFuture.runAsync(() -> {
			unbanUser(groupId, master, user, Optional.ofNullable(reason).orElse("none"));
		});
	}

	public void runKick(int groupId, Guild master, User user, String reason) {
		CompletableFuture.runAsync(() -> {
			kickUser(groupId, master, user, Optional.ofNullable(reason).orElse("none"));
		});
	}
}
