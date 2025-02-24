package minicraft.screen;

import minicraft.core.io.ControllerHandler;
import minicraft.core.io.InputHandler;
import minicraft.entity.Entity;
import minicraft.item.Inventory;
import minicraft.item.Item;
import minicraft.item.StackableItem;
import minicraft.screen.entry.ItemEntry;

class InventoryMenu extends ItemListMenu {

	private final Inventory inv;
	private final Entity holder;
	protected boolean creativeInv = false;

	InventoryMenu(Entity holder, Inventory inv, String title) {
		super(ItemListMenu.getBuilder(), ItemEntry.useItems(inv.getItems()), title);
		this.inv = inv;
		this.holder = holder;
	}

	InventoryMenu(InventoryMenu model) {
		super(ItemListMenu.getBuilder(), ItemEntry.useItems(model.inv.getItems()), model.getTitle());
		this.inv = model.inv;
		this.holder = model.holder;
		setSelection(model.getSelection());
	}

	@Override
	public void tick(InputHandler input, ControllerHandler controlInput) {
		super.tick(input, controlInput);

		boolean dropOne = input.isClicked("drop-one", controlInput);

		if(getNumOptions() > 0 && (dropOne || input.isClicked("drop-stack", controlInput))) {
			ItemEntry entry = ((ItemEntry)getCurEntry());
			if(entry == null) return;
			Item invItem = entry.getItem();
			Item drop = invItem.clone();

			if (!creativeInv) {
				if(dropOne && drop instanceof StackableItem && ((StackableItem)drop).count > 1) {
					// just drop one from the stack
					((StackableItem)drop).count = 1;
					((StackableItem)invItem).count--;
				} else {
					// drop the whole item.
					removeSelectedEntry();
				}
			}

			if(holder.getLevel() != null) {
				holder.getLevel().dropItem(holder.x, holder.y, drop);
			}
		}
	}

	@Override
	public void removeSelectedEntry() {
		inv.remove(getSelection());
		super.removeSelectedEntry();
	}
}
