package Roguelike.Quests;

import Roguelike.AssetManager;
import Roguelike.DungeonGeneration.DungeonFileParser;
import Roguelike.Entity.GameEntity;
import Roguelike.Global;
import Roguelike.Quests.Input.AbstractQuestInput;
import Roguelike.Quests.Output.AbstractQuestOutputCondition;
import Roguelike.Quests.Output.QuestOutput;
import Roguelike.RoguelikeGame;
import Roguelike.Save.SaveLevel;
import Roguelike.Screens.HubScreen;
import Roguelike.Screens.LoadingScreen;
import Roguelike.Sprite.Sprite;
import Roguelike.UI.Seperator;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.XmlReader;

import java.io.IOException;

/**
 * Created by Philip on 23-Jan-16.
 */
public class Quest
{
	public String name;
	public String description;
	public Sprite icon;
	public int reward;
	public String faction;
	public String level;
	public Global.Rarity rarity;
	public int difficulty;

	public String path;
	public Array<AbstractQuestInput> inputs = new Array<AbstractQuestInput>(  );
	public Array<QuestOutput> outputs = new Array<QuestOutput>(  );
	public Array<DungeonFileParser.DFPRoom> rooms = new Array<DungeonFileParser.DFPRoom>(  );

	public boolean evaluateInputs()
	{
		for (AbstractQuestInput input : inputs)
		{
			if (!input.evaluate())
			{
				return false;
			}
		}

		return true;
	}

	public void evaluateOutputs()
	{
		int reward = -1;
		String message = null;

		for (QuestOutput output : outputs)
		{
			boolean succeed = output.evaluate();

			if (succeed)
			{
				if (output.reward > reward)
				{
					reward = output.reward;
					message = output.message;
				}
			}
		}

		HubScreen.Instance.showRewardMessage( message, reward );
	}

	public void parse( XmlReader.Element xml )
	{
		name = xml.get("Name");
		description = xml.get( "Description" );
		icon = AssetManager.loadSprite( xml.getChildByName( "Icon" ) );
		reward = xml.getInt( "Reward" );
		faction = xml.get( "Faction" );
		level = xml.get( "Level" );
		difficulty = xml.getInt( "Difficulty" );

		rarity = Global.Rarity.valueOf( xml.get("Rarity", "Common").toUpperCase() );

		XmlReader.Element inputsElement = xml.getChildByName( "Inputs" );
		if (inputsElement != null)
		{
			for (int i = 0; i < inputsElement.getChildCount(); i++)
			{
				XmlReader.Element inputElement = inputsElement.getChild( i );
				AbstractQuestInput input = AbstractQuestInput.load( inputElement );
				inputs.add( input );
			}
		}

		XmlReader.Element outputsElement = xml.getChildByName( "Outputs" );
		if (outputsElement != null)
		{
			for (int i = 0; i < outputsElement.getChildCount(); i++)
			{
				XmlReader.Element outputElement = outputsElement.getChild( i );
				QuestOutput output = new QuestOutput();
				output.parse( outputElement );

				outputs.add( output );
			}
		}

		XmlReader.Element roomsElement = xml.getChildByName( "Rooms" );
		for ( int i = 0; i < roomsElement.getChildCount(); i++ )
		{
			XmlReader.Element roomElement = roomsElement.getChild( i );
			DungeonFileParser.DFPRoom room = DungeonFileParser.DFPRoom.parse( roomElement );

			rooms.add( room );
		}
	}

	private DungeonFileParser.DFPRoom getPlayerShip()
	{
		XmlReader reader = new XmlReader();
		XmlReader.Element xml = null;

		try
		{
			xml = reader.parse( Gdx.files.internal( "Levels/ship.xml" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		DungeonFileParser.DFPRoom room = DungeonFileParser.DFPRoom.parse(xml);

		return room;
	}

	public void createLevel( GameEntity player )
	{
		Array<DungeonFileParser.DFPRoom> requiredRooms = new Array<DungeonFileParser.DFPRoom>(  );
		requiredRooms.add( getPlayerShip() );
		requiredRooms.addAll( rooms );

		SaveLevel level = new SaveLevel( name, requiredRooms, Global.QuestManager.seed );
		LoadingScreen.Instance.set( level, this, player, "playerspawn", null );
		RoguelikeGame.Instance.switchScreen( RoguelikeGame.ScreenEnum.LOADING );
	}

	public Table createTable( Skin skin )
	{
		Table table = new Table();

		table.defaults().pad( 10 );

		table.add( new Label( name, skin, "title" ) ).expandX().left();
		table.row();

		table.add( new Seperator( skin ) ).expandX().fillX();
		table.row();

		Label desc = new Label( description, skin );
		desc.setWrap( true );
		table.add( desc ).expandX().fillX().left();
		table.row();

		Label rew = new Label( "Reward: " + reward, skin );
		rew.setColor( Color.GOLD );

		table.add( rew ).expandX().left();
		table.row();

		return table;
	}

	public static Quest load(String name)
	{
		Quest quest = new Quest();
		quest.path = name;

		XmlReader reader = new XmlReader();
		XmlReader.Element xml = null;

		try
		{
			xml = reader.parse( Gdx.files.internal( "Quests/" + name + ".xml" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		quest.parse( xml );

		return quest;
	}
}
