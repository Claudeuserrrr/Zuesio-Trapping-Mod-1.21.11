package com.example.timber;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /bot - spawns a killable, fully geared mannequin that looks like you.
 * Uses the vanilla /summon command so it does not depend on any private game code.
 */
public final class BotCommand {
	private BotCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("bot")
				.requires(ZeusTrappingUtility::isModerator)
				.executes(ctx -> spawnBot(ctx.getSource())));
	}

	private static int spawnBot(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String name = player.getName().getString();
		if (!name.matches("[A-Za-z0-9_.\\-]{1,32}")) {
			source.sendFailure(Component.literal("Could not create the bot (unsupported player name)."));
			return 0;
		}

		String command = String.format(Locale.ROOT,
				"summon minecraft:mannequin %.4f %.4f %.4f "
						+ "{profile:\"%s\",CustomName:\"%s\",CustomNameVisible:1b,hide_description:1b,Invulnerable:0b,"
						+ "Rotation:[%.2ff,0.0f],"
						+ "equipment:{"
						+ "head:{id:\"minecraft:netherite_helmet\",count:1},"
						+ "chest:{id:\"minecraft:netherite_chestplate\",count:1},"
						+ "legs:{id:\"minecraft:netherite_leggings\",count:1},"
						+ "feet:{id:\"minecraft:netherite_boots\",count:1},"
						+ "mainhand:{id:\"minecraft:totem_of_undying\",count:1},"
						+ "offhand:{id:\"minecraft:totem_of_undying\",count:1}}}",
				player.getX(), player.getY(), player.getZ(), name, name, player.getYRot());

		source.getServer().getCommands().performPrefixedCommand(source, command);
		return 1;
	}
}
