package Roguelike.Quests.Output;

import Roguelike.Global;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;

/**
 * Created by Philip on 23-Jan-16.
 */
public class QuestOutput
{
	public int reward;
	public String message;

	public String key;
	public String data;

	public Array<AbstractQuestOutputCondition> conditions = new Array<AbstractQuestOutputCondition>(  );

	public boolean evaluate()
	{
		for (AbstractQuestOutputCondition condition : conditions)
		{
			if (!condition.evaluate())
			{
				return false;
			}
		}

		Global.QuestManager.flags.put( key, data );
		System.out.println("Setting world flag: '" + key + "' to '" + data+"'");

		return true;
	}

	public void parse( XmlReader.Element xml )
	{
		reward = xml.getInt( "Reward" );
		message = xml.get( "Message" );

		key = xml.getName().toLowerCase();
		data = xml.get( "Data", "true" ).toLowerCase();

		XmlReader.Element conditionsElement = xml.getChildByName( "Conditions" );
		if (conditionsElement != null)
		{
			for (int i = 0; i < conditionsElement.getChildCount(); i++ )
			{
				XmlReader.Element conditionElement = conditionsElement.getChild( i );
				AbstractQuestOutputCondition condition = AbstractQuestOutputCondition.load( conditionElement );
				conditions.add( condition );
			}
		}
	}
}
