package Roguelike.Entity.ActivationAction;

import Roguelike.Entity.Entity;
import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Global;
import Roguelike.RoguelikeGame;
import com.badlogic.gdx.utils.XmlReader;

/**
 * Created by Philip on 01-Mar-16.
 */
public class ActivationActionEndMission extends AbstractActivationAction
{
	@Override
	public void evaluate( EnvironmentEntity owningEntity, Entity activatingEntity, float delta )
	{
		Global.QuestManager.currentQuest.evaluateOutputs();
		RoguelikeGame.Instance.switchScreen( RoguelikeGame.ScreenEnum.HUB );
	}

	@Override
	public void parse( XmlReader.Element xml )
	{

	}
}
