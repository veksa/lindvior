package blood.ai;

import gnu.trove.map.TIntObjectMap;

import java.util.HashSet;

import l2s.commons.util.Rnd;
import l2s.gameserver.ai.CtrlEvent;
import l2s.gameserver.ai.CtrlIntention;
import l2s.gameserver.geodata.GeoEngine;
import l2s.gameserver.instancemanager.MapRegionManager;
import l2s.gameserver.model.Creature;
//import l2s.gameserver.model.Effect;
import l2s.gameserver.model.GameObject;
import l2s.gameserver.model.Party;
import l2s.gameserver.model.Player;
import l2s.gameserver.model.Skill;
import l2s.gameserver.model.base.RestartType;
import l2s.gameserver.model.instances.NpcInstance;
import l2s.gameserver.skills.EffectType;
import l2s.gameserver.tables.SkillTable;
import l2s.gameserver.templates.TeleportLocation;
import l2s.gameserver.templates.mapregion.RestartArea;
import l2s.gameserver.templates.skill.EffectTemplate;
//import l2s.gameserver.skills.effects.EffectTemplate;
import l2s.gameserver.utils.Location;
import l2s.gameserver.utils.PositionUtils;
import l2s.gameserver.utils.TeleportUtils;
import blood.data.holder.FarmZoneHolder;
import blood.data.holder.NpcHelper;
import blood.model.FPReward;

public class EventFPC extends FPCDefaultAI
{

	protected Skill[] _selfSkills = Skill.EMPTY_ARRAY;
	
	protected long _nextBuffRound 		= 0;
	protected long _nextSummonRound 	= 0;
	protected long _nextSumCubicRound 	= 0;
	protected long _nextEquipRound 		= 0;
	
	protected HashSet<Integer> _allowSkills 	= new HashSet<Integer>();
	
	
	public EventFPC(Player actor)
	{
		super(actor);
		//_is_debug = false;
	}
	
	// block skill as default
	public boolean isAllowSkill(int skill_id)
	{
		return _allowSkills.contains(skill_id);
	}
	
	protected boolean createNewTask()
	{
		return thinkBuff() || thinkCubic() || thinkSummon() || thinkEquip() || createFightTask();
	}
	
	protected boolean createFightTask()
	{
		return defaultFightTask();
	}
	
	private boolean thinkEquip() 
	{
		if(_nextEquipRound > System.currentTimeMillis())
			return false;
		
		_nextEquipRound = System.currentTimeMillis();
		FPReward.getInstance().giveReward(getActor());
		return false;
	}

	private boolean thinkCubic() 
	{
		if(_nextSumCubicRound > System.currentTimeMillis())
			return false;
		
		_nextSumCubicRound = System.currentTimeMillis();
		
		if(_cubicSkills == null || _cubicSkills.length == 0)
			return false;
		
		Player player = getActor();
		
		// TODO add method to validate max cubic
//		if(player.getCubics().size() > 1)
//			return false;
		
		Skill skill = _cubicSkills[0];
		
		for(EffectTemplate et: skill.getEffectTemplates())
		{
			// add cubic
			if(et.getEffectType() == EffectType.Cubic)
			{
				if(player.getCubic(et.getParam().getInteger("cubicId")) == null)
				{
					chooseTaskAndTargets(skill, player, 0);
					return true;
				}
			}
		}
		
		return false;
	}

	protected boolean thinkSummon() 
	{
		if(_nextBuffRound > System.currentTimeMillis())
			return false;
		
		_nextEquipRound = System.currentTimeMillis();
		
		if(_sumSkills == null || _sumSkills.length == 0)
			return false;
		
		Player player = getActor();
		
		Skill[] summonSkills = selectUsableSkills(player, 0, _sumSkills);
		
		if(summonSkills.length == 0)
			return false;
		
		Skill summonSkill = summonSkills[Rnd.get(summonSkills.length)];
		
		if(player.getServitors().length < getMaxSummon()  && player.getCurrentMp() > 300)
			addTaskBuff(player, summonSkill);
		
		return false;
	}
	
	protected int getMaxSummon()
	{
		return 1;
	}

	protected boolean thinkBuff()
	{
		// add interval check for performance
		if(_nextBuffRound > System.currentTimeMillis())
			return false;
		
		_nextBuffRound = System.currentTimeMillis();
		
		doNewbieBuff();
		
		if(_buffSkills == null || _buffSkills.length <= 0)
			return false;
		
		Player player = getActor();
//		
//		for(Skill skill: _buffSkills)
//		{
//			if(thinkBuff(skill))
//				return true;
//		}
		
		// think self buff
		debug("I buff my self");
		
		// think friend buff
		if(player.isInParty())
		{
			for(Player member: player.getParty().getPartyMembers())
			{
				if(member == player)
					continue;
				
				debug("check and add buff task for party member:"+member);
			}
		}
		
		return false;
	}
	
	/*
	 * Buff method
	 */
	
	protected void doNewbieBuff()
	{
		for(Skill skill: getNewbieBuff())
			buffFromNoWhere(skill);
	}
	
