package Roguelike.GameEvent.OnHit;

import Roguelike.Entity.GameEntity;
import Roguelike.GameEvent.IGameObject;
import Roguelike.StatusEffect.StatusEffect;
import Roguelike.Tiles.GameTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import exp4j.Helpers.EquationHelper;

import java.util.HashMap;

/**
 * Created by Philip on 06-Mar-16.
 */
public class StatusOnHitEvent extends AbstractOnHitEvent
{
	private String condition;
	private String[] reliesOn;
	public String stacksEqn;
	public XmlReader.Element status;

	@Override
	public boolean handle( GameEntity entity, GameTile tile, IGameObject parent )
	{
		HashMap<String, Integer> variableMap = entity.getVariableMap();
		for ( String name : reliesOn )
		{
			if ( !variableMap.containsKey( name.toLowerCase() ) )
			{
				variableMap.put( name.toLowerCase(), 0 );
			}
		}

		if ( condition != null )
		{
			int conditionVal = EquationHelper.evaluate( condition, variableMap );
			if ( conditionVal == 0 ) { return false; }
		}

		int stacks = 1;

		if ( stacksEqn != null )
		{
			stacks = EquationHelper.evaluate( stacksEqn, variableMap );
		}

		for ( int i = 0; i < stacks; i++ )
		{
			if (tile.entity != null)
			{
				tile.entity.addStatusEffect( StatusEffect.load( status, parent ) );
			}

			if (tile.environmentEntity != null)
			{
				tile.environmentEntity.addStatusEffect( StatusEffect.load( status, parent ) );
			}
		}

		return true;
	}

	@Override
	public void parse( XmlReader.Element xml )
	{
		reliesOn = xml.getAttribute( "ReliesOn", "" ).split( "," );
		condition = xml.getAttribute( "Condition", null );
		if ( condition != null )
		{
			condition = condition.toLowerCase();
		}
		stacksEqn = xml.getAttribute( "Stacks", null );
		if ( stacksEqn != null )
		{
			stacksEqn = stacksEqn.toLowerCase();
		}

		status = xml;
	}

	@Override
	public Array<String> toString( HashMap<String, Integer> variableMap, IGameObject parent )
	{
		Array<String> lines = new Array<String>();

		StatusEffect s = StatusEffect.load( status, parent );

		lines.add( "Spawn a status:" );
		lines.addAll( s.toString( variableMap, false ) );

		return lines;
	}
}
