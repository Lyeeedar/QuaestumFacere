package Roguelike.Entity.ActivationAction;

import Roguelike.Entity.Entity;
import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Global;
import Roguelike.Lights.Light;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;

/**
 * Created by Philip on 06-Mar-16.
 */
public class ActivationActionSetLight extends AbstractActivationAction
{
	public String entityName;
	public int maxDist;

	Light light;

	@Override
	public void evaluate( EnvironmentEntity owningEntity, Entity activatingEntity, float delta )
	{
		if (entityName != null)
		{
			Array<EnvironmentEntity> all = new Array<EnvironmentEntity>(  );
			owningEntity.tile[0][0].level.getAllEnvironmentEntities( all );

			for (EnvironmentEntity ee : all)
			{
				if ( ee.name.equals( entityName ) && Global.TaxiDist( owningEntity.tile[0][0], ee.tile[0][0] ) <= maxDist )
				{
					ee.light = light;
				}
			}
		}
		else
		{
			owningEntity.light = light;
		}
	}

	@Override
	public void parse( XmlReader.Element xml )
	{
		entityName = xml.getAttribute( "Entity", null );
		maxDist = xml.getIntAttribute( "MaxDist", Integer.MAX_VALUE );

		if (xml.getChildCount() > 0)
		{
			light = Light.load( xml );
		}
	}
}
