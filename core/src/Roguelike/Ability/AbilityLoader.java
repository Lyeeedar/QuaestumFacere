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
	public static IAbility loadAbility( String name )
	{
		XmlReader reader = new XmlReader();
		XmlReader.Element xmlElement = null;

		try
		{
			xmlElement = reader.parse( Gdx.files.internal( "Abilities/" + name + ".xml" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return loadAbility( xmlElement );
	}

	public static IAbility loadAbility( XmlReader.Element xml )
	{
		if (xml.getChildCount() == 0)
		{
			return loadAbility( xml.getText() );
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

			// Try first child
			XmlReader.Element child = xml.getChild( 0 );

			if (child.getName().equalsIgnoreCase("Active"))
			{
				return ActiveAbility.load( child );
			}
			else if (child.getName().equalsIgnoreCase( "Passive" ))
			{
				return PassiveAbility.load( child );
			}

			// Try self as active
			return ActiveAbility.load( xml );
		}
	}
}
