package Roguelike.Entity.ActivationAction;

import Roguelike.Entity.Entity;
import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Lights.Light;
import com.badlogic.gdx.utils.XmlReader;

/**
 * Created by Philip on 06-Mar-16.
 */
public class ActivationActionSetLight extends AbstractActivationAction
{
	Light light;

	@Override
	public void evaluate( EnvironmentEntity owningEntity, Entity activatingEntity, float delta )
	{
		owningEntity.light = light;
	}

	@Override
	public void parse( XmlReader.Element xml )
	{
		if (xml.getChildCount() > 0)
		{
			light = Light.load( xml );
		}
	}
}
