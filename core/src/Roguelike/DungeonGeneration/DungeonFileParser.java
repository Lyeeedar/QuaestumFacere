package Roguelike.DungeonGeneration;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import Roguelike.DungeonGeneration.RecursiveDockGenerator.Room;
import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Pathfinding.PathfindingTile;
import Roguelike.Tiles.TileData;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

public class DungeonFileParser
{
	//----------------------------------------------------------------------
	public enum RoomGeneratorType
	{
		OVERLAPPINGRECTS,
		CELLULARAUTOMATA,
		STARBURST
	}
	
	//----------------------------------------------------------------------
	public static class CorridorStyle
	{
		public enum PathStyle
		{
			STRAIGHT,
			WANDERING
		}
		public PathStyle pathStyle = PathStyle.STRAIGHT;
		
		public int width = 2;
		
		public void parse(Element xml)
		{
			pathStyle = PathStyle.valueOf(xml.get("PathStyle", "Straight").toUpperCase());
			width = xml.getInt("Width", 1);
		}
	}
	
	//----------------------------------------------------------------------
	public static class RoomGenerator
	{
		public RoomGeneratorType type;
		public int weight;
	}
	
	//----------------------------------------------------------------------
	public static class Faction
	{
		public String name;
		public int weight;
		
		public Faction(String name, int weight)
		{
			this.name = name;
			this.weight = weight;
		}
	}
	
	//----------------------------------------------------------------------
	public static class DFPRoom
	{
		public int minDepth = 0;
		public int maxDepth = Integer.MAX_VALUE;
		
		public int width;
		public int height;
		public HashMap<Character, Symbol> localSymbolMap = new HashMap<Character, Symbol>();
		public HashMap<Character, Symbol> sharedSymbolMap;
		public char[][] roomDef;
		public String faction;
		
		public static DFPRoom parse(Element xml, HashMap<Character, Symbol> sharedSymbolMap)
		{
			DFPRoom room = new DFPRoom();
			room.sharedSymbolMap = sharedSymbolMap;
			
			room.minDepth = xml.getIntAttribute("Min", room.minDepth);
			room.maxDepth = xml.getIntAttribute("Max", room.maxDepth);
			
			room.faction = xml.get("Faction", null);
			
			Element rowsElement = xml.getChildByName("Rows");
			if (rowsElement.getChildCount() > 0)
			{
				// Rows defined here
				room.height = rowsElement.getChildCount();
				for (int i = 0; i < room.height; i++)
				{
					if (rowsElement.getChild(i).getText().length() > room.width)
					{
						room.width = rowsElement.getChild(i).getText().length();
					}
				}
				
				room.roomDef = new char[room.width][room.height];
				for (int x = 0; x < room.width; x++)
				{
					for (int y = 0; y < room.height; y++)
					{
						room.roomDef[x][y] = rowsElement.getChild(y).getText().charAt(x);
					}
				}
			}
			else
			{
				// Rows in seperate csv file
				String fileName = rowsElement.getText();
				FileHandle handle = Gdx.files.internal(fileName+".csv");
				String content = handle.readString();
				
				String[] lines = content.split(System.getProperty("line.separator"));
				room.height = lines.length;
				
				String[][] rows = new String[lines.length][];				
				for (int i = 0; i < lines.length; i++)
				{
					rows[i] = lines[i].split(" ");
					
					room.width = rows[i].length;
				}
				
				room.roomDef = new char[room.width][room.height];
				for (int x = 0; x < room.width; x++)
				{
					for (int y = 0; y < room.height; y++)
					{
						room.roomDef[x][y] = rows[x][y].charAt(0);
					}
				}
			}
						
			
			
			Element symbolsElement = xml.getChildByName("Symbols");
			if (symbolsElement != null)
			{
				for (int i = 0; i < symbolsElement.getChildCount(); i++)
				{
					Symbol symbol = Symbol.parse(symbolsElement.getChild(i), sharedSymbolMap, room.localSymbolMap);
					room.localSymbolMap.put(symbol.character, symbol);
				}
			}
			
			return room;
		}
		
