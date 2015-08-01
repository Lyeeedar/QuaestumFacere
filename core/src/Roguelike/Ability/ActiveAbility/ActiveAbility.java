package Roguelike.Ability.ActiveAbility;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import Roguelike.AssetManager;
import Roguelike.Global.Direction;
import Roguelike.Ability.IAbility;
import Roguelike.Ability.ActiveAbility.AbilityType.AbstractAbilityType;
import Roguelike.Ability.ActiveAbility.EffectType.AbstractEffectType;
import Roguelike.Ability.ActiveAbility.MovementType.AbstractMovementType;
import Roguelike.Ability.ActiveAbility.MovementType.MovementTypeBolt;
import Roguelike.Ability.ActiveAbility.MovementType.MovementTypeRay;
import Roguelike.Ability.ActiveAbility.MovementType.MovementTypeSmite;
import Roguelike.Ability.ActiveAbility.TargetingType.AbstractTargetingType;
import Roguelike.Ability.ActiveAbility.TargetingType.TargetingTypeSelf;
import Roguelike.Ability.ActiveAbility.TargetingType.TargetingTypeTile;
import Roguelike.Entity.GameEntity;
import Roguelike.GameEvent.GameEventHandler;
import Roguelike.GameEvent.IGameObject;
import Roguelike.Items.Item;
import Roguelike.Items.Item.EquipmentSlot;
import Roguelike.Lights.Light;
import Roguelike.Screens.GameScreen;
import Roguelike.Shadows.ShadowCaster;
import Roguelike.Sound.SoundInstance;
import Roguelike.Sprite.Sprite;
import Roguelike.Sprite.SpriteEffect;
import Roguelike.Tiles.GameTile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

public class ActiveAbility implements IAbility, IGameObject
{
	private String name;
	private String description;
	
	private int cone = 0;
	private int aoe = 0;
	public int range = 1;
	private float screenshake = 0;
			
	public float cooldownAccumulator;
	public float cooldown = 1;
	
	private AbstractTargetingType targetingType = new TargetingTypeTile();
	private AbstractMovementType movementType = new MovementTypeSmite();
	public Array<AbstractEffectType> effectTypes = new Array<AbstractEffectType>();
	public Array<GameTile> AffectedTiles = new Array<GameTile>();
	
	public GameTile source;
	public GameEntity caster;
	public HashMap<String, Integer> variableMap = new HashMap<String, Integer>();
	
	//----------------------------------------------------------------------
	// rendering
	public Light light;
	public Sprite Icon;
	private Sprite movementSprite;
	private Sprite hitSprite;
	private Sprite useSprite;
	
	public ActiveAbility copy()
	{
		ActiveAbility aa = new ActiveAbility();
		
		aa.name = name;
		aa.description = description;
		aa.aoe = aoe;
		aa.range = range;
		aa.cone = cone;
		aa.screenshake = screenshake;
		
		aa.targetingType = targetingType.copy();
		aa.movementType = movementType.copy();
		
		for (AbstractEffectType effect : effectTypes)
		{
			aa.effectTypes.add(effect.copy());
		}
		
		for (GameTile tile : AffectedTiles)
		{
			aa.AffectedTiles.add(tile);
		}
		
		aa.light = light != null ? light.copy() : null;
		aa.Icon = Icon != null ? Icon.copy() : null;
		aa.movementSprite = movementSprite != null ? movementSprite.copy() : null;
		aa.hitSprite = hitSprite != null ? hitSprite.copy() : null;
		aa.useSprite = useSprite != null ? useSprite.copy() : null;
		
		return aa;
	}
	
	public Array<GameTile> getAffectedTiles()
	{
		return AffectedTiles;
	}
	
	public Sprite getSprite()
	{
		return movementSprite;
	}
	
	public Sprite getHitSprite()
	{
		if (hitSprite != null) { return hitSprite; }
		Item wep = caster.getInventory().getEquip(EquipmentSlot.MAINWEAPON);
		if (wep != null)
		{
			if (wep.hitEffect != null) 
			{ 
				return wep.hitEffect; 
			}
			else
			{
				return wep.weaponType.hitSprite;
			}
		}
		return caster.defaultHitEffect;
	}
	
