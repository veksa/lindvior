package blood;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2s.commons.util.Rnd;
import l2s.gameserver.Config;
import l2s.gameserver.ai.PlayerAI;
import l2s.gameserver.data.xml.holder.ItemHolder;
import l2s.gameserver.model.Player;
import l2s.gameserver.model.Servitor;
import l2s.gameserver.model.World;
import l2s.gameserver.model.base.ClassId;
import l2s.gameserver.model.base.InvisibleType;
//import l2s.gameserver.model.entity.events.impl.DominionSiegeEvent;
import l2s.gameserver.model.items.ItemInstance;
import l2s.gameserver.model.items.TradeItem;
import l2s.gameserver.model.pledge.Clan;
import l2s.gameserver.network.l2.s2c.PrivateStoreMsgBuy;
import l2s.gameserver.network.l2.s2c.PrivateStoreMsgSell;
import l2s.gameserver.skills.AbnormalEffect;
import l2s.gameserver.tables.ClanTable;
import l2s.gameserver.templates.item.ItemTemplate;
import l2s.gameserver.utils.ItemFunctions;
import l2s.gameserver.utils.Location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blood.ai.FPCDefaultAI;
import blood.base.FPCBase;
import blood.base.FPCItem;
import blood.base.FPCRole;
import blood.base.FPCSpawnStatus;
import blood.model.FPReward;
import blood.table.MerchantItem;


public class FPCInfo
{
	private static final Logger 		_log = LoggerFactory.getLogger(FPCInfo.class);
	// Main variables
	private Player _actor;
	private int _obj_id;
	private FPCSpawnStatus _status;
	private FPCRole	_role;
	private boolean _isMage;
	private String	_shop_status = "none";
	private ClassId _classId;
	private ItemInstance _weapon;
	private MerchantItem merchantItem;
	private int AILoopCount = 0;
	
	private static final String[][]	spawnLoc = {
		//{"87358","-141982","-1341", "Schuttgart Town Center"},
		//{"44070","-50243","-796","Rune Town Center"},
		//{"82321","55139","-1529","Oren Town Center"},
		//{"116589","76268","-2734","Hunters Village Town Center"},
		//{"111115","219017","-3547","Heine Town Center"},
		//{"147725","-56517","-2780","Goddard Town Center"},
		//{"147705","-53066","-2731","Goddard Einhasad Temple"},
		//{"-14225","123540","-3121","Gludio Town Center"},
		{"-83063","150791","-3120","Gludin Town Center 1"},
		{"-81784","150840","-3120","Gludin Town Center 2"},
		//{"82698","148638","-3473","Giran Town Center"},
		//{"18748","145437","-3132","Dion Town Center"},
		//{"147450","27064","-2208","Aden Town Center"}
	};	
	
	private static final int[] fpcClanList = {	268439913, 268440345, 268440398, 268446284, 268446827, 
												268446894, 268446993, 268447084, 268447157, 268447205, 
												268447222, 268447223, 268447290, 268447339, 268447404, 
												268447602, 268447722, 268447753, 268447762, 268447810, 
												268447827, 268447828, 268451094, 268451114, 268451115, 
												268451117, 268451158, 268451162, 268451163, 268451232, 
												268451233, 268451234, 268451254, 268451258, 268451409, 
												268448944, 268448961, 268449008, 268449026, 268449027, 
												268449098, 268448417, 268448910, 268448911, 268448959, 
												268449009, 268449028, 268449029, 268449030, 268449320, 
												268454832, 268466180, 268466406, 268466700, 268466837, 
												268467067, 268467220, 268467418, 268468534, 268468537, 
												268468538, 268468539, 268468540, 268468557, 268468558, 
												268468559, 268469537, 268469865, 268470060, 268470386, 
												268470694, 268471152, 268471409, 268489316
											};

	private static FPCBase _instances = new FPCBase();
	
	
	public FPCInfo(int obj_id)
	{
		//_owner = owner;
		_obj_id = obj_id;
		setStatus(FPCSpawnStatus.OFFLINE);
		_instances.addInfo(this);
		
	}
	
