package Roguelike.Screens;

import Roguelike.AssetManager;
import Roguelike.Entity.GameEntity;
import Roguelike.GameEvent.IGameObject;
import Roguelike.Global;
import Roguelike.Items.Item;
import Roguelike.Items.TreasureGenerator;
import Roguelike.Quests.Quest;
import Roguelike.RoguelikeGame;
import Roguelike.Sprite.Sprite;
import Roguelike.UI.ButtonKeyboardHelper;
import Roguelike.UI.SpriteWidget;
import Roguelike.UI.TabPanel;
import Roguelike.Util.Controls;
import Roguelike.Util.FastEnumMap;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Created by Philip on 28-Feb-16.
 */
public class LoadoutScreen implements Screen, InputProcessor
{
	public static LoadoutScreen Instance;

	public LoadoutScreen()
	{
		Instance = this;
	}

	public void create()
	{
		background = AssetManager.loadTexture( "Sprites/GUI/background.png" );
		background.setWrap( Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat );

		stage = new Stage( new ScreenViewport() );
		batch = new SpriteBatch();

		table = new Table();
		table.defaults().pad( 5 );

		stage.addActor( table );
		table.setFillParent( true );

		inputMultiplexer = new InputMultiplexer();

		InputProcessor inputProcessorOne = this;
		InputProcessor inputProcessorTwo = stage;

		inputMultiplexer.addProcessor( inputProcessorTwo );
		inputMultiplexer.addProcessor( inputProcessorOne );
	}

	public void createUI()
	{
		table.clear();
		items.clear();
		desc.clear();

		items.defaults().pad( 2, 10, 2, 5 );

		Skin skin = Global.loadSkin();

		keyboardHelper = new ButtonKeyboardHelper(  );

		ScrollPane itemsScrollPane = new ScrollPane( items, skin );
		itemsScrollPane.setScrollingDisabled( true, false );
		itemsScrollPane.setVariableSizeKnobs( true );
		itemsScrollPane.setFadeScrollBars( false );
		itemsScrollPane.setScrollbarsOnTop( false );
		itemsScrollPane.setForceScroll( false, true );
		itemsScrollPane.setFlickScroll( false );

		table.add( itemsScrollPane ).expandY().fillY().width( Value.percentWidth( 0.4f, table ) );
		table.add( desc ).expandY().fillY().width( Value.percentWidth( 0.6f, table ) );

		for ( final Item.EquipmentSlot slot : Item.EquipmentSlot.values() )
		{
			Button button = new Button(skin);

			items.add( button ).expandX().fillX();
			items.row();

			buttonMap.put( slot, button );

			fillSlotButton(slot);
		}

		table.row();

		launchButton = new TextButton( "Launch Mission", skin );
		launchButton.addListener( new ClickListener(  )
		{
			public void clicked (InputEvent event, float x, float y)
			{
				GameEntity player = GameEntity.load( "player" );

				for ( Item.EquipmentSlot slot : Item.EquipmentSlot.values() )
				{
					Item item = Global.getLoadoutItem( slot );

					if (item != null)
					{
						player.inventory.equip( item );

						if (item.ability1 != null)
						{
							item.ability1.setCooldown( 0 );
							player.slottedAbilities.add( item.ability1 );
							item.ability1.setCaster( player );
						}

						if (item.ability2 != null)
						{
							item.ability2.setCooldown( 0 );
							player.slottedAbilities.add( item.ability2 );
							item.ability2.setCaster( player );
						}
					}
				}

				Global.QuestManager.currentQuest.createLevel( player );
			}
		} );

		table.add( launchButton ).colspan( 3 );
		table.row();

		hideUtilities();

		keyboardHelper = slotHelper;
	}

	private void fillSlotButton( final Item.EquipmentSlot slot )
	{
		Skin skin = Global.loadSkin();
		Button button = buttonMap.get( slot );
		button.clear();

		button.addListener( button.getClickListener() );

		Label slotLabel = new Label( Global.capitalizeString( slot.toString() ), skin );
		slotLabel.setFontScale( 0.6f );
		button.add( slotLabel ).expandX(  ).left();
		button.row();

		final Item current = Global.getLoadoutItem( slot );
		Table itemTable = new Table(  );
		itemTable.defaults().pad( 5 );
		button.add( itemTable ).width( Value.percentWidth( 0.8f, button ) ).height( 32 );

		if (current != null)
		{
			SpriteWidget sprite = new SpriteWidget( current.getIcon(), 24, 24 );
			itemTable.add( sprite );

			Label name = new Label( current.getName(), skin );
			name.setEllipsis( true );

			itemTable.add( name ).expandX().width( Value.percentWidth( 0.7f, itemTable ) ).left();
			itemTable.row();
		}

		button.addListener( new InputListener()
		{
			public void enter (InputEvent event, float x, float y, int pointer, Actor fromActor)
			{
				if (current != null)
				{
					fillDescriptionTable( current, slot );
				}
				else
				{
					desc.clear();
				}
			}
		} );

		button.addListener( new ClickListener(  )
		{
			public void clicked (InputEvent event, float x, float y)
			{
				activeSlot = slot;
				int i = -1;

				for (final Item item : Global.UnlockedItems)
				{
					if ( (activeSlot.isUtility() && item.slot.isUtility()) || item.slot == activeSlot )
					{
						if (!Global.Loadout.containsValue( item.hashCode ))
						{
							i++;
						}
					}
				}

				if (i >= 0)
				{
					fillItemTable();

					keyboardHelper = itemHelper;
					itemHelper.trySetCurrent( 0, i, 0 );
				}
				else
				{
					activeSlot = null;
				}
			}
		} );
	}