	protected HashSet<Skill> getNewbieBuff()
	{
		HashSet<Skill> skills = new HashSet<Skill>();
		
		skills.add(getSkill(15642, 1));
		skills.add(getSkill(15643, 1));
		skills.add(getSkill(15644, 1));
		skills.add(getSkill(15645, 1));
		skills.add(getSkill(15646, 1));
		skills.add(getSkill(15647, 1));
		
		skills.add(getSkill(15651, 1));
		skills.add(getSkill(15652, 1));
		skills.add(getSkill(15653, 1));
		
		skills.add(getNpcSuperiorBuff());
		
		// TODO add montor exp buff for character < 85
		
		return skills;
	}
	
	protected Skill getNpcSuperiorBuff()
	{
		return getSkill(15648, 1);
//		return getSkill(15649, 1); warrior
//		return getSkill(15650, 1); wizzard
	}
	
	protected boolean buffFromNoWhere(Skill skill)
	{
		Player player = getActor();
		if(player.getEffectList().containsEffects(skill))
			return false;
		
		skill.getEffects(player, player, false);
		return true;
	}
	
	/*
	 * Method provide protect party
	 */
	
	protected void notifyFriends(Creature attacker, int damage)
	{
		Player actor = getActor();
		Party party = actor.getParty();
		
		if(party != null)
		{
			for(Player member: party.getPartyMembers())
			{
				if(!member.equals(actor))
				{
					member.getAI().notifyEvent(CtrlEvent.EVT_CLAN_ATTACKED, actor, attacker, damage);
				}
			}
		}
	}
	
	@Override
	protected void onEvtClanAttacked(Creature attacked, Creature attacker, int damage)
	{
		// TODO - put logic protect party here
		Player actor = getActor();
		if ((attacker == null) || actor.isDead())
		{
			return;
		}
		
		// Adding only hate damage if an attacker - a game character will be added to L2NpcInstance.onReduceCurrentHp
		_aggroList.addDamageHate(attacker, damage, damage);
		
		// Usually 1 hate added summon the owner to death summon mob attacked the host.
		if ((damage > 0) && (attacker.isServitor() || attacker.isPet()))
		{
			_aggroList.addDamageHate(attacker.getPlayer(), damage, damage);
		}
		
		// if not attack, set attacker to target
		if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
		{
			if (!actor.isRunning())
			{
				startRunningTask(AI_TASK_ATTACK_DELAY);
			}
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		}
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{

		debug("get attacked by:"+attacker+" damage:"+damage);
		
		Player actor = getActor();
		if ((attacker == null) || actor.isDead())
		{
			return;
		}
		
		// Adding only hate damage if an attacker - a game character will be added to L2NpcInstance.onReduceCurrentHp
		_aggroList.addDamageHate(attacker, damage, damage);
		
		// Usually 1 hate added summon the owner to death summon mob attacked the host.
		if ((damage > 0) && (attacker.isServitor() || attacker.isPet()))
		{
			_aggroList.addDamageHate(attacker.getPlayer(), damage, damage);
		}
		
		if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
		{
			if (!actor.isRunning())
			{
				startRunningTask(AI_TASK_ATTACK_DELAY);
			}
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		}
		
		notifyFriends(attacker, damage);
	}
	
	/*
	 * About dead
	 */
	
	@Override
	protected void onEvtDead(Creature killer)
	{
		super.onEvtDead(killer);
		setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}
	
	/*
	 * About force warrior or kamael soul
	 */
	
	protected boolean thinkUseWarriorForce(int skillId)
	{
		return thinkUseWarriorForce(skillId, 4);
	}	
	
	protected boolean thinkUseWarriorForce(int skillId, int forceLevel)
	{
		Player actor = getActor();
		
		if(actor.getCurrentMp() > 300 && actor.getCurrentHp() > 300)
		{
			if(actor.getIncreasedForce() < forceLevel)
			{
				selfBuff(skillId);
				return true;
			}
		}
		
		return false;
	}
	
	protected boolean thinkUseKamaelSoul(int skillId, int soulLevel)
	{
		Player actor = getActor();
		if(actor.getCurrentMp() > 300 && actor.getCurrentHp() > 300)
		{
			if(actor.getConsumedSouls() < soulLevel)
			{
				selfBuff(skillId);
				return true;
			}
		}
		
		return false;
	}
	
	
	
	protected boolean thinkBuff(int skillId)
	{
		return thinkBuff(getActor().getKnownSkill(skillId));
	}
	
	protected boolean thinkBuff(Skill skill)
	{
		if(skill == null)
			return false;
		
		Player actor = getActor();
		for(EffectTemplate et: skill.getEffectTemplates())
		{
			// add cubic
			if(et.getEffectType() == EffectType.Cubic)
			{
				if(actor.getCubic(et.getParam().getInteger("cubicId")) == null)
				{
					chooseTaskAndTargets(skill, actor, 0);
					return true;
				}
			}
			// real buff
			else if(actor.getEffectList().getEffectsCount(skill) == 0)
			{
				chooseTaskAndTargets(skill, actor, 0);
				return true;
			}
		}
		
		return false;
	}
	
	protected boolean thinkBuff(int[] skills)
	{
		if(skills == null || skills.length <= 0)
			return false;
		
		for(int skillId: skills)
		{
			if(thinkBuff(skillId))
				return true;
		}
		
		return false;
	}
	
	
	@Override
	public int getMaxAttackTimeout()
	{
		return 100000;
	}
	
	protected boolean defaultMoveTask()
	{
		Player player = getActor();
		
		Location myRestartLocation = TeleportUtils.getRestartLocation(player, RestartType.TO_VILLAGE);
		NpcInstance buffer = NpcHelper.getClosestBuffer(myRestartLocation);
		NpcInstance gk = NpcHelper.getClosestGatekeeper(buffer);
		Location targetLocation = FarmZoneHolder.getInstance().getLocation(player);
		RestartArea myRestartArea = MapRegionManager.getInstance().getRegionData(RestartArea.class, player.getLoc());
		RestartArea targetRestartArea = MapRegionManager.getInstance().getRegionData(RestartArea.class, targetLocation);
		
		debug("Where am i?");
		
		debug("My current area:"+myRestartArea);
		debug("My current restart loc:"+myRestartLocation);
		debug("My target location:"+targetLocation);
		debug("Target area:"+targetRestartArea);
		debug("Move to next buffer:"+buffer);
		debug("Move to next GK:"+gk);
		
		if(myRestartArea != targetRestartArea)
		{
			debug("diff area we should change villages");
			Location middleRestartLocation = TeleportUtils.getRestartLocation(player, targetLocation, RestartType.TO_VILLAGE);
			NpcInstance middleGK = NpcHelper.getClosestGatekeeper(middleRestartLocation);
			debug("=>Tele to target GK:"+middleGK);
			gk = middleGK;
		}
		
		debug("find spawn zone");
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
			debug("Teleport to farm zone entrance:"+spawnLocation);
			debug("Move to farm spot:"+targetLocation);
		}
		else
		{
			debug("Teleporto direct to farm spot:"+targetLocation);
		}
		
		return false;
	}
	
