package Roguelike.Entity;

import java.util.HashMap;
import java.util.Iterator;

import Roguelike.AssetManager;
import Roguelike.GameEvent.AdditionalSprite;
import Roguelike.GameEvent.IGameObject;
import Roguelike.Global;
import Roguelike.Global.Direction;
import Roguelike.Global.Passability;
import Roguelike.Global.Statistic;
import Roguelike.GameEvent.GameEventHandler;
import Roguelike.Items.Inventory;
import Roguelike.Items.Item;
import Roguelike.Items.Item.EquipmentSlot;
import Roguelike.Lights.Light;
import Roguelike.Pathfinding.ShadowCastCache;
import Roguelike.Sprite.TilingSprite;
import Roguelike.Sprite.Sprite;
import Roguelike.StatusEffect.StatusEffect;
import Roguelike.Tiles.GameTile;
import Roguelike.UI.Message;
import Roguelike.Util.EnumBitflag;
import Roguelike.Util.FastEnumMap;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

public abstract class Entity implements IGameObject
{
	// ----------------------------------------------------------------------
	public abstract void update( float delta );

	// ----------------------------------------------------------------------
	public abstract int getStatistic( Statistic stat );

	// ----------------------------------------------------------------------
	protected abstract void internalLoad( String file );

	// ----------------------------------------------------------------------
	public abstract Array<GameEventHandler> getAllHandlers();

	// ----------------------------------------------------------------------
	protected void baseInternalLoad( Element xml )
	{
		name = xml.get( "Name", name );

		size = xml.getInt( "Size", size );
		if ( size < 1 )
		{
			size = 1;
		}

		tile = new GameTile[size][size];

		quality = xml.getInt( "Quality", quality );

		Element spriteElement = xml.getChildByName( "Sprite" );
		if ( spriteElement != null )
		{
			sprite = AssetManager.loadSprite( xml.getChildByName( "Sprite" ) );
		}

		if ( sprite != null )
		{
			sprite.size[0] = size;
			sprite.size[1] = size;
		}

		Element raisedSpriteElement = xml.getChildByName( "TilingSprite" );
		if ( raisedSpriteElement != null )
		{
			tilingSprite = TilingSprite.load( raisedSpriteElement );
		}

		if ( tilingSprite != null )
		{
			//for (Sprite sprite : tilingSprite.sprites)
			//{
			//	sprite.size = size;
			//}
		}

		Element lightElement = xml.getChildByName( "Light" );
		if ( lightElement != null )
		{
			light = Roguelike.Lights.Light.load( lightElement );
		}

		Element statElement = xml.getChildByName( "Statistics" );
		if ( statElement != null )
		{
			Statistic.load( statElement, statistics );
			HP = getMaxHP();

			statistics.put( Statistic.WALK, 1 );
			//statistics.put( Statistic.ENTITY, 1 );
		}

		Element inventoryElement = xml.getChildByName( "Inventory" );
		if ( inventoryElement != null )
		{
			inventory.load( inventoryElement );
		}

		Element immuneElement = xml.getChildByName( "Immune" );
		if ( immuneElement != null )
		{
			immune = immuneElement.getText().toLowerCase().split( "," );
		}

		canTakeDamage = xml.getBoolean( "CanTakeDamage", canTakeDamage );

		UID = getClass().getSimpleName() + " " + name + ": ID " + hashCode();
	}

	// ----------------------------------------------------------------------
	public int getVariable( Statistic stat )
	{
		return getVariableMap().get( stat.toString().toLowerCase() );
	}

	// ----------------------------------------------------------------------
	private FastEnumMap<Statistic, Integer> getStatistics()
	{
		FastEnumMap<Statistic, Integer> newMap = new FastEnumMap<Statistic, Integer>( Statistic.class );

		for ( Statistic stat : Statistic.values() )
		{
			newMap.put( stat, getStatistic( stat ) );
		}

		return newMap;
	}

	// ----------------------------------------------------------------------
	public HashMap<String, Integer> getVariableMap()
	{
		recalculateMaps();
		return variableMap;
	}

	// ----------------------------------------------------------------------
	public HashMap<String, Integer> getBaseVariableMap()
	{
		recalculateMaps();
		return baseVariableMap;
	}

	// ----------------------------------------------------------------------
	public void recalculateMaps()
	{
		if ( isVariableMapDirty )
		{
			isVariableMapDirty = false;
			calculateBaseVariableMap();
			calculateVariableMap();
			travelType = Passability.variableMapToTravelType( variableMap );
		}
	}