	private void fillDescriptionTable( Item item, Item.EquipmentSlot slot)
	{
		desc.clear();

		Skin skin = Global.loadSkin();
		ScrollPane scrollPane = new ScrollPane( item.createTable( skin, Global.getLoadoutItem( slot ) ), skin );
		scrollPane.setScrollingDisabled( true, false );
		desc.add( scrollPane ).expand().fill();
	}

	private void fillSlotTable()
	{
		items.clear();

		for ( final Item.EquipmentSlot slot : Item.EquipmentSlot.values() )
		{
			Button button = buttonMap.get( slot );

			items.add( button ).expandX().fillX();
			items.row();

			buttonMap.put( slot, button );

			fillSlotButton(slot);
		}

		table.row();

		hideUtilities();
	}

	private void fillItemTable( )
	{
		ScrollPane scrollPane = itemHelper.scrollPane;
		itemHelper.clearGrid();
		itemHelper.scrollPane = scrollPane;

		items.clear();

		Skin skin = Global.loadSkin();

		//ButtonGroup<Button> group = new ButtonGroup<Button>(  );

		for (final Item item : Global.UnlockedItems)
		{
			boolean display = false;

			if (activeSlot.isUtility())
			{
				if (item.slot.isUtility() && !Global.Loadout.containsValue( item.hashCode ))
				{
					display = true;
				}
			}
			else if (!Global.Loadout.containsValue( item.hashCode ))
			{
				display = item.slot == activeSlot;
			}

			if ( display )
			{
				Button button = new Button( skin, "toggle" );
				//group.add( button );

				button.addListener( new InputListener()
				{
					public void enter (InputEvent event, float x, float y, int pointer, Actor fromActor)
					{
						fillDescriptionTable( item, activeSlot );
					}
				} );

				button.addListener( new ClickListener(  )
				{
					public void clicked (InputEvent event, float x, float y)
					{
						slotHelper.trySetCurrent( 0, activeSlot.ordinal(), 0 );
						keyboardHelper = slotHelper;
						desc.clear();

						item.slot = activeSlot;
						Global.Loadout.put( activeSlot, item.hashCode );
						Global.save();

						fillSlotButton( activeSlot );
						fillSlotTable();
					}
				} );

				SpriteWidget sprite = new SpriteWidget( item.getIcon(), 24, 24 );
				button.add( sprite );

				Label name = new Label( item.getName(), skin );
				name.setEllipsis( true );

				button.add( name ).expandX().width( Value.percentWidth( 0.7f, button ) ).left();
				button.row();

				items.add( button ).expandX().fillX();
				items.row();

				if (item == Global.getLoadoutItem( activeSlot ))
				{
					//button.setChecked( true );
				}

				itemHelper.add( button );
			}
		}

		itemHelper.trySetCurrent(  );
	}

	private void hideUtilities()
	{
		ScrollPane scrollPane = slotHelper.scrollPane;
		slotHelper.clearGrid();

		Item armour = Global.getLoadoutItem( Item.EquipmentSlot.ARMOUR );
		int numUtilSlots = armour != null ? armour.utilSlots : 1;
		for (int i = 0; i < Item.EquipmentSlot.UtilitySlots.length; i++)
		{
			Item.EquipmentSlot slot = Item.EquipmentSlot.UtilitySlots[i];
			if (i < numUtilSlots)
			{
				buttonMap.get( slot ).setVisible( true );
			}
			else
			{
				buttonMap.get( slot ).setVisible( false );
			}
		}

		for ( Item.EquipmentSlot slot : Item.EquipmentSlot.values() )
		{
			Button button = buttonMap.get( slot );

			if (button.isVisible())
			{
				slotHelper.add( button );
			}
			else
			{
				Global.Loadout.put( slot, null );
			}
		}

		slotHelper.add( launchButton );

		slotHelper.scrollPane = scrollPane;
		slotHelper.trySetCurrent(  );
	}

	// ----------------------------------------------------------------------
	public OrthographicCamera camera;

	boolean created;

	Table table;

	Stage stage;

	SpriteBatch batch;

