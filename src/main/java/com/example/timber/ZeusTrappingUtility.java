package com.example.timber;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ZeusTrappingUtility implements ModInitializer {
	public static final String MOD_ID = "zeuss_trapping_utility";
	private static final int MAX_LOGS = 128;
	private static final int MAX_TNT_CARTS = 180;
	private static final double REACH = 100.0;

	@Override
	public void onInitialize() {
		registerCommands();
		registerTimber();
	}

	static boolean isModerator(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
	}

	// ---- Tree felling: sneak + axe + break a log

	private static void registerTimber() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel
					&& player instanceof ServerPlayer serverPlayer
					&& serverPlayer.isShiftKeyDown()
					&& !serverPlayer.isCreative()
					&& state.is(BlockTags.LOGS)) {
				ItemStack tool = serverPlayer.getMainHandItem();
				if (tool.is(ItemTags.AXES)) {
					fellConnectedLogs(serverLevel, serverPlayer, pos, tool);
				}
			}
		});
	}

	private static void fellConnectedLogs(ServerLevel level, ServerPlayer player, BlockPos start, ItemStack tool) {
		Set<BlockPos> seen = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();
		seen.add(start);
		queue.add(start);
		int felled = 0;

		while (!queue.isEmpty() && felled < MAX_LOGS) {
			BlockPos current = queue.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = current.offset(dx, dy, dz);
						if (seen.add(next) && level.getBlockState(next).is(BlockTags.LOGS)) {
							if (tool.getDamageValue() >= tool.getMaxDamage() - 1) {
								return;
							}
							level.destroyBlock(next, true, player);
							tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
							queue.add(next);
							if (++felled >= MAX_LOGS) {
								return;
							}
						}
					}
				}
			}
		}
	}

	// ---- Commands

	private static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BotCommand.register(dispatcher);

			dispatcher.register(Commands.literal("tntcarts")
					.requires(ZeusTrappingUtility::isModerator)
					.executes(ctx -> placeTntCarts(ctx.getSource(), 1))
					.then(Commands.argument("amount", IntegerArgumentType.integer(1, MAX_TNT_CARTS))
							.executes(ctx -> placeTntCarts(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))));

			dispatcher.register(Commands.literal("arrows")
					.requires(ZeusTrappingUtility::isModerator)
					.executes(ctx -> fireArrows(ctx.getSource(), false))
					.then(Commands.literal("straight").executes(ctx -> fireArrows(ctx.getSource(), false)))
					.then(Commands.literal("spread").executes(ctx -> fireArrows(ctx.getSource(), true))));
		});
	}

	private static int placeTntCarts(CommandSourceStack source, int amount) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getLevel();

		HitResult hit = player.pick(REACH, 1.0F, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			source.sendFailure(Component.literal("Look at a block within 100 blocks."));
			return 0;
		}

		BlockHitResult blockHit = (BlockHitResult) hit;
		BlockPos spot = blockHit.getBlockPos().relative(blockHit.getDirection());
		double x = spot.getX() + 0.5;
		double y = spot.getY();
		double z = spot.getZ() + 0.5;

		int placed = 0;
		for (int i = 0; i < amount; i++) {
			var cart = EntityType.TNT_MINECART.create(level, EntitySpawnReason.COMMAND);
			if (cart != null) {
				cart.setPos(x, y, z);
				level.addFreshEntity(cart);
				placed++;
			}
		}

		int total = placed;
		source.sendSuccess(() -> Component.literal("Placed " + total + " TNT minecart" + (total == 1 ? "" : "s") + "."), true);
		return placed;
	}

	private static int fireArrows(CommandSourceStack source, boolean spread) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getLevel();

		Vec3 eye = player.getEyePosition();
		double flag = spread ? 1.0 : 0.0;
		double yaw = Math.toRadians(player.getYRot());
		int totalArrows = 50;
		double cx = eye.x + flag * (Math.floor(eye.x) + 0.5 - eye.x);
		double cz = eye.z + flag * (Math.floor(eye.z) + 0.5 - eye.z);

		int fired = 0;
		for (int power = 1; power <= 5; power++) {
			for (int i = 0; i < 10; i++) {
				var arrow = EntityType.ARROW.create(level, EntitySpawnReason.COMMAND);
				if (arrow != null) {
					int k = i * 5 + (power - 1);
					double t = ((double) k / (totalArrows - 1) - 0.5) * 0.9 * flag;
					arrow.setPos(cx - Math.cos(yaw) * t, eye.y - 0.1, cz - Math.sin(yaw) * t);
					arrow.setOwner(player);
					arrow.setBaseDamage(2.0 + 0.5 * power + 0.5);
					arrow.setCritArrow(true);
					arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 0.0F);
					level.addFreshEntity(arrow);
					fired++;
				}
			}
		}

		int total = fired;
		String mode = spread ? "spread across one block" : "straight";
		source.sendSuccess(() -> Component.literal("Fired " + total + " arrows (10 each at Power I-V), " + mode + "."), true);
		return fired;
	}
}
