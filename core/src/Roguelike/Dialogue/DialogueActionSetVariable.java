package Roguelike.Dialogue;

import Roguelike.Global;
import Roguelike.Dialogue.DialogueManager.ReturnType;

import com.badlogic.gdx.utils.XmlReader.Element;

public class DialogueActionSetVariable extends AbstractDialogueAction
{
	public enum VariableType
	{
		LOCAL, FLAG
	}

	private VariableType type;
	private String key;
	private String value;

	@Override
	public ReturnType process()
	{
		if ( type == VariableType.LOCAL )
		{
			manager.data.put( key, Integer.parseInt( value ) );
		}
		else if ( type == VariableType.FLAG )
		{
			Global.QuestManager.flags.put( key, Global.expandNames( value ) );
		}

		return ReturnType.ADVANCE;
	}

	@Override
	public void parse( Element xml )
	{
		type = VariableType.valueOf( xml.get( "Type", "local" ).toUpperCase() );
		key = xml.get( "Key" ).toLowerCase();
		value = xml.get( "Value" );
	}

}
