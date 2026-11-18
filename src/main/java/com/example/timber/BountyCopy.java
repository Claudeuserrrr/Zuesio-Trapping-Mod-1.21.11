package com.example.timber;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Optional;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Hover a player head in a menu and press the key (default: right click) to copy the player's name. */
public final class BountyCopy {
	private static KeyMapping copyKey;
	private static boolean inMenu;

	private BountyCopy() {
	}

	public static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(ZeusTrappingUtility.MOD_ID, "main"));
		copyKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.zeuss_trapping_utility.copy_bounty_name", InputConstants.Type.MOUSE, 1, category));

		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (inMenu && stack.is(Items.PLAYER_HEAD)) {
				lines.add(Component.literal("Press ")
						.append(copyKey.getTranslatedKeyMessage())
						.append(" to copy name")
						.withStyle(ChatFormatting.GRAY));
			}
		});

		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof AbstractContainerScreen<?> container) {
				inMenu = true;
				ScreenEvents.remove(screen).register(s -> inMenu = false);
				ScreenKeyboardEvents.beforeKeyPress(screen).register((s, keyEvent) -> {
					if (copyKey.matches(keyEvent)) {
						copyHoveredName(client, container);
					}
				});
				ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
						!(copyKey.matchesMouse(event) && copyHoveredName(client, container)));
			}
		});
	}

	private static boolean copyHoveredName(Minecraft client, AbstractContainerScreen<?> screen) {
		if (client.player == null) {
			return false;
		}
		Slot slot = screen.hoveredSlot; // made accessible by the access widener
		if (slot == null) {
			return false;
		}
		ItemStack stack = slot.getItem();
		if (stack.isEmpty() || !stack.is(Items.PLAYER_HEAD)) {
			return false;
		}
		String name = playerName(stack);
		if (name.isEmpty()) {
			return false;
		}
		client.keyboardHandler.setClipboard(name);
		client.player.sendSystemMessage(Component.literal("Copied: " + name));
		return true;
	}

	private static String playerName(ItemStack stack) {
		var profile = stack.get(DataComponents.PROFILE);
		if (profile != null) {
			Object value = profile.name();
			if (value instanceof Optional<?> opt) {
				value = opt.orElse(null);
			}
			if (value instanceof String s && !s.isBlank()) {
				return s.trim();
			}
		}

		String name = stack.getHoverName().getString().trim();
		for (String suffix : new String[] {"'s Head", "\u2019s Head"}) {
			if (name.endsWith(suffix)) {
				name = name.substring(0, name.length() - suffix.length()).trim();
			}
		}
		return name;
	}
}
