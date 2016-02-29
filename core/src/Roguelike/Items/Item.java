package Roguelike.Items;

import Roguelike.Ability.AbilityLoader;
import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.Ability.IAbility;
import Roguelike.Ability.PassiveAbility.PassiveAbility;
import Roguelike.AssetManager;
import Roguelike.Entity.Entity;
import Roguelike.Entity.GameEntity;
import Roguelike.GameEvent.GameEventHandler;
import Roguelike.Global;
import Roguelike.Global.Statistic;
import Roguelike.Lights.Light;
import Roguelike.Sound.SoundInstance;
import Roguelike.Sprite.Sprite;
import Roguelike.Sprite.SpriteAnimation.MoveAnimation;
import Roguelike.Tiles.Point;
import Roguelike.UI.Seperator;
import Roguelike.UI.SpriteWidget;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import exp4j.Helpers.EquationHelper;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.io.IOException;
import java.util.HashMap;

public final class Item extends GameEventHandler
{
	public String name = "";
	public String description = "";
	public Sprite hitEffect;
	public EquipmentSlot slot;
	public ItemCategory category;
	public String type = "";
	public boolean canStack;
	public int count = 1;
	public Light light;
	public boolean canDrop = true;
	public String dropChanceEqn;

	public IAbility ability1;
	public IAbility ability2;

	public WeaponDefinition wepDef;
	public int quality = 1;
	public int value;
	public int utilSlots;

	public Array<SpriteGroup> spriteGroups = new Array<SpriteGroup>(  );

	// ----------------------------------------------------------------------
	public Item()
	{

	}

	// ----------------------------------------------------------------------
	@Override
	protected void appendExtraVariables(HashMap<String, Integer> variableMap )
	{
		for (Object[] data : extraData)
		{
			variableMap.put( (String)data[0], (Integer)data[1] );
		}
	}

	// ----------------------------------------------------------------------
	public static Item load( String name )
	{
		Item item = new Item();

		item.internalLoad( name );

		return item;
	}

	// ----------------------------------------------------------------------
	public static Item load( String recipe, Element xml )
	{
		int quality = xml.getInt( "Quality" );

		TreasureGenerator.RecipeData data = TreasureGenerator.recipeList.getData( recipe );

		Item item = Recipe.createRecipe( data.itemTemplate, quality, data.getName( quality ) );

		Element prefixElement = xml.getChildByName( "Prefix" );
		if ( prefixElement != null )
		{
			String[] prefixes = prefixElement.getText().split( "," );

			for (String prefix : prefixes)
			{
				Recipe.applyModifer( item, prefix, quality, true );
			}
		}

		Element suffixElement = xml.getChildByName( "Suffix" );
		if ( suffixElement != null )
		{
			String[] suffixes = suffixElement.getText().split( "," );

			for (String suffix : suffixes)
			{
				Recipe.applyModifer( item, suffix, quality, false );
			}
		}

		item.value = xml.getInt( "Value", 0 );

		return item;
	}

	// ----------------------------------------------------------------------
	public static Item load( Element xml )
	{
		Item item = null;

		if (xml.getChildCount() == 0)
		{
			item = Item.load( xml.getText() );
			item.count = xml.getIntAttribute( "Count", 1 );
		}
		else if ( xml.getChildByName( "Recipe" ) != null )
		{
			String recipe = Global.capitalizeString( xml.getChildByName( "Recipe" ).getText() );
			item = Item.load( recipe, xml );
		}
		else
		{
			item = new Item();
			item.internalLoad( xml );
		}

		item.value = xml.getInt( "Value", 0 );

		return item;
	}

	// ----------------------------------------------------------------------
	public boolean shouldDrop()
	{
		if ( dropChanceEqn == null ) { return true; }

		return EquationHelper.evaluate( dropChanceEqn ) > 0;
	}

	// ----------------------------------------------------------------------
	public Table createTable( Skin skin, GameEntity entity )
	{
		Inventory inventory = entity != null ? entity.getInventory() : null;

		return createTable( skin, slot != null ? inventory.getEquip( slot ) : null );
	}

	// ----------------------------------------------------------------------
	public Table createTable( Skin skin )
	{
		return createTable( skin, this );
	}

	// ----------------------------------------------------------------------
	public Table createTable( Skin skin, Item other )
	{
		if ( slot == EquipmentSlot.WEAPON )
		{
			return createWeaponTable( other, skin );
		}
		else if ( slot != null )
		{
			return createArmourTable( other, skin );
		}

		Table table = new Table();

		table.add( new Label( getName(), skin, "title" ) ).left();
		if ( count > 1 )
		{
			table.add( new Label( "x" + count, skin ) ).left().padLeft( 20 );
		}

		table.row();

		table.add( new Seperator( skin ) ).expandX().fillX();
		table.row();

		Label descLabel = new Label( description, skin );
		descLabel.setWrap( true );
		table.add( descLabel ).expandX().fillX().left();
		table.row();

		return table;
	}

