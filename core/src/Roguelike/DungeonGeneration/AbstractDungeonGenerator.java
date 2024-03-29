package Roguelike.DungeonGeneration;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import Roguelike.Fields.Field;
import Roguelike.Global;
import Roguelike.Global.Direction;
import Roguelike.DungeonGeneration.DungeonFileParser.DFPRoom;
import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Entity.GameEntity;
import Roguelike.Levels.Level;
import Roguelike.Quests.Quest;
import Roguelike.Save.SaveLevel;
import Roguelike.Tiles.GameTile;

import Roguelike.Tiles.Point;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.reflect.ClassReflection;

public abstract class AbstractDungeonGenerator
{
	public int percent;
	public String generationText = "Selecting Rooms";
	protected int generationIndex = 0;
	protected SaveLevel saveLevel;
	protected Quest quest;

	protected DungeonFileParser dfp;
	protected Array<DFPRoom> additionalRooms = new Array<DFPRoom>();
	protected Array<Room> requiredRooms = new Array<Room>();
	protected Array<Room> toBePlaced = new Array<Room>();

	protected Array<Room> placedRooms = new Array<Room>();

	protected FactionParser majorFaction;

	protected Level level;
	protected Random ran;
	protected int width;
	protected int height;

	protected static final boolean DEBUG_OUTPUT = false;

	// ----------------------------------------------------------------------
	public abstract void setup( SaveLevel level, Quest quest, DungeonFileParser dfp );

	// ----------------------------------------------------------------------
	public abstract boolean generate();

	// ----------------------------------------------------------------------
	public Level getLevel()
	{
		return level;
	}

	// ----------------------------------------------------------------------
	protected void selectFactions()
	{
		String majorFactionName = quest.faction;

		majorFaction = FactionParser.load( majorFactionName );
	}

	// ----------------------------------------------------------------------
	protected void selectRooms()
	{
		selectFactions();

		for ( DFPRoom r : dfp.getRooms( ran, majorFaction ) )
		{
			Room room = new Room();
			r.fillRoom( room, ran, dfp );
			requiredRooms.add( room );
		}

		additionalRooms.addAll( saveLevel.requiredRooms );

		for ( DFPRoom r : additionalRooms )
		{
			Room room = new Room();
			r.fillRoom( room, ran, dfp );
			requiredRooms.add( room );
		}

		requiredRooms.sort( new Comparator<Room>()
				{
			@Override
			public int compare( Room arg0, Room arg1 )
			{
				return arg0.comparisonString().compareTo( arg1.comparisonString() );
			}
				} );
	}

	// ----------------------------------------------------------------------
	public static Symbol[][] minimiseGrid( Symbol[][] grid, Symbol wall )
	{
		int width = grid.length;
		int height = grid[0].length;

		int minx = -1;
		int miny = -1;
		int maxx = -1;
		int maxy = -1;

		boolean complete = false;

		// find min x
		for ( int x = 0; x < width; x++ )
		{
			for ( int y = 0; y < height; y++ )
			{
				Symbol s = grid[x][y];
				if ( s.character != wall.character )
				{
					minx = x - 1;

					complete = true;
					break;
				}
			}
			if ( complete )
			{
				break;
			}
		}
		if ( minx == -1 || minx >= grid.length ) { return grid; }

		// find min y
		complete = false;
		for ( int y = 0; y < height; y++ )
		{
			for ( int x = minx; x < width; x++ )
			{
				Symbol s = grid[x][y];
				if ( s.character != wall.character )
				{
					miny = y - 1;

					complete = true;
					break;
				}
			}
			if ( complete )
			{
				break;
			}
		}
		if ( miny == -1 || miny >= grid[0].length ) { return grid; }

		// find max x
		complete = false;
		for ( int x = width - 1; x >= minx; x-- )
		{
			for ( int y = miny; y < height; y++ )
			{
				Symbol s = grid[x][y];
				if ( s.character != wall.character )
				{
					maxx = x + 2;

					complete = true;
					break;
				}
			}
			if ( complete )
			{
				break;
			}
		}
		if ( maxx == -1 || maxx >= grid.length ) { return grid; }

		// find max y
		complete = false;
		for ( int y = height - 1; y >= miny; y-- )
		{
			for ( int x = minx; x < maxx; x++ )
			{
				Symbol s = grid[x][y];
				if ( s.character != wall.character )
				{
					maxy = y + 2;

					complete = true;
					break;
				}
			}
			if ( complete )
			{
				break;
			}
		}
		if ( maxy == -1 || maxy >= grid[0].length ) { return grid; }

		// minimise room
		int newwidth = Math.min( width, maxx - minx );
		int newheight = Math.min( height, maxy - miny );

		Symbol[][] newgrid = new Symbol[newwidth][newheight];

		for ( int x = 0; x < newwidth; x++ )
		{
			for ( int y = 0; y < newheight; y++ )
			{
				newgrid[x][y] = grid[minx + x][miny + y];
			}
		}

		return newgrid;
	}