	public static FPCInfo getInstance(int obj_id)
	{
		//_log.info("obj_id " + obj_id);
		return _instances.getPlayer(obj_id) != null ? _instances.getPlayer(obj_id) : new FPCInfo(obj_id);
	}
	
	public static FPCInfo getInstance(Player player)
	{
		return getInstance(player.getObjectId());
	}
	
	public Player getActor()
	{
		return _actor;
	}
	
	public int getObjectId()
	{
		return _obj_id;
	}
	
	public void setStatus(FPCSpawnStatus status)
	{
		//_log.info("set status function");
		if(_status == status)
			return;
		
		//_log.info(getActor()+": change status from " + _status + " to "+status);
		
		if(_status != null)
			_status.remove(this);
		
		//_log.info("BEFORE: _status " + _status + " status " + status);
		
		_status = status;
		
		//_log.info("AFTER: _status " + _status + " status " + status);
		
		if(_status != null)
			_status.add(this);
		
		switch(_status)
		{
		case OFFLINE:
			if(_role != null)
			{
				_role.remove(this);
				_role = null;
			}
			break;
		case ONLINE:
			setRole(FPCRole.IDLE);
			break;
		}
			
	}
	
	public FPCSpawnStatus getStatus()
	{
		return _status;
	}
	
	public void setRole(FPCRole role)
	{
		if(_role == role)
		{
			_log.info(getActor()+": same old role "+role);
		}
		else
		{
			_log.info(getActor()+": change role from " + _role + " to "+role);

			if(_role != null)
				_role.remove(this);
			
			_role = role;
			
			_role.add(this);
		}
		
		if(_role != null)
		{
			setAI(_role.getAI(getActor()));
			_log.info("SetRole: " + getActor().getAI());
		}
	}
	
	public FPCRole getRole()
	{
		return _role;
	}
	
	public boolean isMage()
	{
		return _isMage;
	}
	
	public ClassId getClassId()
	{
		return _classId;
	}
	
	public void counterDisarm()
	{
		Player actor = getActor();
		
		if(actor == null || _weapon == null)
			return;
		
		actor.getInventory().equipItem(_weapon);
	}
	
	public static void autoshot(Player player)
	{
		// bsps
		FPCItem.supplyItem(player, 3950, 10);
		FPCItem.supplyItem(player, 3951, 10);
		FPCItem.supplyItem(player, 3952, 10);
		// ss
		FPCItem.supplyItem(player, 1465, 10);
		FPCItem.supplyItem(player, 1466, 10);
		FPCItem.supplyItem(player, 1467, 10);
		// arrow
		FPCItem.supplyItem(player, 1343, 10);
		FPCItem.supplyItem(player, 1344, 10);
		FPCItem.supplyItem(player, 1345, 10);
		//ss beast
		FPCItem.supplyItem(player, 6645, 10);
		
		// set auto ss
		player.addAutoSoulShot(3950);
		player.addAutoSoulShot(3951);
		player.addAutoSoulShot(3952);
		player.addAutoSoulShot(1465);
		player.addAutoSoulShot(1466);
		player.addAutoSoulShot(1467);
		player.addAutoSoulShot(6645);
	}
	
//	private void registerWithNexus(Player player)
//	{
//		EventBuffer.getInstance().loadPlayer(player.getEventInfo());
//		EventManager.getInstance().onPlayerLogin(player.getEventInfo());
//	}
	
	public void setAI(FPCDefaultAI ai)
	{
		Player actor = getActor();
		
		if(actor != null)
		{
			//if(ai instanceof MarketFPC) cancelShop();
			actor.setAI(ai);
		}
	}	
	
