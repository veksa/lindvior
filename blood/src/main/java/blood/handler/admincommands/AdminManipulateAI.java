package blood.handler.admincommands;

import gnu.trove.map.TIntObjectMap;
import l2s.gameserver.geodata.GeoEngine;
import l2s.gameserver.handler.admincommands.IAdminCommandHandler;
import l2s.gameserver.model.Player;
import l2s.gameserver.model.base.RestartType;
import l2s.gameserver.model.instances.NpcInstance;
import l2s.gameserver.templates.TeleportLocation;
import l2s.gameserver.utils.Location;
import l2s.gameserver.utils.TeleportUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blood.FPCInfo;
import blood.ai.FPCDefaultAI;
import blood.base.FPCRole;
import blood.data.holder.FarmZoneHolder;
import blood.data.holder.NpcHelper;
//import l2s.gameserver.model.Effect;
//import l2s.gameserver.tables.PetDataTable;
//import l2s.gameserver.templates.item.ItemTemplate.Grade;

public class AdminManipulateAI implements IAdminCommandHandler
{
	private static final Logger _log = LoggerFactory.getLogger(AdminManipulateAI.class);
	
	private static enum Commands
	{
		admin_find_path, admin_tryai
		}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanEditNPC)
			return false;

		switch(command)
		{
			case admin_tryai:
				FPCInfo newInfo = new FPCInfo(activeChar);
				newInfo.setAI(FPCRole.NEXUS_EVENT.getAI(activeChar));
				newInfo.getAI().toggleDebug();
	//			newInfo.setPVEStyle(FPCPveStyle.PARTY);
	//			newInfo.setParty();
			break;
			case admin_find_path:
				FPCDefaultAI ai = FPCInfo.getInstance(activeChar).getAI();
				Player player = activeChar;
				
				Location myRestartLocation = TeleportUtils.getRestartLocation(player, RestartType.TO_VILLAGE);
				NpcInstance buffer = NpcHelper.getClosestBuffer(myRestartLocation);
				NpcInstance gk = NpcHelper.getClosestGatekeeper(myRestartLocation);
				
				
				ai.addTaskTele(myRestartLocation);
				ai.addTaskSleep(3*1000);
				
				if(myRestartLocation.distance(buffer.getLoc()) < 4000)
				{
					ai.addTaskMove(Location.findAroundPosition(gk, 150), true, true);
					ai.addTaskSleep(5*1000);
				}
				
				ai.addTaskMove(Location.findAroundPosition(gk, 150), true, true);
				
				Location targetLocation = FarmZoneHolder.getInstance().getLocation(player);
				
				if(targetLocation == null)
					return false;
				
				Location middleRestartLocation = TeleportUtils.getRestartLocation(player, targetLocation, RestartType.TO_VILLAGE);
				NpcInstance middleGK = NpcHelper.getClosestGatekeeper(middleRestartLocation);
				
				if(gk.getObjectId() != middleGK.getObjectId())
				{
					gk = middleGK;
					ai.addTaskMove(Location.findAroundPosition(gk, 150), true, true);
					ai.addTaskSleep(5*1000);
				}
				
				TIntObjectMap<TeleportLocation> teleMap = gk.getTemplate().getTeleportList(1);
				double minDistance = Double.MAX_VALUE;
				Location spawnLocation = null;
				for(TeleportLocation teleLoc: teleMap.valueCollection())
				{
					double distanceFromSpawnLoc = teleLoc.distance(targetLocation);
					if(distanceFromSpawnLoc < minDistance && GeoEngine.canMoveToCoord(teleLoc.x, teleLoc.y, teleLoc.z, targetLocation.x, targetLocation.y, targetLocation.z, player.getGeoIndex()))
					{
						minDistance = distanceFromSpawnLoc;
						spawnLocation = teleLoc;
					}
				}
				
				if(spawnLocation != null)
				{
					ai.addTaskTele(spawnLocation);
					ai.addTaskSleep(3*1000);
					ai.addTaskMove(targetLocation, true);
				}
				else
				{
					ai.addTaskTele(targetLocation);
				}
			break;
		}
		return true;
	}
	

	@SuppressWarnings("rawtypes")
	@Override
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

}
