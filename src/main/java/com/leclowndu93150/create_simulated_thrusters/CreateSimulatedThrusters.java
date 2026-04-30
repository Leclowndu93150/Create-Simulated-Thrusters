package com.leclowndu93150.create_simulated_thrusters;

import com.leclowndu93150.create_simulated_thrusters.content.thruster.RedstoneThrusterBlock;
import com.leclowndu93150.create_simulated_thrusters.content.thruster.RedstoneThrusterBlockEntity;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

@Mod(CreateSimulatedThrusters.MODID)
public class CreateSimulatedThrusters {
    public static final String MODID = "create_simulated_thrusters";

    public static final ResourceKey<CreativeModeTab> CREATIVE_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MODID, "main"));

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID).defaultCreativeTab(CREATIVE_TAB_KEY);

    public static final BlockEntry<RedstoneThrusterBlock> REDSTONE_THRUSTER =
            REGISTRATE.block("redstone_thruster", RedstoneThrusterBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.sound(SoundType.METAL))
                    .transform(axeOrPickaxe())
                    .blockstate((ctx, prov) -> {})
                    .item()
                    .model((ctx, prov) -> {})
                    .build()
                    .register();

    public static final BlockEntityEntry<RedstoneThrusterBlockEntity> REDSTONE_THRUSTER_BE = REGISTRATE
            .blockEntity("redstone_thruster", RedstoneThrusterBlockEntity::new)
            .validBlocks(REDSTONE_THRUSTER)
            .register();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(REDSTONE_THRUSTER.get()))
                    .build()
    );

    public CreateSimulatedThrusters(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(REDSTONE_THRUSTER.get(), () -> 6.0));
    }
}
