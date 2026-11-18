package com.example.timber;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shows how many arrows / TNT minecarts are near whatever you are looking at. */
public final class EntityCounterHud {
	private static final float SCALE = 1.75F;
	private static final float ICON_SCALE = 0.5625F;
	private static final int X = 4;
	private static final int TEXT_X = 12;
	private static final int ROW = 12;
	private static final double RANGE = 200.0;
	private static final double NEAR = 1.5;

	private static ItemStack arrowIcon;
	private static ItemStack cartIcon;

	private EntityCounterHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.options.hideGui) {
			return;
		}

		HitResult hit = mc.hitResult;
		if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
			hit = mc.player.pick(RANGE, 1.0F, false);
		}
		if (hit == null || hit.getType() == HitResult.Type.MISS) {
			return;
		}
		Vec3 target = hit.getLocation();

		int arrows = 0;
		int carts = 0;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!entity.isAlive()) {
				continue;
			}
			EntityType<?> type = entity.getType();
			boolean isArrow = type == EntityType.ARROW;
			if (!isArrow && type != EntityType.TNT_MINECART) {
				continue;
			}
			if (Math.abs(entity.getX() - target.x) > NEAR
					|| Math.abs(entity.getY() - target.y) > NEAR
					|| Math.abs(entity.getZ() - target.z) > NEAR) {
				continue;
			}
			if (isArrow) {
				arrows++;
			} else {
				carts++;
			}
		}
		if (arrows == 0 && carts == 0) {
			return;
		}

		if (arrowIcon == null) {
			arrowIcon = new ItemStack(Items.ARROW);
			cartIcon = new ItemStack(Items.TNT_MINECART);
		}

		int rows = (arrows > 0 ? 1 : 0) + (carts > 0 ? 1 : 0);
		float top = mc.getWindow().getGuiScaledHeight() - 30 - ((rows - 1) * ROW + 9) * SCALE;

		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate((float) X, top);
		pose.scale(SCALE, SCALE);
		int y = 0;
		if (arrows > 0) {
			row(graphics, mc, arrowIcon, arrows, y);
			y += ROW;
		}
		if (carts > 0) {
			row(graphics, mc, cartIcon, carts, y);
		}
		pose.popMatrix();
	}

	private static void row(GuiGraphics graphics, Minecraft mc, ItemStack icon, int count, int y) {
		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(0.0F, (float) y);
		pose.scale(ICON_SCALE, ICON_SCALE);
		graphics.renderItem(icon, 0, 0);
		pose.popMatrix();
		graphics.drawString(mc.font, String.valueOf(count), TEXT_X, y, -1, true);
	}
}
