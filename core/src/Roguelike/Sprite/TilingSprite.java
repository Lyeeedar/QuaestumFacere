package Roguelike.Sprite;

import Roguelike.AssetManager;

import Roguelike.Global;
import Roguelike.Util.EnumBitflag;
import Roguelike.Util.ImageUtils;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.XmlReader.Element;

// Naming priority: NSEW
public class TilingSprite
{
	private static final int CENTER = 1 << ( Global.Direction.CENTER.ordinal() + 1 );
	private static final int SOUTH = 1 << ( Global.Direction.SOUTH.ordinal() + 1 );

	public TilingSprite()
	{

	}

	public TilingSprite( Sprite topSprite, Sprite frontSprite )
	{
		sprites.put( CENTER, topSprite );
		sprites.put( SOUTH, frontSprite );

		hasAllElements = true;
	}

	public TilingSprite ( String name, String texture, String mask )
	{
		Element spriteBase = new Element("Sprite", null);

		load( name, name, texture, mask, spriteBase, null );
	}

	public IntMap<Sprite> sprites = new IntMap<Sprite>(  );

	public long thisID;
	public long checkID;
	public String texName;
	public String maskName;
	public Element spriteBase = new Element( "Sprite", null );
	public boolean additive = false;

	public boolean hasAllElements;

	public Sprite overhangSprite;

	public TilingSprite copy()
	{
		TilingSprite copy = new TilingSprite();
		copy.checkID = checkID;
		copy.thisID = thisID;
		copy.texName = texName;
		copy.maskName = maskName;
		copy.spriteBase = spriteBase;
		copy.hasAllElements = hasAllElements;
		copy.overhangSprite = overhangSprite;

		for (IntMap.Entry<Sprite> pair : sprites.entries())
		{
			copy.sprites.put( pair.key, pair.value.copy() );
		}

		return copy;
	}

	public void parse( Element xml )
	{
		String checkName, thisName;
		checkName = thisName = xml.get( "Name", null );

		checkName = xml.get( "CheckName", checkName );
		thisName = xml.get( "ThisName", thisName );

		Element overhangElement = xml.getChildByName( "Overhang" );

		Element topElement = xml.getChildByName("Top");
		if (topElement != null)
		{
			Sprite topSprite = AssetManager.loadSprite( topElement );
			Sprite frontSprite = AssetManager.loadSprite( xml.getChildByName( "Front" ) );

			sprites.put( CENTER, topSprite );
			sprites.put( SOUTH, frontSprite );

			hasAllElements = true;
		}

		Element spriteElement = xml.getChildByName( "Sprite" );
		String texName = spriteElement != null ? spriteElement.get( "Name" ) : null;
		String maskName = xml.get( "Mask", null );

		this.additive = xml.getBoolean( "Additive", false );

		load(thisName, checkName, texName, maskName, spriteElement, overhangElement);
	}

	public void load( String thisName, String checkName, String texName, String maskName, Element spriteElement, Element overhangElement )
	{
		this.thisID = thisName.toLowerCase().hashCode();
		this.checkID = checkName.toLowerCase().hashCode();
		this.texName = texName;
		this.maskName = maskName;
		this.spriteBase = spriteElement;

		if ( overhangElement != null )
		{
			overhangSprite = AssetManager.loadSprite( overhangElement );
		}
	}

	public static TilingSprite load( Element xml )
	{
		TilingSprite sprite = new TilingSprite();
		sprite.parse( xml );
		return sprite;
	}

	private static TextureRegion getMaskedSprite( String baseName, String maskBaseName, Array<String> masks, boolean additive )
	{
		// If no masks then just return the original texture
		if ( masks.size == 0)
		{
			return AssetManager.loadTextureRegion( "Sprites/" + baseName + ".png" );
		}

		// Build the mask suffix
		String mask = "";
		for ( String m : masks)
		{
			mask += "_" + m;
		}

		String maskedName = baseName + "_" + maskBaseName + mask + "_" + additive;

		TextureRegion tex = AssetManager.loadTextureRegion( "Sprites/" + maskedName + ".png" );

		// We have the texture, so return it
		if (tex != null)
		{
			return tex;
		}

		throw new RuntimeException( "No masked sprite packed for file: " + maskedName );

//		// If we havent been given a valid mask, then just return the original texture
//		if (maskBaseName == null)
//		{
//			return AssetManager.loadTextureRegion( "Sprites/" + baseName + ".png" );
//		}
//
//		Pixmap base = ImageUtils.textureToPixmap( AssetManager.loadTexture( "Sprites/" + baseName + ".png" ) );
//		Pixmap merged = base;
//		for (String maskSuffix : masks)
//		{
//			Texture maskTex = AssetManager.loadTexture( "Sprites/" + maskBaseName + "_" + maskSuffix + ".png" );
//
//			if (maskTex == null)
//			{
//				maskTex = AssetManager.loadTexture( "Sprites/" + maskBaseName + "_C.png" );
//			}
//
//			if (maskTex == null)
//			{
//				continue;
//			}
//
//			Pixmap maskedTex = ImageUtils.maskPixmap( merged, ImageUtils.textureToPixmap( maskTex ) );
//			if (merged != base) { merged.dispose(); }
//			merged = maskedTex;
//		}
//
//		return AssetManager.packPixmap( "Sprites/" + maskedName + ".png", merged );
	}

