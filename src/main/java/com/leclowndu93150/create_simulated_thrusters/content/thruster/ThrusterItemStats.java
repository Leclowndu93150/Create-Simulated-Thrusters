package com.leclowndu93150.create_simulated_thrusters.content.thruster;

import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ThrusterItemStats implements TooltipModifier {

    private final Block block;

    private ThrusterItemStats(final Block block) {
        this.block = block;
    }

    @Nullable
    public static ThrusterItemStats create(final Item item) {
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof BlazeThrusterBlock) {
            return new ThrusterItemStats(blockItem.getBlock());
        }
        return null;
    }

    @Override
    public void modify(final ItemTooltipEvent context) {
        if (!(this.block instanceof BlazeThrusterBlock)) {
            return;
        }

        final List<Component> tooltip = context.getToolTip();
        tooltip.add(CommonComponents.EMPTY);
        CreateLang.translate("tooltip.stressImpact")
                .style(ChatFormatting.GRAY)
                .addTo(tooltip);
        CreateLang.builder()
                .add(Component.translatable("create_simulated_thrusters.tooltip.no_su_cost")
                        .withStyle(ChatFormatting.GREEN))
                .addTo(tooltip);
    }
}
