package Roguelike.GameEvent.OnHit;

import Roguelike.Entity.GameEntity;
import Roguelike.GameEvent.IGameObject;
import Roguelike.Tiles.GameTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;

import java.util.HashMap;

/**
 * Created by Philip on 06-Mar-16.
 */
public class DummyOnHitEvent extends AbstractOnHitEvent
{
	@Override
	public boolean handle( GameEntity entity, GameTile tile, IGameObject parent )
	{
		return true;
	}

	@Override
	public void parse( XmlReader.Element xml )
	{

	}

	@Override
	public Array<String> toString( HashMap<String, Integer> variableMap, IGameObject parent )
	{
		return new Array<String>(  );
	}
}
