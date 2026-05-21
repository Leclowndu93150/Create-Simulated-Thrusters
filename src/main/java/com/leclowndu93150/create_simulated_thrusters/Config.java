package com.leclowndu93150.create_simulated_thrusters;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = CreateSimulatedThrusters.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue REDSTONE_THRUSTER_THRUST = BUILDER
            .comment("Maximum thrust of the redstone thruster (at full power and speed)")
            .defineInRange("redstoneThrusterThrust", 600.0, 0.0, 10000.0);

    private static final ModConfigSpec.DoubleValue REDSTONE_THRUSTER_AIRFLOW = BUILDER
            .comment("Maximum airflow of the redstone thruster (at full power and speed)")
            .defineInRange("redstoneThrusterAirflow", 1.0, 0.0, 1.0E6);

    private static final ModConfigSpec.DoubleValue REDSTONE_THRUSTER_MAX_THRUST_RPM = BUILDER
            .comment("Kinetic speed in RPM where the redstone thruster reaches full configured thrust")
            .defineInRange("redstoneThrusterMaxThrustRpm", 256.0, 1.0, 4096.0);

    private static final ModConfigSpec.DoubleValue BLAZE_THRUSTER_LOW_THRUST = BUILDER
            .comment("Thrust of the blaze thruster on low-grade fuel (sticks etc.)")
            .defineInRange("blazeThrusterLowThrust", 80.0, 0.0, 100000.0);

    private static final ModConfigSpec.DoubleValue BLAZE_THRUSTER_HIGH_THRUST = BUILDER
            .comment("Thrust of the blaze thruster on normal fuel (coal, logs etc.)")
            .defineInRange("blazeThrusterHighThrust", 700.0, 0.0, 100000.0);

    private static final ModConfigSpec.DoubleValue BLAZE_THRUSTER_BLUE_THRUST = BUILDER
            .comment("Thrust of the blaze thruster on superheated fuel (blaze cake)")
            .defineInRange("blazeThrusterBlueThrust", 1500.0, 0.0, 100000.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double redstoneThrusterThrust;
    public static double redstoneThrusterAirflow;
    public static double redstoneThrusterMaxThrustRpm;
    public static double blazeThrusterLowThrust;
    public static double blazeThrusterHighThrust;
    public static double blazeThrusterBlueThrust;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        redstoneThrusterThrust = REDSTONE_THRUSTER_THRUST.get();
        redstoneThrusterAirflow = REDSTONE_THRUSTER_AIRFLOW.get();
        redstoneThrusterMaxThrustRpm = REDSTONE_THRUSTER_MAX_THRUST_RPM.get();
        blazeThrusterLowThrust = BLAZE_THRUSTER_LOW_THRUST.get();
        blazeThrusterHighThrust = BLAZE_THRUSTER_HIGH_THRUST.get();
        blazeThrusterBlueThrust = BLAZE_THRUSTER_BLUE_THRUST.get();
    }
}