	// ----------------------------------------------------------------------
	protected void DEBUG_printGrid( Symbol[][] symbolGrid )
	{
		for ( int y = height - 1; y >= 0; y-- )
		{
			for ( int x = 0; x < width; x++ )
			{
				System.out.print( symbolGrid[x][y].character );
			}
			System.out.print( "\n" );
		}
		System.out.println( "\n" );
	}

	// ----------------------------------------------------------------------
	protected void placeFactions()
	{
		// place factions

		// get largest room
		int max = 0;
		Room largest = null;

		for ( Room room : placedRooms )
		{
			int size = room.width * room.height;

			if ( size > max )
			{
				max = size;
				largest = room;
			}
		}

		for ( Room room : placedRooms )
		{
			FactionParser faction = null;
			if ( room.faction == null )
			{
				faction = majorFaction;
			}
			else if ( !room.faction.equalsIgnoreCase( "none" ) )
			{
				if (room.faction.equals( majorFaction.name ))
				{
					faction = majorFaction;
				}

				if (faction == null)
				{
					faction = FactionParser.load( room.faction );
				}
			}

			int influence = ran.nextInt( 50 ) + 30;
			room.addFeatures( ran, dfp, faction, influence, ran.nextInt( 5 ) == 0 );
		}
	}

	// ----------------------------------------------------------------------
	protected Level createLevel( Symbol[][] symbolGrid, Symbol outerWall )
	{
		FactionParser fp = majorFaction;

		if ( DEBUG_OUTPUT )
		{
			DEBUG_printGrid( symbolGrid );
		}

		// minimise
		symbolGrid = minimiseGrid( symbolGrid, outerWall );
		width = symbolGrid.length;
		height = symbolGrid[0].length;

		//if ( DEBUG_OUTPUT )
		{
			DEBUG_printGrid( symbolGrid );
		}

		GameTile[][] actualTiles = new GameTile[width][height];
		Level level = new Level( actualTiles );
		level.Ambient = dfp.ambient;
		level.bgmName = quest.music;
		level.ambientSounds.addAll( dfp.ambientSounds );

		level.fileName = saveLevel.fileName;
		level.seed = saveLevel.seed;
		level.requiredRooms = additionalRooms;

		level.background = dfp.background;

		for ( int x = 0; x < width; x++ )
		{
			for ( int y = 0; y < height; y++ )
			{
				Symbol symbol = symbolGrid[x][y];

				GameTile newTile = new GameTile( x, y, level, symbol.getTileData(), ran.nextFloat() );
				newTile.metaValue.addAll( symbol.metaValue);

				actualTiles[x][y] = newTile;

				// System.out.print(symbol.character);
			}
			// System.out.print("\n");
		}

		for ( int x = 0; x < width; x++ )
		{
			for ( int y = 0; y < height; y++ )
			{
				Symbol symbol = symbolGrid[x][y];

				if ( !saveLevel.created && symbol.hasEnvironmentEntity() )
				{
					GameTile newTile = actualTiles[x][y];
					EnvironmentEntity entity = symbol.getEnvironmentEntity( );

					if ( entity.attachToWall )
					{
						Direction location = Direction.CENTER;

						if ( symbol.attachLocation != null )
						{
							location = symbol.attachLocation;
						}
						else
						{
							// get direction
							HashSet<Direction> validDirections = new HashSet<Direction>();
							for ( Direction dir : Direction.values() )
							{
								boolean passable = symbolGrid[x + dir.getX()][y + dir.getY()].getTileData().passableBy.getBitFlag() != 0;
								if ( !passable )
								{
									validDirections.add( dir );
								}
							}

							if ( validDirections.size() > 0 )
							{
								if ( location == Direction.CENTER )
								{
									for ( Direction dir : Direction.values() )
									{
										if ( dir.isCardinal() )
										{
											if ( validDirections.contains( dir ) )
											{
												location = dir;
												break;
											}
										}
									}
								}

								// look for direction with full surround
								for ( Direction dir : Direction.values() )
								{
									boolean acwvalid = validDirections.contains( dir.getAnticlockwise() );
									boolean valid = validDirections.contains( dir );
									boolean cwvalid = validDirections.contains( dir.getClockwise() );

									if ( acwvalid && valid && cwvalid )
									{
										location = dir;
										break;
									}
								}

								// If that failed then just try the cardinal
								// directions

								// else pick random
								if ( location == Direction.CENTER )
								{
									location = validDirections.toArray( new Direction[validDirections.size()] )[ran.nextInt( validDirections.size() )];
								}

								location = location.getOpposite();
							}
						}

						entity.location = location;
						// entity.sprite.rotation = location.getAngle();
					}
					else
					{
						if ( symbol.containingRoom != null
							 && symbol.containingRoom.orientation != Direction.CENTER
							 && symbol.environmentData.getBoolean( "MatchRoomRotation", false ) )
						{
							entity.sprite.rotation = symbol.containingRoom.orientation.getAngle();
						}
					}

					newTile.addEnvironmentEntity( entity );
				}

				if ( !saveLevel.created && symbol.fieldData != null )
				{
					GameTile newTile = actualTiles[x][y];
					Field field = Field.load( symbol.fieldData.getText() );
					field.stacks = symbol.fieldData.getIntAttribute( "Stacks", 1 );
					newTile.addField( field );
				}

				if ( !saveLevel.created && symbol.hasGameEntity() )
				{
					GameTile newTile = actualTiles[x][y];
					GameEntity e = null;

					if (symbol.entityData instanceof String)
					{
						String entityPath = (String)symbol.entityData;
						FactionParser faction = fp;
						if (symbol.containingRoom != null && symbol.containingRoom.roomData != null && symbol.containingRoom.roomData.faction != null)
						{
							faction = FactionParser.load( symbol.containingRoom.roomData.faction );
						}

						if ( entityPath.equals( "Boss" ) )
						{
							e = GameEntity.load( faction.bosses.get( ran.nextInt( fp.bosses.size ) ) );
						}
						else if ( Global.isNumber( entityPath ) )
						{
							int index = Integer.parseInt( entityPath );

							index = (int) ( ( index / 9.0f ) * ( faction.creatures.size - 1 ) );

							e = GameEntity.load( faction.creatures.get( index ).entityName );
						}
						else
						{
							e = GameEntity.load( entityPath );
						}
					}
					else
					{
						Array<XmlReader.Element> data = new Array<XmlReader.Element>(  );
						data.add( ( XmlReader.Element)symbol.entityData );
						e = GameEntity.load( data );
					}

					if ( e != null )
					{
						newTile.addGameEntity( e );
						e.spawnPos = new Point( newTile );

						e.applyDepthScaling(quest.difficulty);
						e.isVariableMapDirty = true;
						e.HP = e.getMaxHP();
					}
				}
			}
		}

		saveLevel.addSavedLevelContents( level );

		level.UID = saveLevel.UID;

		level.calculateAmbient();

		return level;
	}

	// ----------------------------------------------------------------------
	public static AbstractDungeonGenerator load( SaveLevel level, Quest quest )
	{
		DungeonFileParser dfp = DungeonFileParser.load( quest.level + "/" + quest.level );

		Class<AbstractDungeonGenerator> c = ClassMap.get( dfp.generator.toUpperCase() );
		AbstractDungeonGenerator type = null;

		try
		{
			type = ClassReflection.newInstance( c );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		type.setup( level, quest, dfp );

		return type;
	}

	// ----------------------------------------------------------------------
	protected static HashMap<String, Class> ClassMap = new HashMap<String, Class>();

	// ----------------------------------------------------------------------
	static
	{
		ClassMap.put( "RECURSIVEDOCK", RecursiveDockGenerator.class );
		ClassMap.put( "STATIC", StaticLevelGenerator.class );
	}
}
