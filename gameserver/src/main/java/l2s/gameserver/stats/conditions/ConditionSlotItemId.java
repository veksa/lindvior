package l2s.gameserver.stats.conditions;

import l2s.gameserver.model.Player;
import l2s.gameserver.model.items.Inventory;
import l2s.gameserver.model.items.ItemInstance;
import l2s.gameserver.stats.Env;

public final class ConditionSlotItemId extends ConditionInventory
{
	private final int _itemId;

	private final int _enchantLevel;

	public ConditionSlotItemId(int slot, int itemId, int enchantLevel)
	{
		super(slot);
		_itemId = itemId;
		_enchantLevel = enchantLevel;
	}

	@Override
	protected boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		Inventory inv = ((Player) env.character).getInventory();
		if(_slot >= 0)
		{
			ItemInstance item = inv.getPaperdollItem(_slot);
			if(item == null)
				return _itemId == 0;

			return item.getItemId() == _itemId && item.getEnchantLevel() >= _enchantLevel;
		}
		else
		{
			ItemInstance item;
			for(int slot : Inventory.PAPERDOLL_ORDER)
			{
				item = inv.getPaperdollItem(slot);
				if(item == null)
					continue;

				if(item.getItemId() == _itemId && item.getEnchantLevel() >= _enchantLevel)
					return true;
			}
		}
		return false;
	}
}