	// ----------------------------------------------------------------------
	protected void calculateVariableMap()
	{
		variableMap.clear();

		for ( Statistic s : Statistic.values() )
		{
			variableMap.put( s.toString().toLowerCase(), statistics.get( s ) );
		}

		for ( GameEventHandler handler : getAllHandlers() )
		{
			for ( Statistic s : Statistic.values() )
			{
				String key = s.toString().toLowerCase();
				variableMap.put( key, variableMap.get( key ) + handler.getStatistic( baseVariableMap, s ) );
			}
		}

		variableMap.put( "hp", HP );

		for ( EquipmentSlot slot : EquipmentSlot.values() )
		{
			Item equipped = inventory.getEquip( slot );
			if ( equipped != null && equipped.type != null )
			{
				variableMap.put( equipped.type, 1 );
			}
		}

		for ( StatusEffectStack s : stacks )
		{
			variableMap.put( s.effect.name.toLowerCase(), s.count );
		}

		for ( Statistic s : Statistic.values() )
		{
			variableMap.put( s.toString().toLowerCase(), getStatistic( s ) );
		}

		variableMap.put( "maxhp", variableMap.get( Statistic.VITALITY.toString().toLowerCase() ) * 10 );

		if (inventory.getEquip( EquipmentSlot.WEAPON ) != null)
		{
			Item wep = inventory.getEquip( EquipmentSlot.WEAPON );

			int atk = Global.calculateScaledAttack( Statistic.statsBlockToVariableBlock( wep.getStatistics( variableMap ) ), variableMap );

			String key = Statistic.ATTACK.toString().toLowerCase();
			variableMap.put( key, variableMap.get( key ) + atk );
		}
	}

	// ----------------------------------------------------------------------
	protected void calculateBaseVariableMap()
	{
		baseVariableMap.clear();

		for ( Statistic s : Statistic.values() )
		{
			baseVariableMap.put( s.toString().toLowerCase(), statistics.get( s ) + inventory.getStatistic( Statistic.emptyMap, s ) );
		}

		baseVariableMap.put( "maxhp", baseVariableMap.get( Statistic.VITALITY.toString().toLowerCase() ) * 10 );
		baseVariableMap.put( "hp", HP );

		for ( EquipmentSlot slot : EquipmentSlot.values() )
		{
			Item equipped = inventory.getEquip( slot );
			if ( equipped != null && equipped.type != null )
			{
				baseVariableMap.put( equipped.type, 1 );
			}
		}

		for ( StatusEffectStack s : stacks )
		{
			baseVariableMap.put( s.effect.name.toLowerCase(), s.count );
		}
	}

	// ----------------------------------------------------------------------
	public Inventory getInventory()
	{
		return inventory;
	}

	// ----------------------------------------------------------------------
	public void applyDamage( int dam, Entity damager )
	{
		if ( !canTakeDamage || HP == 0 ) { return; }

		HP = Math.max( HP - dam, 0 );

		if ( HP == 0 )
		{
			for ( GameEventHandler handler : getAllHandlers() )
			{
				handler.onDeath( this, damager );
			}
		}

		damageAccumulator += dam;
		hasDamage = true;
		extraUIHP += dam;

		if (HP + extraUIHP > getMaxHP() )
		{
			extraUIHP = getMaxHP() - HP;
		}

		if ( dam != 0 )
		{
			isVariableMapDirty = true;
		}
	}

	// ----------------------------------------------------------------------
	public void applyHealing( int heal )
	{
		if ( !canTakeDamage ) { return; }

		int appliedHeal = Math.min( heal, getMaxHP() - HP );
		HP += appliedHeal;
		extraUIHP -= appliedHeal;
		if (extraUIHP < 0) { extraUIHP = 0; }

		healingAccumulator += appliedHeal;

		if ( heal != 0 )
		{
			isVariableMapDirty = true;
		}
	}

	// ----------------------------------------------------------------------
	public int getMaxHP()
	{
		return getVariable( Statistic.VITALITY ) * 10;
	}

	// ----------------------------------------------------------------------
	public Array<StatusEffectStack> stackStatusEffects()
	{
		Array<StatusEffectStack> stacks = new Array<StatusEffectStack>();

		for ( StatusEffect se : statusEffects )
		{
			boolean found = false;
			for ( StatusEffectStack stack : stacks )
			{
				if ( stack.effect.name.equals( se.name ) )
				{
					stack.count++;
					found = true;
					break;
				}
			}

			if ( !found )
			{
				StatusEffectStack stack = new StatusEffectStack();
				stack.count = 1;
				stack.effect = se;

				stacks.add( stack );
			}
		}

		return stacks;
	}

	// ----------------------------------------------------------------------
	public boolean isImmune( String name )
	{
		if ( immune == null ) { return false; }

		for ( String s : immune )
		{
			if ( s.equalsIgnoreCase( name ) )
			{
				return true;
			}
		}

		return false;
	}

	// ----------------------------------------------------------------------
	public void addStatusEffect( StatusEffect se )
	{
		if ( !canTakeDamage ) { return; }
		if ( isImmune( se.getName().toLowerCase() ) ) { return; }

		pendingStatusEffects.add( se );
	}