	// ----------------------------------------------------------------------
	private Table createArmourTable( Item other, Skin skin )
	{
		Table table = new Table();
		table.defaults().pad( 5 );

		Table titleRow = new Table();

		titleRow.add( new Label( name, skin, "title" ) ).expandX().left();

		if ( type != null && type.length() > 0 )
		{
			Label label = new Label( type, skin );
			label.setFontScale( 0.7f );
			titleRow.add( label ).expandX().right();
		}

		table.add( titleRow ).expandX().fillX();
		table.row();

		table.add( new Seperator( skin, false ) ).expandX().fillX();
		table.row();

		Label descLabel = new Label( description, skin );
		descLabel.setWrap( true );
		table.add( descLabel ).expandX().fillX().left();
		table.row();

		table.add( new Seperator( skin, false ) ).expandX().fillX();
		table.row();

		for (Statistic stat : Statistic.values())
		{
			int val = getStatistic( Statistic.emptyMap, stat );
			int otherVal = other != null ? other.getStatistic( Statistic.emptyMap, stat ) : 0;

			if (val > 0 || val != otherVal)
			{
				String value = ""+val;

				if (val < otherVal)
				{
					value = value + "[RED] " + (val - otherVal) + "[]";
				}
				else if (val > otherVal)
				{
					value = value + "[GREEN] +" + (val - otherVal) + "[]";
				}

				Label statLabel = new Label( Global.capitalizeString( stat.toString() ) + ": " + value, skin );
				table.add( statLabel ).expandX().left().width( com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth( 1, table ) );
				table.row();
			}
		}

		Array<String> lines = toString( Statistic.emptyMap, true );
		for (String line : lines)
		{
			if (line.equals( "---" ))
			{
				table.add( new Seperator( skin, false ) ).expandX().fillX();
			}
			else
			{
				Label lineLabel = new Label( line, skin );
				lineLabel.setWrap( true );
				table.add( lineLabel ).expandX().left().width( com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth( 1, table ) );
				table.row();
			}

			table.row();
		}

		if (ability1 != null)
		{
			table.add( new Seperator( skin ) ).expandX().fillX();
			table.row();

			Table ability1Table = new Table(  );
			ability1Table.add( new SpriteWidget( ability1.getIcon(), 24, 24 ) );
			ability1Table.add( new Label( ability1.getName(), skin ) );

			table.add( ability1Table ).expandX().left();
			table.row();

			if (ability2 != null)
			{
				Table ability2Table = new Table(  );
				ability2Table.add( new SpriteWidget( ability2.getIcon(), 24, 24 ) );
				ability2Table.add( new Label( ability2.getName(), skin ) );

				table.add( ability2Table ).expandX().left();
				table.row();
			}
		}

		return table;
	}

