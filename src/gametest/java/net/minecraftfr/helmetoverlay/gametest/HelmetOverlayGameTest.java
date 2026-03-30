package net.minecraftfr.helmetoverlay.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

public class HelmetOverlayGameTest implements FabricGameTest {
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void emptyStructureLoads(TestContext context) {
		context.expectBlock(Blocks.AIR, 0, 0, 0);
		context.complete();
	}
}
