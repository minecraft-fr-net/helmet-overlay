package net.minecraftfr.helmetoverlay.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

@Mixin(Gui.class)
public class InGameHudMixin {
  @Inject(method = "extractRenderState", at = @At("HEAD"))
  public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
    Minecraft client = Minecraft.getInstance();

    if (client != null && client.player != null && client.options.getCameraType().isFirstPerson()) {
      ItemStack helmet = getHelmetFromPlayer(client.player);
      if (!helmet.isEmpty()) {
        Identifier identifier = getHelmetIdentifier(helmet);

        if (resourceExists(identifier)) {
          renderOverlay(context, identifier, 1.0F);
        }
      }
    }
  }

  private ItemStack getHelmetFromPlayer(LocalPlayer player) {
    return player.getItemBySlot(EquipmentSlot.HEAD);
  }

  private Identifier getHelmetIdentifier(ItemStack helmet) {
    String helmet_texture_name = BuiltInRegistries.ITEM.getKey(helmet.getItem()).getPath();
    String texture_path = "textures/misc/" + helmet_texture_name + "_overlay.png";
    return Identifier.withDefaultNamespace(texture_path);
  }

  private boolean resourceExists(Identifier identifier) {
    ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
    try {
      Optional<Resource> resource = resourceManager.getResource(identifier);
      return resource.isPresent();
    } catch (Exception e) {
      return false;
    }
  }

  private void renderOverlay(GuiGraphicsExtractor context, Identifier texture, float opacity) {
    int width = context.guiWidth();
    int height = context.guiHeight();
    int alpha = Math.min(255, Math.max(0, (int) (opacity * 255.0F)));
    int color = (alpha << 24) | 0xFFFFFF;
    context.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, width, height, width, height, color);
  }
}
