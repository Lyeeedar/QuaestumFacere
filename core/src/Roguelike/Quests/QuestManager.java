package Roguelike.Quests;

import Roguelike.Global;
import Roguelike.Quests.Input.AbstractQuestInput;
import Roguelike.Save.SaveLevel;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.XmlReader;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by Philip on 23-Jan-16.
 */
public class QuestManager
{
	public Array<Quest> availableQuests = new Array<Quest>(  );

	public ObjectMap<String, String> flags = new ObjectMap<String, String>();

	public int difficulty = 1;

	public SaveLevel currentLevel;

	public int seed;

	public Quest currentQuest;

	public QuestManager()
	{
		XmlReader reader = new XmlReader();
		XmlReader.Element xml = null;

		try
		{
			xml = reader.parse( Gdx.files.internal( "Quests/QuestList.xml" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		for (int i = 0; i < xml.getChildCount(); i++)
		{
			XmlReader.Element questEl = xml.getChild( i );
			Quest quest = Quest.load( questEl.getText() );
			availableQuests.add( quest );
		}
	}

	public Array<Quest> getQuests( )
	{
		Random ran = new Random( seed );

		Array<Quest> validQuests = new Array<Quest>(  );
		for (Quest quest : availableQuests)
		{
			if (Math.abs( quest.difficulty - difficulty ) <= 1 && quest.evaluateInputs())
			{
				int rarity = (Global.Rarity.values().length - quest.rarity.ordinal()) + 1;
				for (int i = 0; i < rarity; i++)
				{
					validQuests.add( quest );
				}
			}
		}

		if (validQuests.size == 0)
		{
			throw new RuntimeException( "No Valid quests! For difficulty "  +difficulty );
		}

		Array<Quest> chosen = new Array<Quest>(  );

		int count = 3 + ran.nextInt( 2 );
		for (int i = 0; i < count; i++)
		{
			Quest picked = validQuests.get( ran.nextInt( validQuests.size ) );
			chosen.add( picked );

			Iterator<Quest> itr = validQuests.iterator();
			while (itr.hasNext())
			{
				if (itr.next() == picked)
				{
					itr.remove();
				}
			}

			if (validQuests.size == 0)
			{
				break;
			}
		}

		return chosen;
	}
}
