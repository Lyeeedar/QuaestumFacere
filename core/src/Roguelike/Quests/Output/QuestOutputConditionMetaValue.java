package Roguelike.Quests.Output;

import Roguelike.Entity.GameEntity;
import Roguelike.Global;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;

/**
 * Created by Philip on 09-Mar-16.
 */
public class QuestOutputConditionMetaValue extends AbstractQuestOutputCondition
{
	String entityName;
	String metaValue;
	boolean not;

	@Override
	public boolean evaluate()
	{
		Array<GameEntity> entities = new Array<GameEntity>(  );
		Global.CurrentLevel.getAllEntities( entities );

		boolean found = false;
		for (GameEntity entity : entities)
		{
			if (entity.HP > 0 && entity.name.equalsIgnoreCase( entityName ) && entity.tile[0][0].metaValue.contains( metaValue, false ) )
			{
				found = true;
				break;
			}
		}

		if (not)
		{
			found = !found;
		}

		return found;
	}

	@Override
	public void parse( XmlReader.Element xml )
	{
		entityName = xml.getAttribute( "Entity" );
		not = xml.getBooleanAttribute( "Not", false );

		metaValue = xml.getText().toLowerCase();
	}
}
