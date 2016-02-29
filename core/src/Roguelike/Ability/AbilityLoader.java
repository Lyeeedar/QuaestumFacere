package Roguelike.Ability;

import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.Ability.PassiveAbility.PassiveAbility;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.XmlReader;

import java.io.IOException;

/**
 * Created by Philip on 29-Feb-16.
 */
public class AbilityLoader
{
	public static IAbility loadAbility( XmlReader.Element xml )
	{
		if (xml.getChildCount() == 0)
		{
			XmlReader reader = new XmlReader();
			XmlReader.Element xmlElement = null;

			try
			{
				xmlElement = reader.parse( Gdx.files.internal( "Abilities/" + xml.getText() + ".xml" ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			if (xmlElement.getName().equalsIgnoreCase( "Active" ))
			{
				return ActiveAbility.load( xml.getText() );
			}
			else if (xmlElement.getName().equalsIgnoreCase( "Passive" ))
			{
				return PassiveAbility.load( xml.getText() );
			}
		}
		else
		{
			if (xml.getName().equalsIgnoreCase("Active"))
			{
				return ActiveAbility.load( xml );
			}
			else if (xml.getName().equalsIgnoreCase( "Passive" ))
			{
				return PassiveAbility.load( xml );
			}
		}

		return null;
	}
}
