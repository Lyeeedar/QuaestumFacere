package Roguelike.Entity.ActivationAction;

import Roguelike.AssetManager;
import Roguelike.Entity.Entity;
import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Global;
import Roguelike.Sprite.Sprite;
import Roguelike.Sprite.TilingSprite;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;

/**
 * Created by Philip on 25-Jan-16.
 */
public class ActivationActionSetSprite extends AbstractActivationAction
{
	public String entityName;
	public int maxDist;

	public Sprite sprite;
	public TilingSprite tilingSprite;

	public ActivationActionSetSprite()
	{

	}

	public ActivationActionSetSprite( Sprite sprite, TilingSprite tilingSprite )
	{
		this.sprite = sprite;
		this.tilingSprite = tilingSprite;
	}

	@Override
	public void evaluate( EnvironmentEntity owningEntity, Entity activatingEntity, float delta )
	{
		if (entityName != null)
		{
			Array<EnvironmentEntity> all = new Array<EnvironmentEntity>(  );
			owningEntity.tile[0][0].level.getAllEnvironmentEntities( all );

			for (EnvironmentEntity ee : all)
			{
				if ( ee.name.equals( entityName ) && Global.TaxiDist(owningEntity.tile[0][0], ee.tile[0][0]) <= maxDist )
				{
					ee.sprite = sprite;
					ee.tilingSprite = tilingSprite;
				}
			}
		}
		else
		{
			owningEntity.sprite = sprite;
			owningEntity.tilingSprite = tilingSprite;
		}
	}

	@Override
	public void parse( XmlReader.Element xml )
	{
		entityName = xml.getAttribute( "Entity", null );
		maxDist = xml.getIntAttribute( "MaxDist", Integer.MAX_VALUE );

		XmlReader.Element spriteElement = xml.getChildByName( "Sprite" );
		if ( spriteElement != null )
		{
			sprite = AssetManager.loadSprite( xml.getChildByName( "Sprite" ) );
		}

		XmlReader.Element raisedSpriteElement = xml.getChildByName( "TilingSprite" );
		if ( raisedSpriteElement != null )
		{
			tilingSprite = TilingSprite.load( raisedSpriteElement );
		}
	}
}