		public void fillRoom(Room room)
		{
			room.width = width;
			room.height = height;
			room.roomContents = new Symbol[width][height];
			room.faction = faction;
			
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					char c = roomDef[x][y];
					Symbol s = localSymbolMap.get(c);
					if (s == null) { s = sharedSymbolMap.get(c); }
					if (s == null) 
					{ 
						//System.out.println("Failed to find symbol for character '" + c +"'! Falling back to using '.'");
						s = sharedSymbolMap.get('.'); 
					}
					
					if (s == null)
					{
						s = sharedSymbolMap.get('.'); 
					}
					
					room.roomContents[x][y] = s;
				}
			}
		}
	}
	
	//----------------------------------------------------------------------
	public HashMap<Character, Symbol> sharedSymbolMap = new HashMap<Character, Symbol>();
	
	//----------------------------------------------------------------------
	public Array<DFPRoom> requiredRooms = new Array<DFPRoom>();
	
	//----------------------------------------------------------------------
	public Array<DFPRoom> optionalRooms = new Array<DFPRoom>();
	
	//----------------------------------------------------------------------
	public Array<Faction> majorFactions = new Array<Faction>();
	
	//----------------------------------------------------------------------
	public Array<Faction> minorFactions = new Array<Faction>();
	
	//----------------------------------------------------------------------
	public Color ambient;
	
	//----------------------------------------------------------------------
	public Array<RoomGenerator> roomGenerators = new Array<RoomGenerator>();
	
	//----------------------------------------------------------------------
	public CorridorStyle corridorStyle = new CorridorStyle();
	
	//----------------------------------------------------------------------
	public Array<DFPRoom> getRequiredRooms(int depth)
	{
		Array<DFPRoom> rooms = new Array<DFPRoom>();
		
		for (DFPRoom room : requiredRooms)
		{
			if (room.minDepth <= depth && room.maxDepth >= depth)
			{
				rooms.add(room);
			}
		}
		
		return rooms;
	}
	
	//----------------------------------------------------------------------
	public String getMajorFaction(Random ran)
	{
		int totalWeight = 0;
		for (Faction fac : majorFactions)
		{
			totalWeight += fac.weight;
		}
		
		int ranVal = ran.nextInt(totalWeight);
		
		int currentWeight = 0;
		for (Faction fac : majorFactions)
		{
			currentWeight += fac.weight;
			
			if (currentWeight >= ranVal)
			{
				return fac.name;
			}
		}
		
		return null;
	}

	//----------------------------------------------------------------------
	public String getMinorFaction(Random ran)
	{
		int totalWeight = 0;
		for (Faction fac : minorFactions)
		{
			totalWeight += fac.weight;
		}

		int ranVal = ran.nextInt(totalWeight);

		int currentWeight = 0;
		for (Faction fac : minorFactions)
		{
			currentWeight += fac.weight;

			if (currentWeight >= ranVal)
			{
				return fac.name;
			}
		}

		return null;
	}
	
	//----------------------------------------------------------------------
	public RoomGeneratorType getRoomGenerator(Random ran)
	{
		int totalWeight = 0;
		for (RoomGenerator rg : roomGenerators)
		{
			totalWeight += rg.weight;
		}
		
		int target = ran.nextInt(totalWeight);
		int current = 0;
	
		for (RoomGenerator rg : roomGenerators)
		{
			current += rg.weight;
			
			if (current >= target)
			{
				return rg.type;
			}
		}
		
		return RoomGeneratorType.OVERLAPPINGRECTS;
	}
	
	//----------------------------------------------------------------------
	private void internalLoad(String name)
	{
		XmlReader xml = new XmlReader();
		Element xmlElement = null;

		try
		{
			xmlElement = xml.parse(Gdx.files.internal("Levels/"+name+".xml"));
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Element roomGenElement = xmlElement.getChildByName("RoomGenerators");
		if (roomGenElement != null)
		{
			for (int i = 0; i < roomGenElement.getChildCount(); i++)
			{
				Element roomGen = roomGenElement.getChild(i);
				
				RoomGenerator gen = new RoomGenerator();
				gen.type = RoomGeneratorType.valueOf(roomGen.getName().toUpperCase());
				gen.weight = Integer.parseInt(roomGen.getText());
				
				roomGenerators.add(gen);
			}
		}
		else
		{
			RoomGenerator gen = new RoomGenerator();
			gen.type = RoomGeneratorType.OVERLAPPINGRECTS;
			gen.weight = 1;
			
			roomGenerators.add(gen);
		}
		
		Element corridorElement = xmlElement.getChildByName("CorridorStyle");
		if (corridorElement != null)
		{
			corridorStyle.parse(corridorElement);
		}
		
		Element factionsElement = xmlElement.getChildByName("Factions");
		
		Element majorFacElement = factionsElement.getChildByName("Major");
		for (int i = 0; i < majorFacElement.getChildCount(); i++)
		{
			Element facElement = majorFacElement.getChild(i);
			
			String facname = facElement.getName();
			int weight = Integer.parseInt(facElement.getText());
			
			majorFactions.add(new Faction(facname, weight));
		}
		
		Element minorFacElement = factionsElement.getChildByName("Minor");
		for (int i = 0; i < minorFacElement.getChildCount(); i++)
		{
			Element facElement = minorFacElement.getChild(i);
			
			String facname = facElement.getName();
			int weight = Integer.parseInt(facElement.getText());
			
			minorFactions.add(new Faction(facname, weight));
		}
		
		
		Element symbolsElement = xmlElement.getChildByName("Symbols");
		for (int i = 0; i < symbolsElement.getChildCount(); i++)
		{
			Symbol symbol = Symbol.parse(symbolsElement.getChild(i), sharedSymbolMap, null);
			sharedSymbolMap.put(symbol.character, symbol);
		}
		
		Element requiredElement = xmlElement.getChildByName("Required");
		if (requiredElement != null)
		{
			for (int i = 0; i < requiredElement.getChildCount(); i++)
			{
				DFPRoom room = DFPRoom.parse(requiredElement.getChild(i), sharedSymbolMap);
				requiredRooms.add(room);
			}
		}
		
		Element optionalElement = xmlElement.getChildByName("Optional");
		if (optionalElement != null)
		{
			for (int i = 0; i < optionalElement.getChildCount(); i++)
			{
				DFPRoom room = DFPRoom.parse(optionalElement.getChild(i), sharedSymbolMap);
				optionalRooms.add(room);
			}
		}
		
		Element ae = xmlElement.getChildByName("Ambient");
		ambient = new Color(ae.getFloat("Red"), ae.getFloat("Blue"), ae.getFloat("Green"), ae.getFloat("Alpha"));
	}
	
	//----------------------------------------------------------------------
	public static DungeonFileParser load(String name)
	{
		DungeonFileParser dfp = new DungeonFileParser();
		
		dfp.internalLoad(name);
		
		return dfp;
	}
}