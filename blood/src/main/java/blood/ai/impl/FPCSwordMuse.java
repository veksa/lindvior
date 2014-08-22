package blood.ai.impl;

import java.util.ArrayList;
import java.util.List;

import l2s.gameserver.model.Player;

public class FPCSwordMuse extends TankerPC
{
	public FPCSwordMuse(Player actor)
	{
		super(actor);
		
	}
	
	protected boolean thinkBuff()
	{
		if(thinkBuff(new int[] {
			//428, // Inner Rhythm
			268, //	Song of Wind
			267, // Song of Warding
			264, // Song of Earth
			304  // Song of Vitality
		}))
			return true;
		
		return super.thinkBuff();
	}
	
	public List<Integer> getAllowSkill()
	{
		List<Integer> SkillList = new ArrayList<Integer>();
		
		//skill 2nd
		_allowSkills.add(407);	//Psycho Symphony
		_allowSkills.add(98);	//Sword Symphony
		
		//skill 3rd
		_allowSkills.add(455);	//Symbol of Noise		
		
		return SkillList;
	}
	
	@Override
	protected ArrayList<Integer> getDrawTargetSkill()
	{
		ArrayList<Integer>	SkillList	= new ArrayList<Integer>();
		
		_allowSkills.add(28); //Aggression
		_allowSkills.add(402); //Arrest
		
		return SkillList;
	}

	@Override
	public int getRateDAM()
	{
		return 20;
	}

	@Override
	public int getRateSTUN()
	{
		return 0;
	}

	@Override
	public int getRateBUFF()
	{
		return 0;
	}
}

