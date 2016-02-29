package Roguelike.Save;

import java.util.HashMap;

import Roguelike.Ability.IAbility;
import Roguelike.Dialogue.Dialogue;
import Roguelike.Dialogue.DialogueManager;
import Roguelike.Entity.GameEntity;
import Roguelike.Global;
import Roguelike.Items.Inventory;
import Roguelike.StatusEffect.StatusEffect;
import Roguelike.Tiles.Point;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;

public final class SaveGameEntity extends SaveableObject<GameEntity>
{
	public String fileName;
	public Array<XmlReader.Element> xmlData;

	public int hp;
	public Point pos = new Point();
	public boolean isPlayer = false;
	public int quality = 1;
	public Array<StatusEffect> statuses = new Array<StatusEffect>();
	public Array<IAbility> slottedAbilities = new Array<IAbility>();
	public Inventory inventory;
	public String UID;
	public Point spawnPoint;

	public HashMap<String, Integer> dialogueData;

	@Override
	public void store( GameEntity obj )
	{
		fileName = obj.fileName;
		xmlData = obj.xmlData;

		hp = obj.HP;
		pos.set( obj.tile[0][0].x, obj.tile[0][0].y );
		for ( StatusEffect status : obj.statusEffects )
		{
			statuses.add( status );
		}
		inventory = obj.inventory;

		if (obj.dialogue != null)
		{
			dialogueData = obj.dialogue.data;
		}

		slottedAbilities.addAll( obj.slottedAbilities );

		UID = obj.UID;

		spawnPoint = obj.spawnPos;
		quality = obj.quality;
	}

	@Override
	public GameEntity create()
	{
		GameEntity entity = fileName != null ? GameEntity.load( fileName ) : GameEntity.load( xmlData );
		entity.spawnPos = spawnPoint;

		entity.HP = hp;
		entity.statusEffects.clear();
		for ( StatusEffect saveStatus : statuses )
		{
			entity.addStatusEffect( saveStatus );
		}

		entity.processStatuses();

		entity.inventory = inventory;

		entity.slottedAbilities.clear();
		for ( int i = 0; i < slottedAbilities.size; i++ )
		{
			entity.slottedAbilities.add( slottedAbilities.get( i ) );
			slottedAbilities.get( i ).setCaster( entity );
		}

		entity.UID = UID;

		if (entity.dialogue != null && dialogueData != null)
		{
			entity.dialogue.data = dialogueData;
		}
		entity.quality = quality;

		if (!isPlayer)
		{
			entity.applyDepthScaling();
		}

		entity.isVariableMapDirty = true;

		return entity;
	}
}
