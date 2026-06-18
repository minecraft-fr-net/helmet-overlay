package net.minecraftfr.helmetoverlay.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

@SuppressWarnings("UnstableApiUsage")
public class HelmetOverlayClientGameTest implements FabricClientGameTest {
	private static final int VIEWPORT_WIDTH = 854;
	private static final int VIEWPORT_HEIGHT = 480;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getClientLevel().waitForChunksDownload();
			sp.getClientLevel().waitForChunksRender();

			sp.getServer().runCommand("item replace entity @p armor.head with minecraft:iron_helmet");

			context.runOnClient((Minecraft client) -> {
				client.options.setCameraType(CameraType.FIRST_PERSON);
			});

			context.waitTicks(20);
			sp.getClientLevel().waitForChunksRender();

			context.assertScreenshotEquals("iron_helmet_overlay");
		}
	}
}
