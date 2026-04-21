package net.turbonautics.init;

import net.turbonautics.TurbonauticsMod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@EventBusSubscriber(modid = TurbonauticsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class TurbonauticsSimulatedTab {
	private static final ResourceLocation SECTION_ID = ResourceLocation.fromNamespaceAndPath(TurbonauticsMod.MODID, "turbonautics");
	private static boolean initialized;

	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(TurbonauticsSimulatedTab::init);
	}

	private static void init() {
		if (initialized || !ModList.get().isLoaded("simulated")) {
			return;
		}

		initialized = true;
		try {
			Class<?> registrateClass = Class.forName("dev.simulated_team.simulated.registrate.SimulatedRegistrate");
			Field tabItemsField = registrateClass.getField("TAB_ITEMS");
			Field itemToSectionField = registrateClass.getField("ITEM_TO_SECTION");
			@SuppressWarnings("unchecked")
			List<Supplier<Item>> tabItems = (List<Supplier<Item>>) tabItemsField.get(null);
			@SuppressWarnings("unchecked")
			Map<ResourceLocation, ResourceLocation> itemToSection = (Map<ResourceLocation, ResourceLocation>) itemToSectionField.get(null);
			addItem(tabItems, itemToSection, TurbonauticsModItems.BLAZE_AFTERBURNER.getId());
			addItem(tabItems, itemToSection, TurbonauticsModItems.INTAKE.getId());
			addItem(tabItems, itemToSection, TurbonauticsModItems.JET_FUEL_BUCKET.getId());
		} catch (ReflectiveOperationException ignored) {
		}
	}

	private static void addItem(List<Supplier<Item>> tabItems, Map<ResourceLocation, ResourceLocation> itemToSection, ResourceLocation itemId) {
		tabItems.add(() -> net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId));
		itemToSection.put(itemId, SECTION_ID);
	}
}
