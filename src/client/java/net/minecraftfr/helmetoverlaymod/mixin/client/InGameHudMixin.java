package net.minecraftfr.helmetoverlaymod.mixin.client;

import java.io.IOException;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Mixin(InGameHud.class)
public class InGameHudMixin {
  @Inject(method = "renderMiscOverlays", at = @At("HEAD"))
  public void renderMiscOverlays(DrawContext context, float tickDelta, CallbackInfo ci) {
    MinecraftClient client = MinecraftClient.getInstance();

    if (client != null && client.player != null && client.options.getPerspective().isFirstPerson()) {
      ItemStack helmet = client.player.getEquippedStack(EquipmentSlot.HEAD);
      if (!helmet.isEmpty()) {
        String helmet_texture_name = Registries.ITEM.getId(helmet.getItem()).getPath();
        String texture_path = "textures/misc/" + helmet_texture_name + "_overlay.png";
        Identifier identifier = new Identifier("minecraft", texture_path);

        if (resourceExists(identifier)) {
          renderOverlay(context, identifier, 1.0F);
        }
      }
    }
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
    RenderSystem.disableDepthTest();
    RenderSystem.depthMask(false);
    RenderSystem.enableBlend();
    context.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
    context.drawTexture(texture, 0, 0, -90, 0.0F, 0.0F, context.getScaledWindowWidth(), context.getScaledWindowHeight(), context.getScaledWindowWidth(), context.getScaledWindowHeight());
    RenderSystem.disableBlend();
    RenderSystem.depthMask(true);
    RenderSystem.enableDepthTest();
    context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
  }
}