	@SuppressWarnings("unchecked")
	public void setAI(String ai)
	{
		Player actor = getActor();
		
		if(actor == null)
			return;
		
		Class<FPCDefaultAI> classAI = null;
		try {
		classAI = (Class<FPCDefaultAI>) Class.forName("blood.ai." + ai);
		}catch(Exception e){
			
		}
		
		if(classAI == null)
			_log.error("Not found ai class for ai: " + ai + ". FakePlayer: " + actor);
		else
		{
			Constructor<FPCDefaultAI> constructorAI = (Constructor<FPCDefaultAI>)classAI.getConstructors()[0];
			try
			{
				setAI(constructorAI.newInstance(actor));
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public FPCDefaultAI getAI()
	{
		Player actor	= getActor();
		
		PlayerAI ai = actor.getAI();
		
		return (FPCDefaultAI) ai;
	}
	
	public boolean uyThac()
	{
		
		Player actor = World.getPlayer(getObjectId());
		_log.info("uyThac: start "+actor);
		try{
			if (actor.isLogoutStarted() || actor.isInOfflineMode()) 
			{
				_log.info("uyThac: failed at stage 1");
				return false; 
			}
			
			// GM dont need uythac
			if(actor.isGM() && !Config.EVERYBODY_HAS_ADMIN_RIGHTS)
			{
				_log.info("uyThac: failed GM dont need that");
				return false;
			}
			
			/* FIXME */
			
//			if(actor.getClassId().getLevel() < 3)
//			{
//				_log.info("uyThac: failed doesnt change class");
//				return false;
//			}
			
			// remove invisible effect
			if(actor.isInvisible())
			{
				actor.setInvisibleType(InvisibleType.NONE);
				actor.broadcastCharInfo();
				actor.stopAbnormalEffect(AbnormalEffect.STEALTH);
				if (actor.getServitors().length > 0)
				{
					for (Servitor sum: actor.getServitors())
						sum.broadcastCharInfo();
				}
			}
			
			// set uythac status
//			actor.setUyThac(); FIXME
//			actor.setFakePlayer(); FIXME
			// auto ss
			autoshot(actor);
			// gear up
			FPCItem.semiGearUp(actor);
			
			//randomTown(actor);
			
			_actor = actor;
			_isMage = actor.isMageClass();
	        _classId = actor.getClassId();
			
			actor.broadcastCharInfo();
			actor.broadcastStatusUpdate();
			
			setStatus(FPCSpawnStatus.ONLINE);
			_weapon = actor.getActiveWeaponInstance();
			setRole(FPCRole.NEXUS_EVENT);
			
			return true;
		}catch(Exception e)
		{
			_log.error("player uythac failed" + getObjectId(), e);
			return false;
		}
		
	}
	
	public void spawn()
	{
		//_log.info("spawn function");
		Player player = null;
    	try{
    		player = Player.restore(getObjectId());
//            player.setFakePlayer();
            player.spawnMe();
    		player.setRunning();
    		player.setHeading(Rnd.get(0, 9000));
    		autoshot(player);
//    		registerWithNexus(player);
            player.setOnlineStatus(true);
            player.restoreExp();
            //player.broadcastCharInfo();
            //_log.info("spawn " + player.getName());
//            randomTown(player);
//            if(Rnd.chance(20))
//            	addClan(player);
//            if(Rnd.chance(30) && player.getTitle().isEmpty() && player.getClan() != null)
//            	setClanTitle(player, FPCNameTable.getRandomTitle());
            // move to spawn zone
            //player.setXYZ(Config.SPAWN_X + Rnd.get(-700, 700), Config.SPAWN_Y + Rnd.get(-700, 700), Config.SPAWN_Z);
            
            _isMage = player.isMageClass();
            _classId = player.getClassId();
            
//            FPCItem.gearUp(player);
            FPReward.getInstance().giveReward(player);
            
            player.broadcastCharInfo();
            
            _actor = player;

//            cancelShop();
            
            setStatus(FPCSpawnStatus.ONLINE);
            
            _weapon = player.getActiveWeaponInstance();
            
    	}catch (Exception e) {
            _log.error("Fake Players Engine: Error loading player: " + player, e);
            if (player != null) {
                player.deleteMe();
            }
        }
	}
	
	public void cancelShop()
	{
		//if(_actor.getPrivateStoreType() == Player.STORE_PRIVATE_NONE)
		//	return;
		
		List<TradeItem> list = new CopyOnWriteArrayList<TradeItem>();
		list.clear();
        _actor.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
        _actor.standUp();
        _actor.setSellList(false, list);
        _log.info("cancel shop, list " + list.size());
        _actor.setBuyList(list);
		
	}
	
	public void kick()
	{
		Player actor = getActor();
		if(actor == null)
			return;
		
		setStatus(FPCSpawnStatus.OFFLINE);
		
		_log.info(actor +": kicked");
		actor.kick();
	}
	
	public boolean isInEvent()
	{
//		return NexusEvents.isInEvent(getActor());
		return false;
		
	}
	
	public void randomTown(Player player)
	{
		String[] randomLoc = spawnLoc[Rnd.get(spawnLoc.length)];
		
		Location baseLoc = Location.findPointToStay(
												        Integer.parseInt(randomLoc[0]), 
												        Integer.parseInt(randomLoc[1]), 
												        Integer.parseInt(randomLoc[2]),
														50, 650, player.getGeoIndex());
		player.setLoc(baseLoc);
	}
	
	public void setSellShop(MerchantItem item)
	{
		//_log.info("player " + _actor + " item: " + item.getItemID() + " price: " + item.getPrice());
		if(item.getPrice() <= 0) return;
		
		//save it for later references
		setMerchantItem(item);
		
		//check amount of item if available, if not, generate more
		ItemInstance sellItem = checkItemAvailable(item.getItemID(), item.getItemAmount());
		
		if(item.getShopTitle().equalsIgnoreCase(""))
			item.setShopTitle(generateShopTitle(sellItem.getTemplate().getName(), item.getPrice()));
		
		TradeItem tradeItem = new TradeItem(sellItem);
		List<TradeItem> list = new CopyOnWriteArrayList<TradeItem>();	
		
		tradeItem.setItemId(item.getItemID());
		tradeItem.setCount(item.getItemAmount());
		tradeItem.setOwnersPrice(item.getPrice());
		
		list.add(tradeItem);
    	
		if(!list.isEmpty())
		{
			
			_actor.setSellList(false, list);
			_actor.setSellStoreName(item.getShopTitle());
			_actor.saveTradeList();
			_actor.setPrivateStoreType(Player.STORE_PRIVATE_SELL);
			_actor.broadcastPacket(new PrivateStoreMsgSell(_actor));
			_actor.sitDown(null);
			_actor.broadcastCharInfo();
			
			//set owner for the MerchantItem, and write into db, table fpc_merchant
			item.setOwner(_obj_id);
			
		}
		//set the current character as shop, so stop asking it to do anything else
		setShopStatus(item.getStatus());
		
	}
	
	public void setBuyShop(MerchantItem item)
	{
		//save it for later references
		setMerchantItem(item);
		
		//add adena for buying
		_actor.addAdena(item.getPrice()*item.getItemAmount());
		
		TradeItem tradeItem = new TradeItem();
		List<TradeItem> list = new CopyOnWriteArrayList<TradeItem>();	
		
		
		tradeItem.setItemId(item.getItemID());
		tradeItem.setCount(item.getItemAmount());
		tradeItem.setOwnersPrice(item.getPrice());
		
		list.add(tradeItem);
		
		if(!list.isEmpty())
		{
			
			_actor.setBuyList(list);
			_actor.setBuyStoreName(item.getShopTitle());
			_actor.saveTradeList();
			_actor.setPrivateStoreType(Player.STORE_PRIVATE_BUY);
			_actor.broadcastPacket(new PrivateStoreMsgBuy(_actor));
			_actor.sitDown(null);
			_actor.broadcastCharInfo();
			
			//set owner for the MerchantItem, and write into db, table fpc_merchant
			item.setOwner(_obj_id);
			
		}
		//set the current character as shop, so stop asking it to do anything else
		setShopStatus(item.getStatus());
		
	}
	
	
	private String generateShopTitle(String name, long price)
	{
		int maxTitleLength = 29;
		
		String shopName;
		
		if(Rnd.chance(50))
			shopName = "Cheap ";
		else if(Rnd.chance(50))
			shopName = "Best ";
		else 
			shopName = "";
	
		String itemName = shortenItemName(name);
		
		String itemPrice = (Rnd.chance(50))?shortenItemPrice(price):"";
		
		shopName = shopName.concat(itemName);
		shopName = shopName.concat(" ");
		
		if(shopName.length() + itemPrice.length() <= maxTitleLength)
			shopName = shopName.concat(itemPrice);
		
		return shopName;
	}
	
	private String shortenItemName(String itemName)
	{
		String[][] itemNameList = {
									{"Destroyer Hammer", "Des Hammer"},
									{"Dasparion's Staff", "Daspa Staff"},
									{"Infernal Master", "Infe Master"},
									{"Meteor Shower", "Meteor"},
									{"Spiritual Eye", "SpirEye"},
									{"White Lightning", "A rapier"},
									{"Elemental Sword", "A Msword"},
									{"Keshanberk*Keshanberk", "kes*kes"},
									{"Carnage Bow", "carnage"},
									{"Branch of the Mother Tree", "BoMT"},
									{"Dragon Slayer", "DraSlayer"},
									{"Flaming Dragon Skull", "FlameDraSkull"},
									{"Dragon Grinder", "DraGrinder"},
									{"Soul Separator", "SoulSep"},
									{"Dark Legion's Edge", "DLE"},
									{"Sword of Miracles", "SoM"},
									{"Keshanberk*Damascus", "Kes*Dam"},
									{"Behemoth's Tuning Fork", "BTFork"},
									{"Sword of Ipos", "Ipos"},
									{"Barakiel's Axe", "Bara Axe"},
									{"Cabrio's Hand", "Cabri Hand"},
									{"Screaming Vengeance", "Best A xbow"},
									{"Sobekk's Hurricane", "Sobekk Fist"},
									{"Damascus*Damascus", "Dam*Dam"},
									{"Damascus * Tallum Blade", "Dam*Tal"},
									{"Dragon Hunter Axe", "Dra Axe"},
									{"Heaven's Divider", "Heaven Div"},
									{"Arcana Mace", "AM"},
									{"Basalt Battlehammer", "Basalt"},
									{"Draconic Bow", "Drac Bow"},
									{"Forgotten Blade", "FB"},
									{"Tallum Blade*Dark Legion's Edge", "Tal*DLE"},
									{"Majestic Necklace", "MJT Neck"},
									{"Majestic Earring", "MJT Earring"},
									{"Majestic Ring", "MJT Ring"},
									{"Tateossian Necklace", "Tat Neck"},
									{"Tateossian Earring", "Tat Earring"},
									{"Tateossian Ring", "Tat Ring"},
									{"Material Chest Lv.1", "Chest lv1"},
									{"Material Chest Lv.2", "Chest lv2"},
									{"Material Chest Lv.3", "Chest lv3"},
									{"Material Chest Lv.4", "Chest lv4"},
									{"Material Chest Lv.5", "Chest lv5"},
									{"Material Chest Lv.6", "Chest lv6"},
									{"Material Chest Lv.7", "Chest lv7"}
									};
		
		for(int i=0;i<itemNameList.length;i++)
			if(itemName.indexOf(itemNameList[i][0]) > -1)
				return itemNameList[i][1];
		
		
		return itemName;		
	}
	
	private String shortenItemPrice(long price)
	{
		String itemPrice;
		
		try
		{
			if(price > 1000)
			{
				if(price%1000 == 0)
					itemPrice = price/1000 + "k";
				else
					itemPrice = String.format("%.1f",price/1000) + "k";
			}
			else
				itemPrice = price + "a";
		
			return itemPrice;
		}
		catch(Exception e)
		{
			return "";
		}
	}

	private ItemInstance checkInventory(int item_id)
	{
		return _actor.getInventory().getItemByItemId(item_id);
	}
	
	private ItemInstance checkItemAvailable(int item_id, long amount)
	{
		ItemInstance item = checkInventory(item_id);
		
		if(item == null)
		{
			//generate item
			item = ItemFunctions.createItem(item_id);
			item.setCount(amount);
			_actor.getInventory().addItem(item);
		}
		else
		{
			//if the current item amount is not enough compare to the requirement
			//only apply for stackable items
			ItemTemplate itemTemplate = ItemHolder.getInstance().getTemplate(item_id);
			if(item.getCount() < amount && itemTemplate.isStackable())
			{
				//generate some more items
				item.setCount(amount - item.getCount());
			}
		}			
		
		return item;
	}
		
	public MerchantItem getMerchantItem()
	{
		if(merchantItem == null)
		{
			//try to get from the Character Variables
			String rs = _actor.getVar("merchant_item");
			if(rs != null && !rs.isEmpty())
			{
				String[] choppedString = rs.split(";");
				merchantItem = new MerchantItem(Integer.parseInt(choppedString[0]),
												Integer.parseInt(choppedString[1]),
												Integer.parseInt(choppedString[2]),
												Integer.parseInt(choppedString[3]),
												choppedString[4],
												Integer.parseInt(choppedString[5]),
												choppedString[6],
												Long.parseLong(choppedString[7]));
			}
		}
		return merchantItem;
	}

	public void setMerchantItem(MerchantItem merchantItem)
	{
		if(merchantItem == null) return;
		
		if(merchantItem.getID() != -1)
			_actor.setVar("merchant_item", merchantItem.toString(), -1 );
	
		//_log.info("set Var: " + merchantItem.toString());
		this.merchantItem = merchantItem;
		
	}

	public String getShopStatus()
	{
		return _shop_status;
	}

	public void setShopStatus(String shop_status)
	{
		
		this._shop_status = shop_status;
	}
	
	public void addClan(Player player)
	{
		Clan currentClan = player.getClan();
		
		if(currentClan == null)
		{
			Clan newClan = getRandomClan();
			
			if(newClan != null)
				setClan(player, newClan);
		}
		
	}
	
	public void setClanTitle(Player player, String title)
	{
		Clan currentClan = player.getClan();
		
		if(title.length() > 16) 
			title = title.substring(0, 15);
		
		if(currentClan != null && currentClan.getLevel() >= 3)
		{
			player.setTitle(title);
		}
	}
	
	public Clan getRandomClan()
	{
		int clanId 		= fpcClanList[Rnd.get(fpcClanList.length)];
		Clan newClan 	= ClanTable.getInstance().getClan(clanId);
		
		if(newClan != null && newClan.getAllSize() > 50)
			return getRandomClan();
		else
		return newClan;
	}
	
	public void setClan(Player player, Clan clan)
	{
//		int pledgeType = Clan.SUBUNIT_MAIN_CLAN;
//		player.sendPacket(new JoinPledge(clan.getClanId()));
//
//		SubUnit subUnit = clan.getSubUnit(pledgeType);
//		if(subUnit == null)
//			return;
//
//		UnitMember member = new UnitMember(clan, player.getName(), player.getTitle(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), Clan.SUBUNIT_MAIN_CLAN, player.getPowerGrade(), player.getApprentice(), player.getSex(), Clan.SUBUNIT_NONE);
//		subUnit.addUnitMember(member);
//
//		player.setPledgeType(pledgeType);
//		player.setClan(clan);
//
//		member.setPlayerInstance(player, false);
//
//		member.setPowerGrade(clan.getAffiliationRank(player.getPledgeType()));
//
//		clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(member), player);
//		clan.broadcastToOnlineMembers(new SystemMessage2(SystemMsg.S1_HAS_JOINED_THE_CLAN).addString(player.getName()), new PledgeShowInfoUpdate(clan));
//
//		// this activates the clan tab on the new member
//		player.sendPacket(SystemMsg.ENTERED_THE_CLAN);
//		player.sendPacket(player.getClan().listAll());
//		player.setLeaveClanTime(0);
//		player.updatePledgeClass();
//
//		// add skills to the player
//		clan.addSkillsQuietly(player);
//		// Display
//		player.sendPacket(new PledgeSkillList(clan));
//		player.sendPacket(new SkillList(player));
//
//		EventHolder.getInstance().findEvent(player);
//		if(clan.getWarDominion() > 0) // bug offs after joined the clan quests need to relog
//		{
//			DominionSiegeEvent siegeEvent = player.getEvent(DominionSiegeEvent.class);
//
//			siegeEvent.updatePlayer(player, true);
//		}
//		else
//			player.broadcastCharInfo();
//
//		player.store(false);
	}

	public int getAILoopCount()
	{
		return AILoopCount;
	}

	public void increaseAILoopCount()
	{
		AILoopCount++;
	}
}