	// ----------------------------------------------------------------------
	private Table createWeaponTable( Item other, Skin skin )
	{
		Table table = new Table();
		table.defaults().pad( 5 );

		Table titleRow = new Table();

		titleRow.add( new Label( name, skin, "title" ) ).expandX().left();

		if ( type != null && type.length() > 0 )
		{
			Label label = new Label( type, skin );
			label.setFontScale( 0.7f );
			titleRow.add( label ).expandX().right();
		}

		table.add( titleRow ).expandX().fillX();
		table.row();

		table.add( new Seperator( skin ) ).expandX().fillX();
		table.row();

		Label descLabel = new Label( description, skin );
		descLabel.setWrap( true );
		table.add( descLabel ).expandX().fillX().left();
		table.row();

		table.add( new Seperator( skin, false ) ).expandX().fillX();
		table.row();

		table.add( new Label("HitCount: " + wepDef.hitCount, skin) ).expandX().fillX();
		table.row();

		table.add( new Label("HitPercent: " + wepDef.hitPercent, skin) ).expandX().fillX();
		table.row();

		table.add( new Seperator( skin, false ) ).expandX().fillX();
		table.row();

		int oldDam = other != null ? other.getStatistic( Statistic.emptyMap, Statistic.ATTACK ) : 0;
		int newDam = getStatistic( Statistic.emptyMap, Statistic.ATTACK );

		String damText = "Damage: " + newDam;
		if ( newDam != oldDam )
		{
			int diff = newDam - oldDam;

			if ( diff > 0 )
			{
				damText += "[GREEN] +" + diff;
			}
			else
			{
				damText += "[RED] " + diff;
			}
		}

		table.add( new Label( damText, skin ) ).expandX().left();
		table.row();

		table.add( new Seperator( skin, false ) ).expandX().fillX();
		table.row();

		Array<String> lines = toString( Statistic.emptyMap, true );
		for (String line : lines)
		{
			if (line.equals( "---" ))
			{
				table.add( new Seperator( skin, false ) ).expandX().fillX();
			}
			else
			{
				Label lineLabel = new Label( line, skin );
				lineLabel.setWrap( true );
				table.add( lineLabel ).expandX().left().width( com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth( 1, table ) );
				table.row();
			}

			table.row();
		}

		if (ability1 != null)
		{
			table.add( new Seperator( skin ) ).expandX().fillX();
			table.row();

			Table ability1Table = new Table(  );
			ability1Table.add( new SpriteWidget( ability1.getIcon(), 24, 24 ) );
			ability1Table.add( new Label( ability1.getName(), skin ) );

			table.add( ability1Table ).expandX().left();
			table.row();

			if (ability2 != null)
			{
				Table ability2Table = new Table(  );
				ability2Table.add( new SpriteWidget( ability2.getIcon(), 24, 24 ) );
				ability2Table.add( new Label( ability2.getName(), skin ) );

				table.add( ability2Table ).expandX().left();
				table.row();
			}
		}

		return table;
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
		return description;
	}

	// ----------------------------------------------------------------------
	@Override
	public Sprite getIcon()
	{
		if ( spriteGroups.size > 0 )
		{
			for (int i = spriteGroups.size - 1; i >= 0; i--)
			{
				if (spriteGroups.get( i ).stacks < count)
				{
					return spriteGroups.get( i ).sprite;
				}
			}
			return spriteGroups.get( 0 ).sprite;
		}

		if ( spriteGroups.size == 0 )
		{
			Sprite icon = AssetManager.loadSprite( "white" );
			spriteGroups.add( new SpriteGroup( 0, icon ) );
		}
		return spriteGroups.get( 0 ).sprite;
	}

