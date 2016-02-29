package Roguelike.Items;

import Roguelike.Sprite.Sprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.XmlReader;

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
			else if ( type.startsWith( "item(" ) )
			{
				String[] parts = type.split( "[\\(\\)]" );

				items.addAll( TreasureGenerator.generateItemFromMaterial( parts[1], quality, ran ) );
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
			3 // weapons
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

	public static Array<Item> generateItemFromMaterial( String materialType, int quality, Random ran )
	{
		Array<RecipeData> validRecipes = new Array<RecipeData>(  );

		for (RecipeData recipe : recipeList.weaponRecipes)
		{
			if (recipe.acceptsMaterial( materialType ))
			{
				validRecipes.add( recipe );
			}
		}

		for (RecipeData recipe : recipeList.armourRecipes)
		{
			if (recipe.acceptsMaterial( materialType ))
			{
				validRecipes.add( recipe );
			}
		}

		Array<Item> items = new Array<Item>(  );

		if (validRecipes.size == 0)
		{
			return items;
		}

		RecipeData chosen = validRecipes.get( ran.nextInt( validRecipes.size ) );
		items.add( itemFromRecipe( chosen, quality, ran ) );

		return items;
	}

	public static Item itemFromRecipe( RecipeData recipe, int quality, Random ran )
	{
		String materialType = recipe.getMaterial( quality, ran );
		Item materialItem = getMaterial( materialType, quality, ran );

		Item item = Recipe.createRecipe( recipe.recipeName, materialItem );

		item.getIcon().colour.mul( materialItem.getIcon().colour );

		int numModifiers = ran.nextInt( Math.max( 2, quality / 2 ) );

		for (int i = 0; i < numModifiers; i++)
		{
			String modifier = recipe.getModifier( quality, ran );

			if (modifier != null)
			{
				Recipe.applyModifer( item, modifier, quality, ran.nextBoolean() );
			}
		}

		return item;
	}

	public static Item getMaterial( String materialType, int quality, Random ran )
	{
		if (!materialLists.containsKey( materialType ))
		{
			if (Gdx.files.internal( "Items/Material/" + materialType + ".xml" ).exists())
			{
				QualityMap materialMap = new QualityMap( "Items/Material/" + materialType + ".xml" );
				materialLists.put( materialType, materialMap );
			}
			else
			{
				materialLists.put( materialType, null );
			}
		}

		QualityMap materialMap = materialLists.get( materialType );
		if (materialMap == null)
		{
			return null;
		}

		int materialQuality = Math.min( quality, materialMap.qualityData.size );

		String material = null;
		String colour = null;
		{
			int numChoices = materialMap.qualityData.get( materialQuality - 1 ).size;
			int choice = ran.nextInt( numChoices );
			QualityData qdata = materialMap.qualityData.get( materialQuality - 1 ).get( choice );
			material = qdata.name;
			colour = qdata.colour;
		}

		Item materialItem = null;
		if ( Gdx.files.internal( "Items/Material/"+material+".xml" ).exists() )
		{
			materialItem = Item.load( "Material/" + material );
		}
		else
		{
			materialItem = new Item();
			materialItem.name = material;
			materialItem.quality = materialQuality;
		}

		if ( colour != null )
		{
			Color col = new Color();

			String[] cols = colour.split( "," );
			col.r = Float.parseFloat( cols[0] ) / 255.0f;
			col.g = Float.parseFloat( cols[1] ) / 255.0f;
			col.b = Float.parseFloat( cols[2] ) / 255.0f;
			col.a = 1;

			materialItem.getIcon().colour = col;
		}

		return materialItem;
	}

	private static final RecipeList recipeList = new RecipeList( "Items/Recipes/Recipes.xml" );
	private static final HashMap<String, QualityMap> materialLists = new HashMap<String, QualityMap>(  );

	private static class RecipeList
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
	}

	private static class RecipeData
	{
		public String recipeName;

		public Array<RecipeDataItem> acceptedAbilities = new Array<RecipeDataItem>(  );
		public Array<RecipeDataItem> acceptedMaterials = new Array<RecipeDataItem>(  );
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

			XmlReader.Element matsElement = xml.getChildByName( "AllowedMaterials" );
			for (int i = 0; i < matsElement.getChildCount(); i++)
			{
				XmlReader.Element matEl = matsElement.getChild( i );

				acceptedMaterials.add( new RecipeDataItem( matEl.getName(), matEl.getIntAttribute( "MinQuality", 0 ) ) );
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

		public boolean acceptsMaterial(String name)
		{
			for (RecipeDataItem item : acceptedMaterials)
			{
				if (item.name.equals( name ))
				{
					return true;
				}
			}

			return false;
		}

		public String getMaterial( int quality, Random ran )
		{
			Array<String> temp = new Array<String>(  );
			for (RecipeDataItem item : acceptedMaterials)
			{
				if (item.minQuality <= quality)
				{
					temp.add( item.name );
				}
			}

			return temp.get( ran.nextInt( temp.size ) );
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

	private static class RecipeDataItem
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