	public Sprite getUseSprite()
	{
		return useSprite;
	}
	
	//----------------------------------------------------------------------
	public Sprite getIcon()
	{
		return Icon;
	}

	//----------------------------------------------------------------------
	public Table createTable(Skin skin)
	{
		Table table = new Table();

		table.add(new Label(name, skin)).expandX().left();
		table.row();

		Label descLabel = new Label(description, skin);
		descLabel.setWrap(true);
		table.add(descLabel).expand().left().width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(1, table));
		table.row();

		return table;
	}

	public boolean isTargetValid(GameTile tile, int[][] valid)
	{
		for (int[] validTile : valid)
		{
			if (validTile[0] == tile.x && validTile[1] == tile.y)
			{
				return true;
			}
		}
		
		return false;
	}
	
	public int[][] getValidTargets()
	{		
		Array<int[]> validTargets = new Array<int[]>();
		
		Array<int[]> output = new Array<int[]>();
		ShadowCaster shadow = new ShadowCaster(source.level.getGrid(), range);
		shadow.ComputeFOV(source.x, source.y, output);
		
		for (int[] tilePos : output)
		{
			GameTile tile = source.level.getGameTile(tilePos);
			
			if (targetingType.isTargetValid(this, tile))
			{
				validTargets.add(tilePos);
			}
		}
		
		return validTargets.toArray(int[].class);
	}
	
	public void lockTarget(GameTile tile)
	{
		movementType.init(this, tile.x, tile.y);
	}
	
	public void updateAccumulators(float cost)
	{
		movementType.updateAccumulators(cost);
	}
	
	public boolean needsUpdate()
	{
		if (movementType.needsUpdate()) { return true; }
		
		return false;
	}
	
	public boolean update()
	{
		boolean finished = movementType.update(this);
		if (movementSprite != null) { movementSprite.rotation = movementType.direction.GetAngle(); }
		
		if (finished)
		{
			GameTile epicenter = AffectedTiles.peek();
			
			if (movementType instanceof MovementTypeRay)
			{
				epicenter = AffectedTiles.first();
			}
			else if (aoe > 0)
			{
				Array<int[]> output = new Array<int[]>();
				
				ShadowCaster shadow = new ShadowCaster(epicenter.level.getGrid(), aoe);
				shadow.ComputeFOV(epicenter.x, epicenter.y, output);
				
				for (int[] tilePos : output)
				{
					GameTile tile = epicenter.level.getGameTile(tilePos);
					
					AffectedTiles.add(tile);
				}
			}
			else if (cone > 0)
			{
				Direction dir = Direction.getDirection(source, epicenter);
				Array<int[]> cone = Direction.buildCone(dir, new int[]{epicenter.x, epicenter.y}, this.cone);
				
				Array<int[]> output = new Array<int[]>();
				
				ShadowCaster shadow = new ShadowCaster(epicenter.level.getGrid(), this.cone);
				shadow.ComputeFOV(epicenter.x, epicenter.y, output);
				
				for (int[] tilePos : cone)
				{
					GameTile tile = epicenter.level.getGameTile(tilePos);
					
					boolean canAdd = false;
					for (int[] visPos : output)
					{
						GameTile visTile = epicenter.level.getGameTile(visPos);
						
						if (tile == visTile)
						{
							canAdd = true;
							break;
						}
					}
					
					if (canAdd)
					{
						AffectedTiles.add(tile);
					}
				}
			}
			
			// minimise list
			HashSet<GameTile> added = new HashSet<GameTile>();
			Iterator<GameTile> itr = AffectedTiles.iterator();
			while (itr.hasNext())
			{
				GameTile tile = itr.next();
				
				if (!added.contains(tile))
				{
					added.add(tile);
				}
				else
				{
					itr.remove();
				}
			}
			
			for (GameTile tile : AffectedTiles)
			{
				for (AbstractEffectType effect : effectTypes)
				{
					effect.update(this, 1, tile);
				}
				
				if (getHitSprite() != null)
				{
					Light l = light != null ? light.copy() : null;
					Sprite s = getHitSprite().copy();
					s.render = false;
					s.animationAccumulator = -s.animationDelay * tile.getDist(epicenter) + s.animationDelay;
					tile.spriteEffects.add(new SpriteEffect(s, Direction.CENTER, l));
					
					SoundInstance sound = s.sound;
					if (sound != null) { sound.play(tile); }
				}
			}
			
			if (screenshake > 0)
			{
				// check distance for screenshake
				float dist = Vector2.dst(epicenter.x, epicenter.y,  epicenter.level.player.tile.x,  epicenter.level.player.tile.y);
				float shakeRadius = screenshake;
				if (dist > aoe)
				{
					shakeRadius *= (dist-aoe) / (aoe*2);
				}
				
				if (shakeRadius > 2)
				{
					GameScreen.Instance.screenShakeRadius = shakeRadius;
					GameScreen.Instance.screenShakeAngle = MathUtils.random() * 360;
				}
			}			
		}
		
		return finished;
	}
	
	private void internalLoad(String name)
	{
		XmlReader xml = new XmlReader();
		Element xmlElement = null;
		
		try
		{
			xmlElement = xml.parse(Gdx.files.internal("Abilities/Lines/"+name+".xml"));
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		internalLoad(xmlElement);
	}
	
	private void internalLoad(Element xmlElement)
	{
		
		String extendsElement = xmlElement.getAttribute("Extends", null);
		if (extendsElement != null)
		{
			internalLoad(extendsElement);
		}
		
		this.name = xmlElement.get("Name", this.name);
		description = xmlElement.get("Description", description);
		aoe = xmlElement.getInt("AOE", aoe);
		cone = xmlElement.getInt("Cone", cone);
		range = xmlElement.getInt("Range", range);
		cooldown = xmlElement.getFloat("Cooldown", cooldown);
		screenshake = xmlElement.getFloat("ScreenShake", screenshake);
		
		Icon = xmlElement.getChildByName("Icon") != null ? AssetManager.loadSprite(xmlElement.getChildByName("Icon")) : Icon;
		movementSprite = xmlElement.getChildByName("MovementSprite") != null ? AssetManager.loadSprite(xmlElement.getChildByName("MovementSprite")) : movementSprite;
		hitSprite = xmlElement.getChildByName("HitSprite") != null ? AssetManager.loadSprite(xmlElement.getChildByName("HitSprite")) : hitSprite;
		useSprite = xmlElement.getChildByName("UseSprite") != null ? AssetManager.loadSprite(xmlElement.getChildByName("UseSprite")) : useSprite;
		
		Element lightElement = xmlElement.getChildByName("Light");
		if (lightElement != null)
		{
			light = Roguelike.Lights.Light.load(lightElement);
		}
		
//		Element typeElement = xmlElement.getChildByName("Type");
//		if (typeElement != null)
//		{
//			abilityType = AbstractAbilityType.load(typeElement.getChild(0));
//		}
		
		Element targetingElement = xmlElement.getChildByName("Targeting");
		if (targetingElement != null)
		{
			targetingType = AbstractTargetingType.load(targetingElement.getChild(0));
		}
		
		Element movementElement = xmlElement.getChildByName("Movement");
		if (movementElement != null)
		{
			movementType = AbstractMovementType.load(movementElement.getChild(0));
		}
		
		Element effectsElement = xmlElement.getChildByName("Effect");
		if (effectsElement != null)
		{
			for (int i = 0; i < effectsElement.getChildCount(); i++)
			{
				Element effectElement = effectsElement.getChild(i);
				effectTypes.add(AbstractEffectType.load(effectElement));
			}
		}
	}
	
	public static ActiveAbility load(String name)
	{
		ActiveAbility ab = new ActiveAbility();
		
		ab.internalLoad(name);
		
		return ab;
	}
	
	public static ActiveAbility load(Element xml)
	{
		ActiveAbility ab = new ActiveAbility();
		
		ab.internalLoad(xml);
		
		return ab;
	}
	
	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getDescription()
	{
		return description;
	}
}