	// ----------------------------------------------------------------------
	public void processStatuses()
	{
		processPendingStatuses();

		boolean removedStatusEffect = false;
		Iterator<StatusEffect> itr = statusEffects.iterator();
		while ( itr.hasNext() )
		{
			StatusEffect se = itr.next();

			if ( se.duration <= 0 )
			{
				se.onExpire( this );
				itr.remove();
				isVariableMapDirty = true;
			}
		}

		if (removedStatusEffect)
		{
			stacks = stackStatusEffects();
		}

		processPendingStatuses();

		replacementSprite = null;
		additionalSprites.clear();

		for (StatusEffect se : statusEffects)
		{
			if (se.replacementSprite != null)
			{
				replacementSprite = se.replacementSprite;
			}
			for (AdditionalSprite as : se.additionalSprites)
			{
				additionalSprites.add( as );
			}
		}
	}

	// ----------------------------------------------------------------------
	public void processPendingStatuses()
	{
		if (pendingStatusEffects.size > 0)
		{
			for (StatusEffect se : pendingStatusEffects)
			{
				boolean needsAdding = true;
				if (!se.stackable)
				{
					for (StatusEffect ose : statusEffects)
					{
						if (ose.name.equals( se.name ))
						{
							ose.duration = Math.max( ose.duration, se.duration );
							needsAdding = false;
							break;
						}
					}
				}

				if ( needsAdding )
				{
					statusEffects.add( se );
				}
			}

			stacks = stackStatusEffects();
			isVariableMapDirty = true;

			pendingStatusEffects.clear();
		}
	}

	// ----------------------------------------------------------------------
	public void removeStatusEffect( String se )
	{
		for ( int i = 0; i < statusEffects.size; i++ )
		{
			if ( statusEffects.get( i ).name.equals( se ) )
			{
				statusEffects.removeIndex( i );
				isVariableMapDirty = true;

				break;
			}
		}

		stacks = stackStatusEffects();
	}

	// ----------------------------------------------------------------------
	public void setPopupText( String text, float duration )
	{
		if (popup != null && popup.equals( text ))
		{
			return;
		}

		popup = text;
		displayedPopup = "";
		popupDuration = duration;
		popupFade = 1;
		popupAccumulator = 0;
	}

	// ----------------------------------------------------------------------
	public abstract void removeFromTile();

	// ----------------------------------------------------------------------
	public void updateShadowCast()
	{
		visibilityCache.getShadowCast( tile[0][0].level.Grid, tile[0][0].x, tile[0][0].y, getStatistic( Statistic.SIGHT ), this );
	}

	// ----------------------------------------------------------------------
	@Override
	public String getName()
	{
		return name;
	}

	// ----------------------------------------------------------------------
	@Override
	public String getDescription()
	{
		return "";
	}

	// ----------------------------------------------------------------------
	@Override
	public Sprite getIcon()
	{
		return sprite;
	}

	// ----------------------------------------------------------------------
	public Sprite replacementSprite;
	public Array<AdditionalSprite> additionalSprites = new Array<AdditionalSprite>(  );

	// ----------------------------------------------------------------------
	public boolean weaponSheathed = false;

	// ----------------------------------------------------------------------
	public boolean isVariableMapDirty = true;

	// ----------------------------------------------------------------------
	protected HashMap<String, Integer> baseVariableMap = new HashMap<String, Integer>();
	protected HashMap<String, Integer> variableMap = new HashMap<String, Integer>();
	protected EnumBitflag<Passability> travelType = new EnumBitflag<Passability>();

	// ----------------------------------------------------------------------
	public int damageAccumulator = 0;
	public int healingAccumulator = 0;
	public boolean hasDamage = false;

	// ----------------------------------------------------------------------
	public Array<Message> pendingMessages = new Array<Message>(  );
	public float messageAccumulator = 0;
	public static final float MESSAGE_DELAY = 0.4f;

	// ----------------------------------------------------------------------
	public String[] immune;
	public FastEnumMap<Statistic, Integer> statistics = Statistic.getStatisticsBlock();
	public Array<StatusEffect> statusEffects = new Array<StatusEffect>( false, 16 );
	public Array<StatusEffect> pendingStatusEffects = new Array<StatusEffect>(  );
	public Array<StatusEffectStack> stacks = new Array<StatusEffectStack>();
	public Inventory inventory = new Inventory();

	// ----------------------------------------------------------------------
	public String name = "";

	// ----------------------------------------------------------------------
	public String popup;
	public String displayedPopup;
	public float popupDuration = 0;
	public float popupFade = 0;
	public float popupAccumulator = 0;

	// ----------------------------------------------------------------------
	public Sprite sprite;
	public TilingSprite tilingSprite;
	public Light light;

	// ----------------------------------------------------------------------
	public GameTile[][] tile = new GameTile[1][1];
	public int size = 1;
	public Direction location = Direction.CENTER;

	// ----------------------------------------------------------------------
	public int HP = 1;
	public int extraUIHP = 0;
	public float extraUIHPAccumulator = 0;

	// ----------------------------------------------------------------------
	public boolean canTakeDamage = true;

	// ----------------------------------------------------------------------
	public int quality = 1;

	// ----------------------------------------------------------------------
	public String UID;

	// ----------------------------------------------------------------------
	public ShadowCastCache visibilityCache = new ShadowCastCache(  );

	// ----------------------------------------------------------------------
	public static class StatusEffectStack
	{
		public StatusEffect effect;
		public int count;
	}

}
