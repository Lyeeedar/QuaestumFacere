package Roguelike.Entity;

import Roguelike.Ability.AbilityLoader;
import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.Ability.IAbility;
import Roguelike.Ability.PassiveAbility.PassiveAbility;
import Roguelike.AssetManager;
import Roguelike.Dialogue.DialogueManager;
import Roguelike.Entity.AI.BehaviourTree.BehaviourTree;
import Roguelike.Entity.Tasks.AbstractTask;
import Roguelike.Fields.Field;
import Roguelike.GameEvent.GameEventHandler;
import Roguelike.Global;
import Roguelike.Global.Direction;
import Roguelike.Global.Passability;
import Roguelike.Global.Statistic;
import Roguelike.Items.Item;
import Roguelike.Items.Item.EquipmentSlot;
import Roguelike.Lights.Light;
import Roguelike.Sprite.Sprite;
import Roguelike.StatusEffect.StatusEffect;
import Roguelike.Tiles.GameTile;
import Roguelike.Tiles.Point;
import Roguelike.Util.EnumBitflag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class GameEntity extends Entity
{
	// ####################################################################//
	// region Constructor

	// ----------------------------------------------------------------------
	public GameEntity()
	{
	}

	// endregion Constructor
	// ####################################################################//
	// region Public Methods

	// ----------------------------------------------------------------------
	public void applyDepthScaling(int difficulty)
	{
		this.difficulty = difficulty;

		for ( Global.Statistic stat : Statistic.ScaleValues )
		{
			int current = statistics.get( stat );

			float scaleVal = difficulty;
			scaleVal /= 5.0f;
			scaleVal += 1;

			int scaledVal = (int) ( (float) current * scaleVal );
			statistics.put( stat, scaledVal );
		}
	}

	// ----------------------------------------------------------------------
	public void attack( Entity other, Direction dir )
	{
		Global.calculateDamage( this, other, getVariable( Statistic.ATTACK ), other.getVariable( Statistic.DEFENSE ), getVariable( Statistic.PENETRATION ), true );
	}

	// ----------------------------------------------------------------------
	@Override
	public void update( float cost )
	{
		if ( inventory.isVariableMapDirty )
		{
			isVariableMapDirty = true;
			inventory.isVariableMapDirty = false;
		}

		actionDelayAccumulator += cost;

		for ( IAbility a : slottedAbilities )
		{
			if (a != null)
			{
				a.onTurn();
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
			if (dialogue.exclamationManager != null)
			{
				dialogue.exclamationManager.update( 1 );
			}
		}
	}

	// ----------------------------------------------------------------------
	public boolean inCombat()
	{
		for ( Point p : visibilityCache.getCurrentShadowCast() )
		{
			GameTile otile = tile[0][0].level.getGameTile(p);
			if (otile.entity != null && !otile.entity.isAllies( this ))
			{
				return true;
			}
		}

		return false;
	}

	// ----------------------------------------------------------------------
	@Override
	public int getStatistic( Statistic stat )
	{
		int val = statistics.get( stat ) + inventory.getStatistic( Statistic.emptyMap, stat );

		HashMap<String, Integer> variableMap = getBaseVariableMap();

		variableMap.put( stat.toString().toLowerCase(), val );

		for ( IAbility a : slottedAbilities )
		{
			if ( a != null && a instanceof PassiveAbility )
			{
				PassiveAbility passive = (PassiveAbility) a;
				val += passive.getStatistic( variableMap, stat );
			}
		}

		for ( int i = 0; i < statusEffects.size; i++ )
		{
			StatusEffect se = statusEffects.get( i );
			val += se.getStatistic( variableMap, stat );
		}

		return val;
	}

	// ----------------------------------------------------------------------
	@Override
	protected void internalLoad( String entity )
	{
		XmlReader xml = new XmlReader();
		Element xmlElement = null;

		try
		{
			xmlElement = xml.parse( Gdx.files.internal( "Entities/" + entity + ".xml" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		internalLoad( xmlElement );
	}

	// ----------------------------------------------------------------------
	protected void internalLoad( Element xmlElement )
	{
		String extendsElement = xmlElement.getAttribute( "Extends", null );
		if ( extendsElement != null )
		{
			internalLoad( extendsElement );
		}

		super.baseInternalLoad( xmlElement );

		defaultHitEffect = xmlElement.getChildByName( "HitEffect" ) != null ? AssetManager.loadSprite( xmlElement.getChildByName( "HitEffect" ) ) : defaultHitEffect;
		AI = xmlElement.getChildByName( "AI" ) != null ? BehaviourTree.load( Gdx.files.internal( "AI/" + xmlElement.get( "AI" ) + ".xml" ) ) : AI;
		canSwap = xmlElement.getBoolean( "CanSwap", canSwap );
		canMove = xmlElement.getBoolean( "CanMove", canMove );
		blood = xmlElement.get( "Blood", "Blood" );

		Element factionElement = xmlElement.getChildByName( "Factions" );
		if ( factionElement != null )
		{
			if (factionElement.getBooleanAttribute( "Override", false ))
			{
				factions.clear();
			}

			for ( Element faction : factionElement.getChildrenByName( "Faction" ) )
			{
				factions.add( faction.getText().toLowerCase() );
			}
		}

		Element abilitiesElement = xmlElement.getChildByName( "Abilities" );
		if ( abilitiesElement != null )
		{
			for ( int i = 0; i < abilitiesElement.getChildCount(); i++ )
			{
				Element abilityElement = abilitiesElement.getChild( i );
				IAbility ability = AbilityLoader.loadAbility( abilityElement );
				ability.setCaster( this );
				slottedAbilities.add( ability );
			}
		}

		Element statusesElement = xmlElement.getChildByName( "Statuses" );
		if ( statusesElement != null )
		{
			for ( int i = 0; i < statusesElement.getChildCount(); i++ )
			{
				Element statusElement = statusesElement.getChild( i );

				StatusEffect effect = StatusEffect.load( statusElement.getText() );
				pendingStatusEffects.add( effect );
			}

			processStatuses();
		}
	}

	// ----------------------------------------------------------------------
	@Override
	public Array<GameEventHandler> getAllHandlers()
	{
		Array<GameEventHandler> handlers = new Array<GameEventHandler>();

		for ( IAbility a : slottedAbilities )
		{
			if ( a != null && a instanceof PassiveAbility )
			{
				handlers.add( (PassiveAbility) a );
			}
		}

		for ( int i = 0; i < statusEffects.size; i++ )
		{
			StatusEffect se = statusEffects.get( i );
			handlers.add( se );
		}

		for ( EquipmentSlot slot : EquipmentSlot.values() )
		{
			Item i = inventory.getEquip( slot );
			if ( i != null )
			{
				handlers.add( i );
			}
		}

		return handlers;
	}

	// ----------------------------------------------------------------------
	@Override
	public void applyDamage( int dam, Entity damager )
	{
		super.applyDamage( dam, damager );

		if (damager != null && damager.tile[0][0] != null)
		{
			AI.setData( "EnemyPos", Global.PointPool.obtain().set( damager.tile[ 0 ][ 0 ].x, damager.tile[ 0 ][ 0 ].y ) );
		}

		if (blood != null && tile[0][0] != null)
		{
			if (dam >= getMaxHP() / 3)
			{
				Direction dir = Direction.values()[ MathUtils.random( Direction.values().length - 1 ) ];
				GameTile bloodTile = tile[ 0 ][ 0 ].level.getGameTile( tile[ 0 ][ 0 ].x + dir.getX(), tile[ 0 ][ 0 ].y + dir.getY() );

				if (bloodTile.getPassable( travelType, this ))
				{
					Field field = Field.load( blood );
					field.spawnSpeed = 0.05f;
					field.trySpawnInTile( bloodTile, 1 );
				}
			}

			if (HP > 0 && dam >= HP)
			{
				Direction dir = Direction.getDirection( damager.tile[0][0], tile[0][0] );

				Point start = new Point(tile[0][0]);
				Array<Point> cone = Direction.buildCone( dir, start, 1 );
				cone.add( start );

				int count = MathUtils.random( 2 );
				for (int i = 0; i < count; i++)
				{
					Point p = cone.removeIndex( MathUtils.random( cone.size - 1 ) );

					GameTile bloodTile = tile[ 0 ][ 0 ].level.getGameTile( p );

					if (bloodTile != null && bloodTile.getPassable( travelType, this ))
					{
						Field field = Field.load( blood );
						field.spawnSpeed = 0.05f;
						field.trySpawnInTile( bloodTile, 1 );
					}
				}
			}
		}

		if (!updatedAbilityDam)
		{
			for (IAbility ab : slottedAbilities)
			{
				if (ab != null)
				{
					ab.onDamaged();
				}
			}
			updatedAbilityDam = true;
		}
	}

	// ----------------------------------------------------------------------
	@Override
	public void applyHealing( int heal )
	{
		super.applyHealing( heal );

		if (!updatedAbilityHeal)
		{
			for (IAbility ab : slottedAbilities)
			{
				if (ab != null)
				{
					ab.onHealed();
				}
			}
			updatedAbilityHeal = true;
		}
	}

	// ----------------------------------------------------------------------
	@Override
	public void removeFromTile()
	{
		for ( int x = 0; x < size; x++ )
		{
			for ( int y = 0; y < size; y++ )
			{
				if ( tile[ x ][ y ] != null && tile[ x ][ y ].entity == this )
				{
					tile[ x ][ y ].entity = null;
				}

				tile[ x ][ y ] = null;
			}
		}
	}

	// ----------------------------------------------------------------------
	public boolean isAllies( GameEntity other )
	{
		for ( String faction : factions )
		{
			if ( other.factions.contains( faction ) ) { return true; }
		}

		return false;
	}

	// ----------------------------------------------------------------------
	public boolean isAllies( HashSet<String> other )
	{
		if ( other == null ) { return false; }

		for ( String faction : factions )
		{
			if ( other.contains( faction ) ) { return true; }
		}

		return false;
	}

	// ----------------------------------------------------------------------
	public float getActionDelay()
	{
		float speed = getStatistic( Statistic.SPEED ) / 10.0f - 1;
		return (float)Math.pow( 0.75, speed );
	}

	// ----------------------------------------------------------------------
	public void getLight( Array<Light> output )
	{
		if ( light != null )
		{
			output.add( light );
		}

//		for ( EquipmentSlot slot : EquipmentSlot.values() )
//		{
//			Item i = inventory.getEquip( slot );
//
//			if ( i != null && i.light != null )
//			{
//				output.add( i.light );
//			}
//		}
	}

	// ----------------------------------------------------------------------
	public static GameEntity load( String name )
	{
		GameEntity e = new GameEntity();
		e.fileName = name;

		e.internalLoad( name );

		e.statistics.put( Statistic.WALK, 1 );

		e.isVariableMapDirty = true;
		e.recalculateMaps();

		e.HP = e.getMaxHP();

		e.isVariableMapDirty = true;
		e.recalculateMaps();

		return e;
	}

	// ----------------------------------------------------------------------
	public static GameEntity load( Array<Element> xml )
	{
		GameEntity e = new GameEntity();
		e.xmlData = xml;

		for ( Element el : xml )
		{
			e.internalLoad( el );
		}

		e.statistics.put( Statistic.WALK, 1 );

		e.isVariableMapDirty = true;
		e.recalculateMaps();

		e.HP = e.getMaxHP();

		e.isVariableMapDirty = true;
		e.recalculateMaps();

		return e;
	}

	// ----------------------------------------------------------------------
	public EnumBitflag<Passability> getTravelType()
	{
		recalculateMaps();
		return travelType;
	}

	// endregion Public Methods
	// ####################################################################//
	// region Data

	// ----------------------------------------------------------------------
	public int difficulty;

	// ----------------------------------------------------------------------
	public boolean seen = false;
	public String fileName;
	public Array<Element> xmlData;

	// ----------------------------------------------------------------------
	public Array<IAbility> slottedAbilities = new Array<IAbility>();

	// ----------------------------------------------------------------------
	public Sprite defaultHitEffect = AssetManager.loadSprite( "EffectSprites/Strike/Strike", 0.1f, "Hit" );

	// ----------------------------------------------------------------------
	public Array<AbstractTask> tasks = new Array<AbstractTask>();
	public float actionDelayAccumulator;
	public boolean canSwap;
	public boolean canMove = true;

	// ----------------------------------------------------------------------
	public HashSet<String> factions = new HashSet<String>();
	public BehaviourTree AI;

	// ----------------------------------------------------------------------
	public boolean updatedAbilityDam = false;
	public boolean updatedAbilityHeal = false;

	// ----------------------------------------------------------------------
	public String blood;

	// ----------------------------------------------------------------------
	public Point spawnPos;

	// endregion Data
	// ####################################################################//
}
