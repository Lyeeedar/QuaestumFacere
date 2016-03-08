package Roguelike.Entity.ActivationAction;

import Roguelike.Entity.Entity;
import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Entity.GameEntity;
import Roguelike.Global;
import Roguelike.Sprite.SpriteAnimation.StretchAnimation;
import Roguelike.Tiles.GameTile;
import com.badlogic.gdx.utils.XmlReader;

/**
 * Created by Philip on 25-Jan-16.
 */
public class ActivationActionSpawnEntity extends AbstractActivationAction
{
	public String entityName;
	public int numToSpawn;
	public float delay;

	public float spawnAccumulator;

	public ActivationActionSpawnEntity()
	{

	}

	public ActivationActionSpawnEntity(String entity, int numToSpawn, float delay)
	{
		this.entityName = entity;
		this.numToSpawn = numToSpawn;
		this.delay = delay;
	}

	@Override
	public void evaluate( EnvironmentEntity owningEntity, Entity activatingEntity, float delta )
	{
		if (numToSpawn == 0) { return; }

		spawnAccumulator += delta;

		if (spawnAccumulator >= delay)
		{
			spawnAccumulator = 0;

			GameEntity ge = GameEntity.load( entityName );

			GameTile tile = owningEntity.tile[0][0];
			int x = tile.x;
			int y = tile.y;

			GameTile spawnTile = null;

			for ( Global.Direction d : Global.Direction.values() )
			{
				int nx = x + d.getX();
				int ny = y + d.getY();

				GameTile ntile = tile.level.getGameTile( nx, ny );

				if ( ntile != null && ntile.getPassable( ge.getTravelType(), null ) && ntile.entity == null )
				{
					spawnTile = ntile;
					break;
				}
			}

			if (spawnTile != null)
			{
				ge.sprite.spriteAnimation = new StretchAnimation( 0.2f, null, 0, StretchAnimation.StretchEquation.EXPAND );
				spawnTile.addGameEntity( ge );
				numToSpawn--;

				System.out.println( "Spawned Entity: " + ge.name );
			}
		}
	}

	@Override
	public void parse( XmlReader.Element xml )
	{
		entityName = xml.getText();
		numToSpawn = xml.getIntAttribute( "Num", 1 );
		delay = xml.getFloatAttribute( "Delay", 0 );
	}
}