	protected boolean thinkActiveByClass()
	{
		return false;
	}
	

	
	public static Location findFrontPosition(GameObject obj, int posX, int posY, int radiusmin, int radiusmax)
	{
		if(radiusmax < radiusmin && radiusmax > 100)
		{
			radiusmin = radiusmax - 100;
		}
		
		if(radiusmax == 0 || radiusmax < radiusmin)
			return new Location(obj);

		double collision = obj.getCollisionRadius();
		int randomRadius, randomAngle, tempz;
		int minangle = 0;
		int maxangle = 360;

		double angle = PositionUtils.calculateAngleFrom(obj.getX(), obj.getY(), posX, posY);
		minangle = (int) angle - 30;
		maxangle = (int) angle + 30;

		Location pos = new Location();
		for(int i = 0; i < 100; i++)
		{
			randomRadius = Rnd.get(radiusmin, radiusmax);
			randomAngle = Rnd.get(minangle, maxangle);
			pos.x = obj.getX() + (int) ((collision + randomRadius) * Math.cos(Math.toRadians(randomAngle)));
			pos.y = obj.getY() + (int) ((collision + randomRadius) * Math.sin(Math.toRadians(randomAngle)));
			pos.z = obj.getZ();
			tempz = GeoEngine.getHeight(pos.x, pos.y, pos.z, obj.getGeoIndex());
			if(Math.abs(pos.z - tempz) < 200 && GeoEngine.getNSWE(pos.x, pos.y, tempz, obj.getGeoIndex()) == GeoEngine.NSWE_ALL 
				&& GeoEngine.canMoveToCoord(obj.getX(), obj.getY(), obj.getZ(), pos.getX(), pos.getY(), pos.getZ(), obj.getGeoIndex()))
			{
				pos.z = tempz;
				pos.h = obj.getHeading();
				return pos;
			}
		}

		return new Location(obj);
	}
	
	protected void tryCastSkill(int skillId, Creature target)
	{
		tryCastSkill(skillId, target, getActor().getDistance(target));
	}

	protected void tryCastSkill(int skillId, Creature target, double distance)
	{
		Player actor = getActor();
		Skill skill = actor.getKnownSkill(skillId);
		debug(actor + " try cast skill:"+skillId+" on distance: "+ distance + " on: "+target);
		if(skill != null && target != null)
		{
			chooseTaskAndTargets(skill, target, distance);
		}
			
	}
	
	protected void selfBuff(int skillId)
	{
		Player actor = getActor();
		Skill skill = actor.getKnownSkill(skillId);
		if(skill != null)
		{
			chooseTaskAndTargets(skill, actor, 0);
		}
			
	}
	
	public Skill getUniqueSkill(int[] skills)
	{
		Skill skill = null;
		Player actor = getActor();
		for(int skill_id: skills){
			if(skill == null) skill = actor.getKnownSkill(skill_id);
		}
		
		return skill;
	}
	
	/**
	 * utils method to get any skill
	 * @param id
	 * @param level
	 * @return
	 */
	
	protected Skill getSkill(int id, int level)
	{
		return SkillTable.getInstance().getInfo(id, level);
	}
	

}
