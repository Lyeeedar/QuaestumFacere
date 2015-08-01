package Roguelike.StatusEffect;

import java.io.IOException;

import Roguelike.AssetManager;
import Roguelike.Entity.Entity;
import Roguelike.Entity.GameEntity;
import Roguelike.GameEvent.GameEventHandler;
import Roguelike.GameEvent.IGameObject;
import Roguelike.Sprite.Sprite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

public class StatusEffect extends GameEventHandler
{
	public String name;
	private String description;
	
	public Sprite icon;
	public float duration;
	public Entity attachedTo;
	
	@Override
	public void onTurn(Entity entity, float cost)
	{
		super.onTurn(entity, cost);
		
		duration -= cost;
	}
	
	public Table createTable(Skin skin)
	{
		Table table = new Table();
		
		table.add(new Label(name, skin)).expandX().left();
		table.row();
		
		Label descLabel = new Label(description, skin);
		descLabel.setWrap(true);
		table.add(descLabel).expand().left().width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(1, table));
		table.row();
		
		return table;
	}
		
	private void internalLoad(String name)
	{
		XmlReader xml = new XmlReader();
		Element xmlElement = null;
		
		try
		{
			xmlElement = xml.parse(Gdx.files.internal("StatusEffects/"+name+".xml"));
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		internalLoad(xmlElement);
	}
	
	private void internalLoad(Element xmlElement)
	{		
		String extendsElement = xmlElement.getAttribute("Extends", null);
		if (extendsElement != null)
		{
			internalLoad(extendsElement);
		}
		
		this.name = xmlElement.get("Name", this.name);
		description = xmlElement.get("Description", description);
		
		icon = xmlElement.getChildByName("Icon") != null ? AssetManager.loadSprite(xmlElement.getChildByName("Icon")) : icon;
		duration = xmlElement.getFloat("Duration", duration);
		
		Element eventsElement = xmlElement.getChildByName("Events");
		if (eventsElement != null)
		{
			super.parse(eventsElement);
		}
	}
	
	public static StatusEffect load(String name)
	{
		StatusEffect se = new StatusEffect();
		
		se.internalLoad(name);
		
		return se;
	}
	
	public static StatusEffect load(Element xml, IGameObject parent)
	{
		StatusEffect se = new StatusEffect();
	
		if (xml.getText() != null)
		{
			se.internalLoad(xml.getText());
		}
		else
		{
			se.internalLoad(xml);
			
			if (se.name == null)
			{
				se.name = parent.getName();
			}
			
			if (se.description == null)
			{
				se.description = parent.getDescription();
			}
			
			if (se.icon == null)
			{
				se.icon = parent.getIcon().copy();
			}
		}
		
		return se;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public Sprite getIcon()
	{
		return icon;
	}
}

/*
IDEAS

DOT
HOT
STAT BUFF
STAT DEBUFF
BLOCK ON HIT
DRAIN
DELAYED EXPLOSION
TELEPORT ON HIT
SLEEP
INSANITY
TURN DELAY
HASTE
STOP

*/