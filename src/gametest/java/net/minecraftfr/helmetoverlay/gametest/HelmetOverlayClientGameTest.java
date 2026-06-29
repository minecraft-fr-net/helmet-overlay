package net.minecraftfr.helmetoverlay.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

@SuppressWarnings("UnstableApiUsage")
public class HelmetOverlayClientGameTest implements FabricClientGameTest {
	private static final int VIEWPORT_WIDTH  = 854;
	private static final int VIEWPORT_HEIGHT = 480;

	private static final String[][] HELMETS = {
		{"iron_helmet",      "iron_helmet_overlay"},
		{"diamond_helmet",   "diamond_helmet_overlay"},
		{"golden_helmet",    "golden_helmet_overlay"},
		{"netherite_helmet", "netherite_helmet_overlay"},
		{"chainmail_helmet", "chainmail_helmet_overlay"},
		{"leather_helmet",   "leather_helmet_overlay"},
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getClientLevel().waitForChunksDownload();
			sp.getClientLevel().waitForChunksRender();

			sp.getServer().runCommand("gamerule announceAdvancements false");
			sp.getServer().runCommand("gamerule sendCommandFeedback false");

			context.runOnClient((Minecraft client) ->
				client.options.setCameraType(CameraType.FIRST_PERSON)
			);

			for (String[] helmet : HELMETS) {
				String item     = helmet[0];
				String template = helmet[1];

				sp.getServer().runCommand("item replace entity @p armor.head with minecraft:" + item);
				context.waitTicks(20);
				sp.getClientLevel().waitForChunksRender();

				context.runOnClient((Minecraft client) -> {
					client.gui.getChat().clearMessages(false);
					client.getToastManager().clear();
				});
				context.waitTicks(2);

				context.assertScreenshotEquals(template);
			}
		}
	}
}
