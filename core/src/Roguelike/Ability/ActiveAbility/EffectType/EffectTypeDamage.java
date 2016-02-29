package Roguelike.Ability.ActiveAbility.EffectType;

import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.Entity.Entity;
import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Entity.GameEntity;
import Roguelike.Global;
import Roguelike.Global.Statistic;
import Roguelike.Items.Item;
import Roguelike.Tiles.GameTile;
import Roguelike.Util.FastEnumMap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;
import exp4j.Helpers.EquationHelper;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.HashMap;

public class EffectTypeDamage extends AbstractEffectType
{
	private FastEnumMap<Statistic, String> equations = new FastEnumMap<Statistic, String>( Statistic.class );
	private String[] reliesOn;

	private boolean scaled;

	@Override
	public void update( ActiveAbility aa, float time, GameTile tile, GameEntity entity, EnvironmentEntity envEntity )
	{
		if ( entity != null || envEntity != null )
		{
			HashMap<String, Integer> variableMap = calculateVariableMap( aa );

			if ( entity != null )
			{
				applyToEntity( entity, aa, variableMap );
			}

			if ( envEntity != null )
			{
				applyToEntity( envEntity, aa, variableMap );
			}
		}
	}

	@Override
	public void parse( Element xml )
	{
		reliesOn = xml.getAttribute( "ReliesOn", "" ).toLowerCase().split( "," );

		scaled = xml.getBooleanAttribute( "Scaled", false );

		for ( int i = 0; i < xml.getChildCount(); i++ )
		{
			Element sEl = xml.getChild( i );

			Statistic stat = Statistic.valueOf( sEl.getName().toUpperCase() );
			equations.put( stat, sEl.getText().toLowerCase() );
		}
	}

	@Override
	public AbstractEffectType copy()
	{
		EffectTypeDamage e = new EffectTypeDamage();
		e.equations = equations;
		e.reliesOn = reliesOn;
		e.scaled = scaled;
		return e;
	}

	@Override
	public Array<String> toString( ActiveAbility aa )
	{
		HashMap<String, Integer> variableMap = calculateVariableMap( aa );

		Array<String> lines = new Array<String>();

		float damage = variableMap.get( Statistic.ATTACK.toString() );

		lines.add( "Damage: " + (int)damage );

		lines.add( "---" );

		int pen = variableMap.get( Statistic.PENETRATION.toString().toLowerCase() );
		if (  pen > 0 )
		{
			lines.add( "Weapon Penetration: " + pen );
		}

		return lines;
	}

	private void applyToEntity( Entity target, ActiveAbility aa, HashMap<String, Integer> variableMap )
	{
		float damage = variableMap.get( Statistic.ATTACK.toString() );

		int pen = variableMap.get( Statistic.PENETRATION.toString().toLowerCase() );

		Global.calculateDamage( aa.getCaster(), target, (int)damage, target.getVariable( Statistic.DEFENSE ), pen, true );
	}

	private HashMap<String, Integer> calculateVariableMap( ActiveAbility aa )
	{
		HashMap<String, Integer> variableMap = aa.getVariableMap();

		for ( String name : reliesOn )
		{
			if ( !variableMap.containsKey( name ) )
			{
				variableMap.put( name, 0 );
			}
		}

		FastEnumMap<Statistic, Integer> stats = Statistic.getStatisticsBlock();

		for ( Statistic stat : Statistic.values() )
		{
			if ( equations.containsKey( stat ) )
			{
				String eqn = equations.get( stat );
				int raw = EquationHelper.evaluate( eqn, variableMap );

				stats.put( stat, raw );
			}
		}

		if (scaled)
		{
			float atk = stats.get( Statistic.ATTACK );
			float dam = aa.getVariableMap().get( Item.EquipmentSlot.WEAPON.toString() );
			int damage = (int)(dam * (atk / 100.0f));

			stats.put( Statistic.ATTACK, damage );
		}

		variableMap = Statistic.statsBlockToVariableBlock( stats );

		return variableMap;
	}
}