	// ----------------------------------------------------------------------
	private void internalLoad( String name )
	{
		XmlReader xml = new XmlReader();
		Element xmlElement = null;

		try
		{
			xmlElement = xml.parse( Gdx.files.internal( "Items/" + name + ".xml" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		internalLoad( xmlElement );
	}

	// ----------------------------------------------------------------------
	private void internalLoad( Element xmlElement )
	{
		String extendsElement = xmlElement.getAttribute( "Extends", null );
		if ( extendsElement != null )
		{
			internalLoad( extendsElement );
		}

		name = xmlElement.get( "Name", name );
		description = xmlElement.get( "Description", description );
		canStack = xmlElement.getBoolean( "CanStack", canStack );

		String countEqn = xmlElement.get( "Count", "" + count );
		count = EquationHelper.evaluate( countEqn, Statistic.emptyMap );

		Element iconElement = xmlElement.getChildByName( "Icon" );
		if ( iconElement != null )
		{
			if (iconElement.getChildByName( "Name" ) != null)
			{
				Sprite icon = AssetManager.loadSprite( iconElement );

				spriteGroups.add( new SpriteGroup( 0, icon ) );
			}

			for (Element el : iconElement.getChildrenByName( "Sprite" ))
			{
				int count = el.getIntAttribute( "Count" );
				Sprite icon = AssetManager.loadSprite( el );

				spriteGroups.add( new SpriteGroup( count, icon ) );
			}
		}

		Element hitElement = xmlElement.getChildByName( "HitEffect" );
		if ( hitElement != null )
		{
			hitEffect = AssetManager.loadSprite( hitElement );
		}

		Element eventsElement = xmlElement.getChildByName( "Events" );
		if ( eventsElement != null )
		{
			super.parse( eventsElement );
		}

		Element lightElement = xmlElement.getChildByName( "Light" );
		if ( lightElement != null )
		{
			light = Light.load( lightElement );
		}

		String slotsElement = xmlElement.get( "Slot", null );
		if ( slotsElement != null )
		{
			slot = EquipmentSlot.valueOf( slotsElement.toUpperCase() );
		}
		category = xmlElement.get( "Category", null ) != null ? ItemCategory.valueOf( xmlElement.get( "Category" ).toUpperCase() ) : category;
		type = xmlElement.get( "Type", type ).toLowerCase();
		quality = xmlElement.getInt( "Quality", quality );
		value = xmlElement.getInt( "Value", value );
		utilSlots = xmlElement.getInt( "UtilitySlots", utilSlots );

		// Load the wep def
		if (slot == EquipmentSlot.WEAPON && type != null)
		{
			wepDef = WeaponDefinition.load( type );
		}

		Array<Element> abilityElement = xmlElement.getChildrenByName( "Ability" );
		if ( abilityElement.size > 0 )
		{
			ability1 = AbilityLoader.loadAbility( abilityElement.get( 0 ) );
		}

		if ( abilityElement.size > 1 )
		{
			ability2 = AbilityLoader.loadAbility( abilityElement.get( 1 ) );
		}

		// Preload sprites
		if ( type != null )
		{
			getWeaponHitEffect();
			getIcon();
		}
	}

	// ----------------------------------------------------------------------
	public EquipmentSlot getMainSlot()
	{
		return slot;
	}

	// ----------------------------------------------------------------------
	public Sprite getWeaponHitEffect()
	{
		if ( hitEffect != null ) { return hitEffect; }

		if ( wepDef != null && wepDef.hitSprite != null )
		{
			return wepDef.hitSprite;
		}

		return AssetManager.loadSprite( "EffectSprites/Strike/Strike", 0.1f, "Hit" );
	}

	// ----------------------------------------------------------------------
	public enum EquipmentSlot
	{
		WEAPON,
		ARMOUR,
		UTILITY_1,
		UTILITY_2,
		UTILITY_3,
		UTILITY_4;

		public static EquipmentSlot[] UtilitySlots = { EquipmentSlot.UTILITY_1, EquipmentSlot.UTILITY_2, EquipmentSlot.UTILITY_3, EquipmentSlot.UTILITY_4 };
	}

	// ----------------------------------------------------------------------
	public enum ItemCategory
	{
		WEAPON,
		ARMOUR,
		UTILITY,

		TREASURE,
		MISC
	}

	// ----------------------------------------------------------------------
	public static class WeaponDefinition
	{
		public enum HitType
		{
			ALL,
			CLOSEST,
			RANDOM
		}

		public HitType hitType;
		public String hitData;
		public Sprite hitSprite;
		public int hitCount;
		public int hitPercent;

		public Array<Point> hitPoints = new Array<Point>(  );

		public static WeaponDefinition load( String name )
		{
			WeaponDefinition wepDef = new WeaponDefinition();

			name = Global.capitalizeString( name );

			XmlReader reader = new XmlReader();
			XmlReader.Element xml = null;

			try
			{
				xml = reader.parse( Gdx.files.internal( "Items/Weapons/" + name + ".xml" ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			Element spriteElement = xml.getChildByName( "HitSprite" );
			if ( spriteElement != null )
			{
				wepDef.hitSprite = AssetManager.loadSprite( spriteElement );
			}

			String[] hitTypeData = xml.get( "HitType" ).split( "[\\(\\)]" );
			wepDef.hitType = HitType.valueOf( hitTypeData[0].toUpperCase() );
			wepDef.hitData = hitTypeData.length > 1 ? hitTypeData[1] : null;
			wepDef.hitPercent = xml.getInt( "HitPercent", 100 );
			wepDef.hitCount = xml.getInt( "HitCount", 1 );

			Element hitPatternElement = xml.getChildByName( "HitPattern" );

			char[][] hitGrid = new char[hitPatternElement.getChildCount()][];
			Point centralPoint = new Point();

			for (int y = 0; y < hitPatternElement.getChildCount(); y++)
			{
				Element lineElement = hitPatternElement.getChild( y );
				String text = lineElement.getText();

				hitGrid[ y ] = text.toCharArray();

				for (int x = 0; x < hitGrid[ y ].length; x++)
				{
					if (hitGrid[ y ][ x ] == 'x')
					{
						centralPoint.x = x;
						centralPoint.y = y;
					}
				}
			}

			for (int y = 0; y < hitGrid.length; y++)
			{
				for (int x = 0; x < hitGrid[0].length; x++)
				{
					if (hitGrid[y][x] == '#')
					{
						int dx = x - centralPoint.x;
						int dy = centralPoint.y - y;

						wepDef.hitPoints.add( new Point(dx, dy) );
					}
				}
			}

			return wepDef;
		}
	}

	// ----------------------------------------------------------------------
	public static class SpriteGroup
	{
		public int stacks;
		public Sprite sprite;

		public SpriteGroup()
		{

		}

		public SpriteGroup(int stacks, Sprite sprite)
		{
			this.stacks = stacks;
			this.sprite = sprite;
		}
	}
}
