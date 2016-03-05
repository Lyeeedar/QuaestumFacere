package Roguelike.Items;

import Roguelike.Ability.AbilityLoader;
import Roguelike.Ability.IAbility;
import Roguelike.Sprite.Sprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.XmlReader;
import exp4j.Helpers.EquationHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by Philip on 22-Dec-15.
 */
public class TreasureGenerator
{
	public static Array<Item> generateLoot( int quality, String typeBlock, Random ran )
	{
		Array<Item> items = new Array<Item>(  );
		String[] types = typeBlock.toLowerCase().split( "," );

		for (String type : types)
		{
			if ( type.equals( "currency" ) )
			{
				items.addAll( TreasureGenerator.generateCurrency( quality, ran ) );
			}
			else if ( type.equals( "armour" ) )
			{
				items.addAll( TreasureGenerator.generateArmour( quality, ran ) );
			}
			else if ( type.equals( "weapon" ) )
			{
				items.addAll( TreasureGenerator.generateWeapon( quality, ran ) );
			}
			else if ( type.equals( "utility" ) )
			{
				items.addAll( TreasureGenerator.generateUtility( quality, ran ) );
			}
			else if ( type.equals( "item" ) )
			{
				if ( ran.nextBoolean() )
				{
					items.addAll( TreasureGenerator.generateArmour( quality, ran ) );
				}
				else
				{
					items.addAll( TreasureGenerator.generateWeapon( quality, ran ) );
				}
			}
			else if ( type.equals( "random" ) )
			{
				items.addAll( TreasureGenerator.generateRandom( quality, ran ) );
			}
		}

		return items;
	}

	public static Array<Item> generateRandom( int quality, Random ran )
	{
		Array<Item> items = new Array<Item>(  );

		// Chances,
		// Currency 3
		// Weapon 2
		// Armour 2
		// Ability 1

		int[] chances = {
			0, // currency
			3, // armour
			3, // weapons
			2 // utilities
		};

		int count = 0;
		for (int i : chances)
		{
			count += i;
		}

		int chance = ran.nextInt( count );

		if ( chance < chances[0] )
		{
			items.addAll( TreasureGenerator.generateCurrency( quality, ran ) );
			return items;
		}
		chance -= chances[0];

		if ( chance < chances[1] )
		{
			items.addAll( TreasureGenerator.generateArmour( quality, ran ) );
			return items;
		}
		chance -= chances[1];

		if ( chance < chances[2] )
		{
			items.addAll( TreasureGenerator.generateWeapon( quality, ran ) );
			return items;
		}
		chance -= chances[2];

		if ( chance < chances[3] )
		{
			items.addAll( TreasureGenerator.generateUtility( quality, ran ) );
			return items;
		}
		chance -= chances[3];

		return items;
	}

	public static Array<Item> generateCurrency( int quality, Random ran )
	{
		Array<Item> items = new Array<Item>(  );

		int val = ran.nextInt(100) * quality;

		Item money = Item.load( "Treasure/Money" );
		money.count = val;

		items.add(money);

		return items;
	}

	public static Array<Item> generateArmour( int quality, Random ran )
	{
		Array<Item> items = new Array<Item>(  );

		RecipeData recipe = recipeList.armourRecipes.get( ran.nextInt( recipeList.armourRecipes.size ) );

		items.add( itemFromRecipe( recipe, quality, ran ) );

		return items;
	}

	public static Array<Item> generateWeapon( int quality, Random ran )
	{
		Array<Item> items = new Array<Item>(  );

		RecipeData recipe = recipeList.weaponRecipes.get( ran.nextInt( recipeList.weaponRecipes.size ) );

		items.add( itemFromRecipe( recipe, quality, ran ) );

		return items;
	}

	public static Array<Item> generateUtility( int quality, Random ran )
	{
		Array<Item> items = new Array<Item>(  );

		HashMap<String, Integer> variableMap = new HashMap<String, Integer>(  );
		variableMap.put("quality", quality);

		Array<UtilityData> valid = new Array<UtilityData>(  );
		for (UtilityData ud : utilityList.utilityDatas)
		{
			if ( EquationHelper.evaluate( ud.condition, variableMap ) > 0 )
			{
				valid.add( ud );
			}
		}

		String path = valid.get( ran.nextInt( valid.size ) ).path;
		Item item = Item.load( "Utility/"+path );
		item.category = Item.ItemCategory.UTILITY;
		item.applyQuality( quality );
		item.slot = Item.EquipmentSlot.UTILITY_1;

		items.add( item );

		return items;
	}

