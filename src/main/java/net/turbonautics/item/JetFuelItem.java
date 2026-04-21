package net.turbonautics.item;

import net.turbonautics.init.TurbonauticsModFluids;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;

public class JetFuelItem extends BucketItem {
	public JetFuelItem() {
		super(TurbonauticsModFluids.JET_FUEL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)

		);
	}
}