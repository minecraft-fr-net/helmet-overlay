package net.minecraftfr.helmetoverlay.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Mixin(InGameHud.class)
public class InGameHudMixin {
  @Inject(method = "renderMiscOverlays", at = @At("HEAD"))
  public void renderMiscOverlays(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
    MinecraftClient client = MinecraftClient.getInstance();

    if (client != null && client.player != null && client.options.getPerspective().isFirstPerson()) {
      ItemStack helmet = getHelmetFromPlayer(client.player);
      if (!helmet.isEmpty()) {
        Identifier identifier = getHelmeIdentifier(helmet);

        if (resourceExists(identifier)) {
          renderOverlay(context, identifier, 1.0F);
        }
      }
    }
  }

  private ItemStack getHelmetFromPlayer(ClientPlayerEntity player) {
    return player.getEquippedStack(EquipmentSlot.HEAD);
  }

  private Identifier getHelmeIdentifier(ItemStack helmet) {
    String helmet_texture_name = Registries.ITEM.getId(helmet.getItem()).getPath();
    String texture_path = "textures/misc/" + helmet_texture_name + "_overlay.png";
    Identifier identifier = Identifier.ofVanilla(texture_path);

    return identifier;
  }

  private boolean resourceExists(Identifier identifier) {
    ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
    try {
      Optional<Resource> resource = resourceManager.getResource(identifier);
      return resource.isPresent();
    } catch (Exception e) {
      return false;
    }
  }

  private void renderOverlay(DrawContext context, Identifier texture, float opacity) {
    int width = context.getScaledWindowWidth();
    int height = context.getScaledWindowHeight();
    int alpha = Math.min(255, Math.max(0, (int) (opacity * 255.0F)));
    int color = (alpha << 24) | 0xFFFFFF;
    context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, width, height, width, height, color);
  }
}
