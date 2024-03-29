package Roguelike.Entity;

import java.util.HashMap;

import Roguelike.AssetManager;
import Roguelike.Entity.ActivationAction.*;
import Roguelike.Global;
import Roguelike.Global.Passability;
import Roguelike.Global.Statistic;
import Roguelike.GameEvent.GameEventHandler;
import Roguelike.Sprite.TilingSprite;
import Roguelike.Sprite.Sprite;
import Roguelike.StatusEffect.StatusEffect;
import Roguelike.Tiles.GameTile;
import Roguelike.Tiles.Point;
import Roguelike.Util.EnumBitflag;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

public class EnvironmentEntity extends Entity
{
	public boolean attachToWall = false;
	public boolean overHead = false;

	public boolean forceKill = false;

	public EnumBitflag<Passability> passableBy;

	public Array<ActivationActionGroup> onActivateActions = new Array<ActivationActionGroup>(  );
	public Array<ActivationActionGroup> onTurnActions = new Array<ActivationActionGroup>(  );
	public Array<ActivationActionGroup> onHearActions = new Array<ActivationActionGroup>(  );
	public Array<ActivationActionGroup> onDeathActions = new Array<ActivationActionGroup>(  );
	public Array<ActivationActionGroup> noneActions = new Array<ActivationActionGroup>(  );
	public Array<ActivationActionGroup> proximityActions = new Array<ActivationActionGroup>(  );

	// ----------------------------------------------------------------------
	@Override
	public void update( float cost )
	{
		for (ActivationActionGroup group : onTurnActions)
		{
			if (group.enabled)
			{
				group.activate( this, this, cost );
			}
		}

		for ( GameEventHandler h : getAllHandlers() )
		{
			h.onTurn( this, 1 );
		}

		processStatuses();

		if ( popupDuration > 0 )
		{
			popupDuration -= 1;
		}

		if (dialogue != null)
		{
			dialogue.exclamationManager.update( cost );

			Array<Point> current = visibilityCache.getShadowCast( tile[0][0].level.Grid, tile[0][0].x, tile[0][0].y, 5, this );
			Array<GameTile> tiles = new Array<GameTile>(  );
			for (Point p : current)
			{
				tiles.add( tile[0][0].level.getGameTile( p ) );
			}

			dialogue.exclamationManager.process( tiles, this );
		}
	}

	// ----------------------------------------------------------------------
	@Override
	public int getStatistic( Statistic stat )
	{
		int val = statistics.get( stat );

		HashMap<String, Integer> variableMap = getBaseVariableMap();

		for ( StatusEffect se : statusEffects )
		{
			val += se.getStatistic( variableMap, stat );
		}

		return val;
	}

	// ----------------------------------------------------------------------
	@Override
	protected void internalLoad( String file )
	{

	}