	public static Item itemFromRecipe( RecipeData recipe, int quality, Random ran )
	{
		Item item = Recipe.createRecipe( recipe.itemTemplate, quality, recipe.getName( quality ) );

		int numModifiers = ran.nextInt( Math.max( 1, quality / 2 ) );

		for (int i = 0; i < numModifiers; i++)
		{
			String modifier = recipe.getModifier( quality, ran );

			if (modifier != null)
			{
				Recipe.applyModifer( item, modifier, quality, ran.nextBoolean() );
			}
		}

		int numAbilities = (int)( Math.min( recipe.acceptedAbilities.size, 2 ) * ran.nextFloat() * ran.nextFloat() );

		if (numAbilities > 0)
		{
			String ability = recipe.getAbility( quality, ran );

			if (ability != null)
			{
				item.ability1 = AbilityLoader.loadAbility( ability );
			}
		}

		if (numAbilities > 1)
		{
			String ability = recipe.getAbility( quality, ran );

			if (ability != null)
			{
				IAbility ab = AbilityLoader.loadAbility( ability );

				if (item.ability1 == null)
				{
					item.ability1 = ab;
				}
				else
				{
					item.ability2 = ab;
				}
			}
		}

		item.value = (int)( (float)item.value * ( 1.0f + (float)quality * 0.8f ) * (1.0f + 0.2f * (float)( numAbilities + numModifiers ) ) );

		return item;
	}

	public static final RecipeList recipeList = new RecipeList( "Items/Recipes/Recipes.xml" );
	public static final UtilityList utilityList = new UtilityList( "Items/Utility/Utilities.xml" );
	private static final HashMap<String, QualityMap> materialLists = new HashMap<String, QualityMap>(  );

	public static class UtilityList
	{
		public Array<UtilityData> utilityDatas = new Array<UtilityData>(  );