	public static Array<String> getMasks( EnumBitflag<Global.Direction> emptyDirections )
	{
		Array<String> masks = new Array<String>();

		if (emptyDirections.getBitFlag() == 0)
		{
			masks.add("C");
		}

		if (emptyDirections.contains( Global.Direction.NORTH ))
		{
			if (emptyDirections.contains( Global.Direction.EAST ))
			{
				masks.add("NE");
			}

			if (emptyDirections.contains( Global.Direction.WEST ))
			{
				masks.add("NW");
			}

			if (!emptyDirections.contains( Global.Direction.EAST ) && !emptyDirections.contains( Global.Direction.WEST ))
			{
				masks.add("N");
			}
		}

		if (emptyDirections.contains( Global.Direction.SOUTH ))
		{
			if (emptyDirections.contains( Global.Direction.EAST ))
			{
				masks.add("SE");
			}

			if (emptyDirections.contains( Global.Direction.WEST ))
			{
				masks.add("SW");
			}

			if (!emptyDirections.contains( Global.Direction.EAST ) && !emptyDirections.contains( Global.Direction.WEST ))
			{
				masks.add("S");
			}
		}

		if (emptyDirections.contains( Global.Direction.EAST ))
		{
			if (!emptyDirections.contains( Global.Direction.NORTH ) && !emptyDirections.contains( Global.Direction.SOUTH ))
			{
				masks.add("E");
			}
		}

		if (emptyDirections.contains( Global.Direction.WEST ))
		{
			if (!emptyDirections.contains( Global.Direction.NORTH ) && !emptyDirections.contains( Global.Direction.SOUTH ))
			{
				masks.add("W");
			}
		}

		if (emptyDirections.contains( Global.Direction.NORTHEAST ) && !emptyDirections.contains( Global.Direction.NORTH ) && !emptyDirections.contains( Global.Direction.EAST ))
		{
			masks.add("DNE");
		}

		if (emptyDirections.contains( Global.Direction.NORTHWEST ) && !emptyDirections.contains( Global.Direction.NORTH ) && !emptyDirections.contains( Global.Direction.WEST ))
		{
			masks.add("DNW");
		}

		if (emptyDirections.contains( Global.Direction.SOUTHEAST ) && !emptyDirections.contains( Global.Direction.SOUTH ) && !emptyDirections.contains( Global.Direction.EAST ))
		{
			masks.add("DSE");
		}

		if (emptyDirections.contains( Global.Direction.SOUTHWEST ) && !emptyDirections.contains( Global.Direction.SOUTH ) && !emptyDirections.contains( Global.Direction.WEST ))
		{
			masks.add("DSW");
		}

		return masks;
	}

	public Sprite getSprite( EnumBitflag<Global.Direction> emptyDirections )
	{
		if (hasAllElements)
		{
			if (emptyDirections.contains( Global.Direction.SOUTH ))
			{
				return sprites.get( SOUTH );
			}
			else
			{
				return sprites.get( CENTER );
			}
		}
		else
		{
			Sprite sprite = sprites.get( emptyDirections.getBitFlag() );
			if (sprite != null)
			{
				return sprite;
			}
			else
			{
				Array<String> masks = getMasks( emptyDirections );

				String mask = "";
				for ( String m : masks)
				{
					mask += "_" + m;
				}

				if (texName != null)
				{
					TextureRegion region = getMaskedSprite( texName, maskName, masks, additive );
					sprite = AssetManager.loadSprite( spriteBase, region );
				}
				else
				{
					sprite = sprites.get( CENTER );
				}

				sprites.put( emptyDirections.getBitFlag(), sprite );
				return sprite;
			}
		}
	}
}