	// ----------------------------------------------------------------------
	@Override
	public void removeFromTile()
	{
		for ( int x = 0; x < size; x++ )
		{
			for ( int y = 0; y < size; y++ )
			{
				if ( tile[x][y] != null )
				{
					tile[x][y].environmentEntity = null;
					tile[x][y] = null;
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	@Override
	public Array<GameEventHandler> getAllHandlers()
	{
		Array<GameEventHandler> handlers = new Array<GameEventHandler>();

		for ( StatusEffect se : statusEffects )
		{
			handlers.add( se );
		}

		return handlers;
	}

	// ----------------------------------------------------------------------
	private static EnvironmentEntity CreateDoor( final Element xml )
	{
		Sprite doorHClosed = AssetManager.loadSprite( "Oryx/Custom/terrain/door_wood_h_closed", true );
		Sprite doorHOpen = AssetManager.loadSprite( "Oryx/Custom/terrain/door_wood_h_open", true );

		Sprite doorVClosed = AssetManager.loadSprite( "Oryx/Custom/terrain/door_wood_v_closed", true );
		Sprite doorVOpen = AssetManager.loadSprite( "Oryx/Custom/terrain/door_wood_v_open", true );

		Element hClosedElement = xml.getChildByName( "HClosed" );
		if (hClosedElement != null)
		{
			doorHClosed = AssetManager.loadSprite( hClosedElement );
		}

		Element hOpenElement = xml.getChildByName( "HOpen" );
		if (hOpenElement != null)
		{
			doorHOpen = AssetManager.loadSprite( hOpenElement );
		}

		Element vClosedElement = xml.getChildByName( "VClosed" );
		if (vClosedElement != null)
		{
			doorVClosed = AssetManager.loadSprite( vClosedElement );
		}

		Element vOpenElement = xml.getChildByName( "VOpen" );
		if (vOpenElement != null)
		{
			doorVOpen = AssetManager.loadSprite( vOpenElement );
		}

		final TilingSprite closedSprite = new TilingSprite(doorVClosed, doorHClosed);
		closedSprite.checkID = "wall".hashCode();

		final TilingSprite openSprite = new TilingSprite(doorVOpen, doorHOpen);
		openSprite.checkID = "wall".hashCode();

		EnvironmentEntity entity = new EnvironmentEntity();
		entity.passableBy = Passability.parse( "false" );
		entity.passableBy.clearBit( Passability.LIGHT );
		entity.tilingSprite = closedSprite;
		entity.canTakeDamage = false;

		String lockedBy = xml.get( "LockedBy", null );
		if (lockedBy != null)
		{
			ActivationActionGroup unlock = new ActivationActionGroup();
			unlock.name = "Unlock";
			unlock.enabled = true;
			unlock.conditions.add( new ActivationConditionHasItem( lockedBy, 1 ) );
			unlock.actions.add( new ActivationActionRemoveItem( lockedBy, 1 ) );
			unlock.actions.add( new ActivationActionSetEnabled( null, "Open", true ) );
			unlock.actions.add( new ActivationActionSetEnabled( null, "Unlock", false ) );

			entity.onActivateActions.add( unlock );

			ActivationActionGroup open = new ActivationActionGroup();
			open.name = "Open";
			open.enabled = false;
			open.actions.add( new ActivationActionSetSprite( null, openSprite ) );
			open.actions.add( new ActivationActionSetPassable( Passability.parse( "true" ) ) );
			open.actions.add( new ActivationActionSetEnabled( null, "Open", false ) );

			entity.onActivateActions.add( open );
		}
		else
		{
			ActivationActionGroup open = new ActivationActionGroup();
			open.name = "Open";
			open.enabled = true;
			open.actions.add( new ActivationActionSetSprite( null, openSprite ) );
			open.actions.add( new ActivationActionSetPassable( Passability.parse( "true" ) ) );
			open.actions.add( new ActivationActionSetEnabled( null, "Open", false ) );

			entity.onActivateActions.add( open );
		}

		return entity;
	}

	// ----------------------------------------------------------------------
	private static EnvironmentEntity CreateBasic( Element xml )
	{
		EnvironmentEntity entity = new EnvironmentEntity();

		entity.passableBy = Passability.parse( xml.get( "Passable", "false" ) );

		if ( xml.get( "Opaque", null ) != null )
		{
			boolean opaque = xml.getBoolean( "Opaque", false );

			if ( opaque )
			{
				entity.passableBy.clearBit( Passability.LIGHT );
			}
			else
			{
				entity.passableBy.setBit( Passability.LIGHT );
			}
		}

		entity.attachToWall = xml.getBoolean( "AttachToWall", false );
		entity.overHead = xml.getBoolean( "Overhead", false );
		entity.canTakeDamage = xml.getChildByName( "Statistics" ) != null;

		entity.baseInternalLoad( xml );

		loadActions(xml.getChildByName( "OnTurn" ), entity.onTurnActions);
		loadActions(xml.getChildByName( "OnActivate" ), entity.onActivateActions);
		loadActions(xml.getChildByName( "OnHear" ), entity.onHearActions);
		loadActions(xml.getChildByName( "OnDeath" ), entity.onDeathActions);
		loadActions( xml.getChildByName( "Actions" ), entity.noneActions );
		loadActions( xml.getChildByName( "Proximity" ), entity.proximityActions );

		return entity;
	}

	// ----------------------------------------------------------------------
	public void getAllActivationActions(Array<ActivationActionGroup> output)
	{
		output.addAll( onActivateActions );
		output.addAll( onTurnActions );
		output.addAll( onHearActions );
		output.addAll( onDeathActions );
		output.addAll( noneActions );
		output.addAll( proximityActions );
	}

	// ----------------------------------------------------------------------
	private static void loadActions(Element xml, Array<ActivationActionGroup> actionList)
	{
		if (xml != null)
		{
			for (int i = 0; i < xml.getChildCount(); i++)
			{
				Element el = xml.getChild( i );
				ActivationActionGroup group = new ActivationActionGroup(  );
				group.parse( el );

				actionList.add( group );
			}
		}
	}

	// ----------------------------------------------------------------------
	public static EnvironmentEntity load( Element xml )
	{
		EnvironmentEntity entity = null;

		String type = xml.get( "Type", "" );

		if ( type.equalsIgnoreCase( "Door" ) )
		{
			entity = CreateDoor( xml );
		}
		else
		{
			entity = CreateBasic( xml );
		}
		entity.tile = new GameTile[entity.size][entity.size];
		return entity;
	}
}
