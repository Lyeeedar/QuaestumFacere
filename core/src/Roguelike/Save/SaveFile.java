package Roguelike.Save;

import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.Ability.PassiveAbility.PassiveAbility;
import Roguelike.AssetManager;
import Roguelike.DungeonGeneration.DungeonFileParser.DFPRoom;
import Roguelike.DungeonGeneration.Room;
import Roguelike.DungeonGeneration.Room.RoomDoor;
import Roguelike.DungeonGeneration.Symbol;
import Roguelike.Entity.ActivationAction.*;
import Roguelike.GameEvent.AdditionalSprite;
import Roguelike.GameEvent.Constant.ConstantEvent;
import Roguelike.GameEvent.Damage.*;
import Roguelike.GameEvent.OnDeath.AbstractOnDeathEvent;
import Roguelike.GameEvent.OnDeath.FieldOnDeathEvent;
import Roguelike.GameEvent.OnDeath.HealOnDeathEvent;
import Roguelike.GameEvent.OnDeath.SpawnOnDeathEvent;
import Roguelike.GameEvent.OnExpire.AbilityOnExpireEvent;
import Roguelike.GameEvent.OnExpire.KillOnExpireEvent;
import Roguelike.GameEvent.OnHit.AbilityOnHitEvent;
import Roguelike.GameEvent.OnHit.DummyOnHitEvent;
import Roguelike.GameEvent.OnHit.StatusOnHitEvent;
import Roguelike.GameEvent.OnTask.CancelTaskEvent;
import Roguelike.GameEvent.OnTask.CostTaskEvent;
import Roguelike.GameEvent.OnTask.DamageTaskEvent;
import Roguelike.GameEvent.OnTask.StatusTaskEvent;
import Roguelike.GameEvent.OnTurn.DamageOverTimeEvent;
import Roguelike.GameEvent.OnTurn.HealOverTimeEvent;
import Roguelike.Global;
import Roguelike.Global.Direction;
import Roguelike.Global.Statistic;
import Roguelike.Items.Inventory;
import Roguelike.Items.Item;
import Roguelike.Items.Item.EquipmentSlot;
import Roguelike.Items.Item.ItemCategory;
import Roguelike.Lights.Light;
import Roguelike.Pathfinding.ShadowCastCache;
import Roguelike.Quests.Input.QuestInputFlagEquals;
import Roguelike.Quests.Input.QuestInputFlagPresent;
import Roguelike.Quests.Output.*;
import Roguelike.Quests.Quest;
import Roguelike.Quests.QuestManager;
import Roguelike.Save.SaveLevel.SaveLevelItem;
import Roguelike.Sound.SoundInstance;
import Roguelike.Sprite.Sprite;
import Roguelike.Sprite.Sprite.AnimationMode;
import Roguelike.Sprite.Sprite.AnimationState;
import Roguelike.Sprite.SpriteAnimation.AbstractSpriteAnimation;
import Roguelike.Sprite.SpriteAnimation.BumpAnimation;
import Roguelike.Sprite.SpriteAnimation.MoveAnimation;
import Roguelike.Sprite.SpriteAnimation.StretchAnimation;
import Roguelike.Sprite.TilingSprite;
import Roguelike.StatusEffect.StatusEffect;
import Roguelike.Tiles.Point;
import Roguelike.Util.EnumBitflag;
import Roguelike.Util.FastEnumMap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.minlog.Log;
import kryo.FastEnumMapSerializer;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

public final class SaveFile
{
	private static Kryo kryo;

	public QuestManager questManager;
	public Array<Item> unlockedItems;
	public int funds;
	public Array<Item> market;
	public Array<Quest> missions;
	public FastEnumMap<Item.EquipmentSlot, Integer> loadout;

