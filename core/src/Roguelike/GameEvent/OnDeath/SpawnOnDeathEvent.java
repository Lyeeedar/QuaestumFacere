package Roguelike.GameEvent.OnDeath;

import Roguelike.Entity.Entity;
import Roguelike.Entity.GameEntity;
import Roguelike.Global;
import Roguelike.Tiles.GameTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import exp4j.Helpers.EquationHelper;

import java.util.HashMap;

/**
 * Created by Philip on 06-Mar-16.
 */
public class SpawnOnDeathEvent extends AbstractOnDeathEvent
{
	String condition;
	String countEqn;
	String[] reliesOn;
	String entityName;

	@Override
	public boolean handle( Entity entity, Entity killer )
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

		int count = 1;

		if ( countEqn != null )
		{
			count = EquationHelper.evaluate( countEqn, variableMap );
		}

		for (int i = 0; i < count; i++)
		{
			Array<Global.Direction> valid = new Array<Global.Direction>( Global.Direction.values() );

			while (valid.size > 0)
			{
				Global.Direction dir = valid.removeIndex( MathUtils.random( valid.size-1 ) );

				GameEntity newEntity = GameEntity.load( entityName );
				GameTile newTile = entity.tile[ 0 ][ 0 ].level.getGameTile( entity.tile[ 0 ][ 0 ].x + dir.getX(), entity.tile[ 0 ][ 0 ].y + dir.getY() );

				if ( newTile.entity == null && newTile.getPassable( newEntity.getTravelType(), newEntity ) )
				{
					newTile.addGameEntity( newEntity );
				}
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
		countEqn = xml.getAttribute( "Count", null );
		if ( countEqn != null )
		{
			countEqn = countEqn.toLowerCase();
		}

		entityName = xml.getText();
	}

	@Override
	public Array<String> toString( HashMap<String, Integer> variableMap )
	{
		Array<String> lines = new Array<String>(  );

		lines.add("Spawns " + entityName);

		return lines;
	}
}