	public InputMultiplexer inputMultiplexer;

	public ButtonKeyboardHelper keyboardHelper;

	ButtonKeyboardHelper slotHelper = new ButtonKeyboardHelper(  );
	ButtonKeyboardHelper itemHelper = new ButtonKeyboardHelper(  );

	Texture background;

	FastEnumMap<Item.EquipmentSlot, Button> buttonMap = new FastEnumMap<Item.EquipmentSlot, Button>( Item.EquipmentSlot.class );
	Item.EquipmentSlot activeSlot;

	TextButton launchButton;

	Table items = new Table(  );
	Table desc = new Table(  );

	@Override
	public void render( float delta )
	{
		keyboardHelper.update( delta );
		stage.act();

		Gdx.gl.glClearColor( 0.3f, 0.3f, 0.3f, 1 );
		Gdx.gl.glClear( GL20.GL_COLOR_BUFFER_BIT );

		batch.begin();

		batch.draw( background, 0, 0, stage.getWidth(), stage.getHeight(), 0, 0, stage.getWidth() / background.getWidth(), stage.getHeight() / background.getHeight() );

		batch.end();

		stage.draw();

		// limit fps
		sleep( Global.FPS );
	}

	@Override
	public void show()
	{
		if ( !created )
		{
			create();
			created = true;
		}

		createUI();

		Gdx.input.setInputProcessor( inputMultiplexer );

		camera = new OrthographicCamera( Global.Resolution[0], Global.Resolution[1] );
		camera.translate( Global.Resolution[0] / 2, Global.Resolution[1] / 2 );
		camera.setToOrtho( false, Global.Resolution[0], Global.Resolution[1] );
		camera.update();

		batch.setProjectionMatrix( camera.combined );
		stage.getViewport().setCamera( camera );
		stage.getViewport().setWorldWidth( Global.Resolution[0] );
		stage.getViewport().setWorldHeight( Global.Resolution[1] );
		stage.getViewport().setScreenWidth( Global.ScreenSize[0] );
		stage.getViewport().setScreenHeight( Global.ScreenSize[1] );

		Global.changeBGM( "Voice Over Under" );
	}

	@Override
	public boolean keyDown( int keycode )
	{
		if (keyboardHelper != null)
		{
			keyboardHelper.keyDown( keycode );
		}

		return false;
	}

	@Override
	public boolean keyUp( int keycode )
	{
		return false;
	}

	@Override
	public boolean keyTyped( char character )
	{
		return false;
	}

	@Override
	public boolean touchDown( int screenX, int screenY, int pointer, int button )
	{
		return false;
	}

	@Override
	public boolean touchUp( int screenX, int screenY, int pointer, int button )
	{
		return false;
	}

	@Override
	public boolean touchDragged( int screenX, int screenY, int pointer )
	{
		return false;
	}

	@Override
	public boolean mouseMoved( int screenX, int screenY )
	{
		if (keyboardHelper != null)
		{
			keyboardHelper.clear();
		}
		return false;
	}

	@Override
	public boolean scrolled( int amount )
	{
		return false;
	}

	// ----------------------------------------------------------------------
	private long diff, start = System.currentTimeMillis();

	public void sleep( int fps )
	{
		if ( fps > 0 )
		{
			diff = System.currentTimeMillis() - start;
			long targetDelay = 1000 / fps;
			if ( diff < targetDelay )
			{
				try
				{
					Thread.sleep( targetDelay - diff );
				}
				catch ( InterruptedException e )
				{
				}
			}
			start = System.currentTimeMillis();
		}
	}

	@Override
	public void resize( int width, int height )
	{
		Global.ScreenSize[0] = width;
		Global.ScreenSize[1] = height;

		float w = 360;
		float h = 480;

		if ( width < height )
		{
			h = w * ( (float) height / (float) width );
		}
		else
		{
			w = h * ( (float) width / (float) height );
		}

		Global.Resolution[0] = (int) w;
		Global.Resolution[1] = (int) h;

		camera = new OrthographicCamera( Global.Resolution[0], Global.Resolution[1] );
		camera.translate( Global.Resolution[0] / 2, Global.Resolution[1] / 2 );
		camera.setToOrtho( false, Global.Resolution[0], Global.Resolution[1] );
		camera.update();

		batch.setProjectionMatrix( camera.combined );
		stage.getViewport().setCamera( camera );
		stage.getViewport().setWorldWidth( Global.Resolution[0] );
		stage.getViewport().setWorldHeight( Global.Resolution[1] );
		stage.getViewport().setScreenWidth( Global.ScreenSize[0] );
		stage.getViewport().setScreenHeight( Global.ScreenSize[1] );
	}

	@Override
	public void pause()
	{
	}

	@Override
	public void resume()
	{
	}

	@Override
	public void hide()
	{
	}

	@Override
	public void dispose()
	{
	}
}