		public UtilityList(String path)
		{
			XmlReader reader = new XmlReader();
			XmlReader.Element xml = null;

			try
			{
				xml = reader.parse( Gdx.files.internal( path ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			for (int i = 0; i < xml.getChildCount(); i++)
			{
				XmlReader.Element utilEl = xml.getChild( i );

				utilityDatas.add( new UtilityData( utilEl.getName(), utilEl.getText() ) );
			}
		}
	}

	public static class UtilityData
	{
		public String path;
		public String condition;

		public UtilityData(String path, String cond)
		{
			this.path = path;
			this.condition = cond;
			if (cond == null || cond == "")
			{
				condition = "1";
			}
		}
	}

	public static class RecipeList
	{
		public Array<RecipeData> armourRecipes = new Array<RecipeData>(  );
		public Array<RecipeData> weaponRecipes = new Array<RecipeData>(  );

		public RecipeList( String path )
		{
			XmlReader reader = new XmlReader();
			XmlReader.Element xml = null;

			try
			{
				xml = reader.parse( Gdx.files.internal( path ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			XmlReader.Element armoursElement = xml.getChildByName( "Armours" );
			for (int i = 0; i < armoursElement.getChildCount(); i++)
			{
				XmlReader.Element armourElement = armoursElement.getChild( i );

				armourRecipes.add( new RecipeData( armourElement.getName() ) );
			}

			XmlReader.Element weaponsElement = xml.getChildByName( "Weapons" );
			for (int i = 0; i < weaponsElement.getChildCount(); i++)
			{
				XmlReader.Element weaponElement = weaponsElement.getChild( i );

				weaponRecipes.add( new RecipeData( weaponElement.getName() ) );
			}
		}

		public RecipeData getData( String recipe )
		{
			for (RecipeData rd : armourRecipes)
			{
				if (rd.recipeName.equals( recipe ))
				{
					return rd;
				}
			}

			for (RecipeData rd : weaponRecipes)
			{
				if (rd.recipeName.equals( recipe ))
				{
					return rd;
				}
			}

			return null;
		}
	}

	public static class RecipeData
	{
		public String recipeName;

		public Array<RecipeDataItem> acceptedAbilities = new Array<RecipeDataItem>(  );
		public Array<String> names = new Array<String>(  );
		public Array<RecipeDataItem> acceptedModifiers = new Array<RecipeDataItem>(  );

		public XmlReader.Element itemTemplate;

		public RecipeData( String recipeName )
		{
			this.recipeName = recipeName;

			XmlReader reader = new XmlReader();
			XmlReader.Element xml = null;

			try
			{
				xml = reader.parse( Gdx.files.internal( "Items/Recipes/"+recipeName+".xml" ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			itemTemplate = xml.getChildByName( "ItemTemplate" );

			XmlReader.Element namesElement = xml.getChildByName( "Names" );
			for (int i = 0; i < namesElement.getChildCount(); i++)
			{
				XmlReader.Element nameEl = namesElement.getChild( i );

				names.add( nameEl.getName().replace( "_", " " ) );
			}

			XmlReader.Element modsElement = xml.getChildByName( "AllowedModifiers" );
			for (int i = 0; i < modsElement.getChildCount(); i++)
			{
				XmlReader.Element modEl = modsElement.getChild( i );

				acceptedModifiers.add( new RecipeDataItem( modEl.getName(), modEl.getIntAttribute( "MinQuality", 0 ) ) );
			}

			XmlReader.Element absElement = xml.getChildByName( "AllowedAbilities" );
			for (int i = 0; i < absElement.getChildCount(); i++)
			{
				XmlReader.Element abEl = absElement.getChild( i );

				acceptedAbilities.add( new RecipeDataItem( abEl.getName(), abEl.getIntAttribute( "MinQuality", 0 ) ) );
			}
		}

		public String getName( int quality )
		{
			quality = quality - 1;
			if (quality >= names.size)
			{
				return names.get( names.size-1 );
			}
			else
			{
				return names.get( quality );
			}
		}

		public String getModifier( int quality, Random ran )
		{
			Array<String> temp = new Array<String>(  );
			for (RecipeDataItem item : acceptedModifiers)
			{
				if (item.minQuality <= quality)
				{
					temp.add( item.name );
				}
			}

			return temp.size > 0 ? temp.get( ran.nextInt( temp.size ) ) : null;
		}

		public String getAbility( int quality, Random ran )
		{
			Array<String> temp = new Array<String>(  );
			for (RecipeDataItem item : acceptedAbilities)
			{
				if (item.minQuality <= quality)
				{
					temp.add( item.name );
				}
			}

			return temp.size > 0 ? temp.get( ran.nextInt( temp.size ) ) : null;
		}
	}

	public static class RecipeDataItem
	{
		public String name;
		public int minQuality;

		public RecipeDataItem(String name, int minQuality)
		{
			this.name = name;
			this.minQuality = minQuality;
		}
	}

	private static class QualityMap
	{
		public Array<Array<QualityData>> qualityData = new Array<Array<QualityData>>(  );

		public QualityMap( String file )
		{
			XmlReader reader = new XmlReader();
			XmlReader.Element xml = null;

			try
			{
				xml = reader.parse( Gdx.files.internal( file ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			for (int i = 0; i < xml.getChildCount(); i++)
			{
				XmlReader.Element qualityElement = xml.getChild( i );

				Array<QualityData> qualityLevel = new Array<QualityData>(  );

				for (int ii = 0; ii < qualityElement.getChildCount(); ii++)
				{
					XmlReader.Element qEl = qualityElement.getChild( ii );
					String name = qEl.getName();
					String colour = qEl.getAttribute( "RGB", null );
					String tagString = qEl.getText();
					if (tagString == null) { tagString = ""; }
					String[] tags = tagString.toLowerCase().split( "," );
					qualityLevel.add( new QualityData( name, colour, tags ) );
				}

				qualityData.add( qualityLevel );
			}
		}
	}

	private static class QualityData
	{
		public String name;
		public String colour;
		public ObjectSet<String> tags = new ObjectSet<String>(  );

		public QualityData( String name, String colour, String[] tags )
		{
			this.name = name;
			this.colour = colour;
			this.tags.addAll( tags );
		}
	}
}