	public void save()
	{
		setupKryo();

		FileHandle attemptFile = Gdx.files.local( "attempt_save.dat" );

		Output output = null;
		try
		{
			output = new Output( new GZIPOutputStream( attemptFile.write( false ) ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		kryo.writeObject( output, questManager );
		kryo.writeObject( output, unlockedItems );
		output.writeInt( funds );
		kryo.writeObject( output, market );
		kryo.writeObject( output, missions );
		kryo.writeObject( output, loadout );

		output.close();

		byte[] bytes = attemptFile.readBytes();
		FileHandle actualFile = Gdx.files.local( "save.dat" );
		actualFile.writeBytes( bytes, false );

		attemptFile.delete();

		System.out.println( "Saved" );
	}

	public void load()
	{
		setupKryo();

		Input input = null;
		try
		{
			input = new Input( new GZIPInputStream( Gdx.files.local( "save.dat" ).read() ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		questManager = kryo.readObject( input, QuestManager.class );
		unlockedItems = kryo.readObject( input, Array.class );
		funds = input.readInt();
		market = kryo.readObject( input, Array.class );
		missions = kryo.readObject( input, Array.class );
		loadout = kryo.readObject( input, FastEnumMap.class );

		input.close();
	}

	private void setupKryo()
	{
		if ( kryo == null )
		{
			kryo = new Kryo();
			kryo.setRegistrationRequired( true );
			kryo.setAsmEnabled( true );

			registerSerializers( kryo );
			registerClasses( kryo );

			//Log.set(Log.LEVEL_TRACE);
		}
	}

	private void registerSerializers( Kryo kryo )
	{
		kryo.register( FastEnumMap.class, new FastEnumMapSerializer() );

		kryo.register( Array.class, new Serializer<Array>()
		{
			private Class[] genericType;

			{
				setAcceptsNull( true );
			}

			@Override
			public void write( Kryo kryo, Output output, Array array )
			{
				if (TRACE) trace("kryo", "ArraySerializer.write array: " + array.toString());

				int length = array.size;
				output.writeInt( length, true );

				if ( length == 0 )
				{
					genericType = null;
					return;
				}

				if ( genericType != null )
				{
					Serializer serializer = kryo.getSerializer( genericType[0] );

					if (TRACE) trace("kryo", "ArraySerializer writing objects of type: " + genericType[0]);

					genericType = null;
					for ( Object element : array )
					{
						if (TRACE) trace("kryo", "ArraySerializer.beginWrite: " + element);
						kryo.writeObjectOrNull( output, element, serializer );
						if (TRACE) trace("kryo", "ArraySerializer.endWrite: " + element);
					}
				}
				else
				{
					for ( Object element : array )
					{
						kryo.writeClassAndObject( output, element );
					}
				}
			}

			@Override
			public void setGenerics( Kryo kryo, Class[] generics )
			{
				if (generics != null && kryo.isFinal( generics[0] ))
				{
					if (TRACE) trace("kryo", "ArraySerializer.setGenerics: " + generics[0]);
					genericType = generics;
				}
				else
				{
					if (TRACE) trace("kryo", "ArraySerializer.setGenerics: null");
					genericType = null;
				}
			}


			@Override
			public Array read( Kryo kryo, Input input, Class<Array> type )
			{
				Array array = new Array();
				kryo.reference( array );

				int length = input.readInt( true );
				array.ensureCapacity( length );

				if ( genericType != null )
				{
					Class elementClass = genericType[0];
					Serializer serializer = kryo.getSerializer( elementClass );
					genericType = null;

					for ( int i = 0; i < length; i++ )
					{
						array.add( kryo.readObjectOrNull( input, elementClass, serializer ) );
					}
				}
				else
				{
					for ( int i = 0; i < length; i++ )
					{
						array.add( kryo.readClassAndObject( input ) );
					}
				}
				return array;
			}
		} );

		kryo.register( Color.class, new Serializer<Color>()
		{
			@Override
			public Color read( Kryo kryo, Input input, Class<Color> type )
			{
				Color color = new Color();
				Color.rgba8888ToColor( color, input.readInt() );
				return color;
			}

			@Override
			public void write( Kryo kryo, Output output, Color color )
			{
				output.writeInt( Color.rgba8888( color ) );
			}
		} );

		kryo.register( Sprite.class, new Serializer<Sprite>()
		{
			@Override
			public Sprite read( Kryo kryo, Input input, Class<Sprite> type )
			{
				String fileName = input.readString();
				float animDelay = input.readFloat();
				float repeatDelay = input.readFloat();
				Color color = kryo.readObject( input, Color.class );
				int modeVal = input.readInt();
				AnimationMode mode = AnimationMode.values()[ modeVal ];
				float[] scale = input.readFloats( 2 );
				boolean drawActualSize = input.readBoolean();
				SoundInstance sound = kryo.readObjectOrNull( input, SoundInstance.class );
				AbstractSpriteAnimation anim = (AbstractSpriteAnimation)kryo.readClassAndObject( input );

				Sprite sprite = AssetManager.loadSprite( fileName, animDelay, color, mode, sound, drawActualSize );
				sprite.spriteAnimation = anim;
				sprite.baseScale = scale;
				sprite.repeatDelay = repeatDelay;
				return sprite;
			}

			@Override
			public void write( Kryo kryo, Output output, Sprite sprite )
			{
				output.writeString( sprite.fileName );
				output.writeFloat( sprite.animationDelay );
				output.writeFloat( sprite.repeatDelay );
				kryo.writeObject( output, sprite.colour );
				output.writeInt( sprite.animationState.mode.ordinal() );
				output.writeFloats( sprite.baseScale );
				output.writeBoolean( sprite.drawActualSize );
				kryo.writeObjectOrNull( output, sprite.sound, SoundInstance.class );
				kryo.writeClassAndObject( output, sprite.spriteAnimation );
			}
		} );

		kryo.register( SoundInstance.class, new Serializer<SoundInstance>()
		{
			@Override
			public SoundInstance read( Kryo kryo, Input input, Class<SoundInstance> type )
			{
				SoundInstance sound = new SoundInstance(  );

				sound.name = input.readString();
				sound.sound = AssetManager.loadSound( sound.name );

				sound.minPitch = input.readFloat();
				sound.maxPitch = input.readFloat();
				sound.volume = input.readFloat();

				sound.range = input.readInt();
				sound.falloffMin = input.readInt();

				return sound;
			}

			@Override
			public void write( Kryo kryo, Output output, SoundInstance sound )
			{
				output.writeString( sound.name );

				output.writeFloat( sound.minPitch );
				output.writeFloat( sound.maxPitch );
				output.writeFloat( sound.volume );

				output.writeInt( sound.range );
				output.writeInt( sound.falloffMin );
			}
		} );

		kryo.register( ActiveAbility.class, new Serializer<ActiveAbility>()
		{
			@Override
			public void write( Kryo kryo, Output output, ActiveAbility object )
			{
				output.writeString( object.creationPath );
				kryo.writeObjectOrNull( output, object.creationData, Element.class);
				output.writeInt( object.cooldownAccumulator );
			}

			@Override
			public ActiveAbility read( Kryo kryo, Input input, Class<ActiveAbility> type )
			{
				String creationPath = input.readString();
				Element creationData = kryo.readObjectOrNull( input, Element.class );
				int cooldown = input.readInt();

				ActiveAbility ab;
				if (creationPath != null)
				{
					ab = ActiveAbility.load( creationPath );
				}
				else
				{
					ab = ActiveAbility.load( creationData );
				}

				ab.cooldownAccumulator = cooldown;

				return ab;
			}
		} );

		kryo.register( Element.class, new Serializer<Element>()
		{
			@Override
			public Element read( Kryo kryo, Input input, Class<Element> type )
			{
				String xml = input.readString();

				XmlReader reader = new XmlReader();
				Element element = reader.parse( xml );
				return element;
			}

			@Override
			public void write( Kryo kryo, Output output, Element element )
			{
				output.writeString( element.toString() );
			}
		} );

		kryo.register( ObjectMap.class, new Serializer<ObjectMap>()
		{
			private Class[] genericType;

			{
				setAcceptsNull( true );
			}

			@Override
			public void write( Kryo kryo, Output output, ObjectMap map )
			{
				int length = map.size;
				output.writeInt( length, true );

				if ( length == 0 )
				{
					genericType = null;
					return;
				}

				if ( genericType != null )
				{
					Serializer keyserializer = kryo.getSerializer( genericType[0] );
					Serializer valueserializer = kryo.getSerializer( genericType[1] );

					genericType = null;
					for ( Object key : map.keys() )
					{
						Object value = map.get( key );

						kryo.writeObject( output, key, keyserializer );
						kryo.writeObjectOrNull( output, value, valueserializer );
					}
				}
				else
				{
					for ( Object key : map.keys() )
					{
						Object value = map.get( key );

						kryo.writeClassAndObject( output, key );
						kryo.writeClassAndObject( output, value );
					}
				}
			}

			@Override
			public void setGenerics( Kryo kryo, Class[] generics )
			{
				if (generics != null && kryo.isFinal( generics[0] ) && kryo.isFinal( generics[1] ) )
				{
					if (TRACE) trace("kryo", "ObjectMapSerializer.setGenerics: " + generics[0] + ", " + generics[1] );
					genericType = generics;
				}
				else
				{
					if (TRACE) trace("kryo", "ObjectMapSerializer.setGenerics: null");
					genericType = null;
				}
			}


			@Override
			public ObjectMap read( Kryo kryo, Input input, Class<ObjectMap> type )
			{
				ObjectMap map = new ObjectMap();
				kryo.reference( map );

				int length = input.readInt( true );
				map.ensureCapacity( length );

				if ( genericType != null )
				{
					Class keyClass = genericType[0];
					Serializer keyserializer = kryo.getSerializer( keyClass );

					Class valueClass = genericType[1];
					Serializer valueserializer = kryo.getSerializer( valueClass );
					genericType = null;

					for ( int i = 0; i < length; i++ )
					{
						Object key = kryo.readObject( input, keyClass, keyserializer );
						Object value = kryo.readObjectOrNull( input, valueClass, valueserializer );

						map.put( key, value );
					}
				}
				else
				{
					for ( int i = 0; i < length; i++ )
					{
						Object key = kryo.readClassAndObject( input );
						Object value = kryo.readClassAndObject( input );

						map.put( key, value );
					}
				}

				return map;
			}
		} );

		kryo.register( IntMap.class, new Serializer<IntMap>()
		{
			private Class[] genericType;

			{
				setAcceptsNull( true );
			}

			@Override
			public void write( Kryo kryo, Output output, IntMap map )
			{
				int length = map.size;
				output.writeInt( length, true );

				if ( length == 0 )
				{
					genericType = null;
					return;
				}

				if ( genericType != null )
				{
					Serializer valueserializer = kryo.getSerializer( genericType[0] );

					genericType = null;

					for (Object entryObj : map.entries())
					{
						IntMap.Entry entry = (IntMap.Entry)entryObj;

						output.writeInt( entry.key );
						kryo.writeObjectOrNull( output, entry.value, valueserializer );
					}
				}
				else
				{
					for (Object entryObj : map.entries())
					{
						IntMap.Entry entry = (IntMap.Entry)entryObj;

						output.writeInt( entry.key );
						kryo.writeClassAndObject( output, entry.value );
					}
				}
			}

			@Override
			public void setGenerics( Kryo kryo, Class[] generics )
			{
				if (generics != null && kryo.isFinal( generics[0] ))
				{
					if (TRACE) trace("kryo", "IntMapSerializer.setGenerics: " + generics[0]);
					genericType = generics;
				}
				else
				{
					if (TRACE) trace("kryo", "IntMapSerializer.setGenerics: null");
					genericType = null;
				}
			}


			@Override
			public IntMap read( Kryo kryo, Input input, Class<IntMap> type )
			{
				IntMap map = new IntMap();
				kryo.reference( map );

				int length = input.readInt( true );
				map.ensureCapacity( length );

				if ( genericType != null )
				{
					Class valueClass = genericType[0];
					Serializer valueserializer = kryo.getSerializer( valueClass );
					genericType = null;

					for ( int i = 0; i < length; i++ )
					{
						int key = input.readInt();
						Object value = kryo.readObjectOrNull( input, valueClass, valueserializer );

						map.put( key, value );
					}
				}
				else
				{
					for ( int i = 0; i < length; i++ )
					{
						int key = input.readInt();
						Object value = kryo.readClassAndObject( input );

						map.put( key, value );
					}
				}

				return map;
			}
		} );
	}

	private void registerClasses( Kryo kryo )
	{
		kryo.register( SaveEnvironmentEntity.class );
		kryo.register( SaveField.class );
		kryo.register( SaveFile.class );
		kryo.register( SaveGameEntity.class );
		kryo.register( SaveLevel.class );
		kryo.register( SaveLevelItem.class );

		kryo.register( Point.class );
		kryo.register( StatusEffect.class );
		kryo.register( StatusEffect.DurationType.class );
		kryo.register( Inventory.class );
		kryo.register( Element.class );
		kryo.register( DFPRoom.class );
		kryo.register( Item.class );
		kryo.register( Item.WeaponDefinition.class );
		kryo.register( Item.SpriteGroup.class );
		kryo.register( Light.class );
		kryo.register( ShadowCastCache.class );
		kryo.register( EnumBitflag.class );
		kryo.register( Symbol.class );
		kryo.register( Symbol[].class );
		kryo.register( Symbol[][].class );
		kryo.register( Room.class );
		kryo.register( RoomDoor.class );
		kryo.register( DFPRoom.Placement.class );
		kryo.register( AnimationState.class );
		kryo.register( AnimationMode.class );
		kryo.register( QuestManager.class );
		kryo.register( TilingSprite.class );
		kryo.register( PassiveAbility.class );

		kryo.register( HashMap.class );
		kryo.register( String[].class );
		kryo.register( int[].class );
		kryo.register( Object.class );
		kryo.register( Object[].class );
		kryo.register( char[].class );
		kryo.register( char[][].class );
		kryo.register( float[].class );
		kryo.register( Float[].class );
		kryo.register( Float[][].class );
		kryo.register( boolean[].class );
		kryo.register( boolean[][].class );
		kryo.register( ObjectSet.class );

		kryo.register( BumpAnimation.class );
		kryo.register( MoveAnimation.class );
		kryo.register( StretchAnimation.class );
		kryo.register( MoveAnimation.MoveEquation.class );
		kryo.register( StretchAnimation.StretchEquation.class );

		kryo.register( EquipmentSlot.class );
		kryo.register( ItemCategory.class );
		kryo.register( Statistic.class );
		kryo.register( Direction.class );
		kryo.register( Global.Rarity.class );
		kryo.register( Item.WeaponDefinition.HitType.class );
		kryo.register( ActiveAbility.CooldownType.class );

		kryo.register( ConstantEvent.class );
		kryo.register( DamageEvent.class );
		kryo.register( FieldEvent.class );
		kryo.register( HealEvent.class );
		kryo.register( StatusEvent.class );
		kryo.register( BlockEvent.class );
		kryo.register( FieldOnDeathEvent.class );
		kryo.register( HealOnDeathEvent.class );
		kryo.register( AbstractOnDeathEvent.class );
		kryo.register( CancelTaskEvent.class );
		kryo.register( CostTaskEvent.class );
		kryo.register( DamageTaskEvent.class );
		kryo.register( StatusTaskEvent.class );
		kryo.register( DamageOverTimeEvent.class );
		kryo.register( HealOverTimeEvent.class );
		kryo.register( KillOnExpireEvent.class );
		kryo.register( AbilityOnExpireEvent.class );
		kryo.register( AdditionalSprite.class );
		kryo.register( AbilityOnHitEvent.class );
		kryo.register( SpawnOnDeathEvent.class );
		kryo.register( DummyOnHitEvent.class );
		kryo.register( StatusOnHitEvent.class );

		kryo.register( Quest.class );
		kryo.register( QuestInputFlagPresent.class );
		kryo.register( QuestInputFlagEquals.class );
		kryo.register( QuestOutput.class );
		kryo.register( QuestOutputConditionEntityAlive.class );
		kryo.register( QuestOuputConditionDialogueValue.class );
		kryo.register( QuestOutputConditionActionEnabled.class );
		kryo.register( QuestOutputConditionHasItem.class );
		kryo.register( QuestOutputConditionMetaValue.class );

		kryo.register( ActivationActionGroup.class );
		kryo.register( ActivationActionAbility.class );
		kryo.register( ActivationActionAddItem.class );
		kryo.register( ActivationActionSetEnabled.class );
		kryo.register( ActivationActionSetPassable.class );
		kryo.register( ActivationActionSetSprite.class );
		kryo.register( ActivationActionSetLight.class );
		kryo.register( ActivationActionSpawnEntity.class );
		kryo.register( ActivationActionActivate.class );
		kryo.register( ActivationActionSpawnField.class );
		kryo.register( ActivationActionKillThis.class );
		kryo.register( ActivationActionRemoveItem.class );
		kryo.register( ActivationActionAddStatus.class );
		kryo.register( ActivationActionDealDamage.class );
		kryo.register( ActivationConditionProximity.class );
		kryo.register( ActivationConditionHasItem.class );
		kryo.register( ActivationActionEndMission.class );
	}
}
