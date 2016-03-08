package Roguelike.Quests.Output;

import Roguelike.Global;
import com.badlogic.gdx.utils.XmlReader;

/**
 * Created by Philip on 08-Mar-16.
 */
public class QuestOutputConditionHasItem extends AbstractQuestOutputCondition
{
	public String name;
	public int count;
	public boolean not;

	@Override
	public boolean evaluate()
	{
		boolean has = Global.CurrentLevel.player.inventory.getItemCount( name ) >= count;

		if (has)
		{
			return !not;
		}
		else
		{
			return not;
		}
	}

	@Override
	public void parse( XmlReader.Element xml )
	{
		name = xml.getText();
		count = xml.getIntAttribute( "Count", 1 );
		not = xml.getBooleanAttribute( "Not", false );
	}
}
