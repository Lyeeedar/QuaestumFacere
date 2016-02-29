package Roguelike.Screens;

import Roguelike.AssetManager;
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

		Skin skin = Global.loadSkin();

		keyboardHelper = new ButtonKeyboardHelper(  );

		Table slots = new Table(  );

		ScrollPane slotsScrollPane = new ScrollPane( slots, skin );
		slotsScrollPane.setScrollingDisabled( true, false );
		slotsScrollPane.setVariableSizeKnobs( true );
		slotsScrollPane.setFadeScrollBars( false );
		slotsScrollPane.setScrollbarsOnTop( false );
		slotsScrollPane.setForceScroll( false, true );

		ScrollPane itemsScrollPane = new ScrollPane( items, skin );
		itemsScrollPane.setScrollingDisabled( true, false );
		itemsScrollPane.setVariableSizeKnobs( true );
		itemsScrollPane.setFadeScrollBars( false );
		itemsScrollPane.setScrollbarsOnTop( false );
		itemsScrollPane.setForceScroll( false, true );
		itemsScrollPane.setFlickScroll( false );

		table.add( slotsScrollPane ).expandY().fillY().width( Value.percentWidth( 1.0f/3.0f, table ) );
		table.add( itemsScrollPane ).expandY().fillY().width( Value.percentWidth( 1.0f/3.0f, table ) );
		table.add( desc ).expandY().fillY().width( Value.percentWidth( 1.0f/3.0f, table ) );

		for ( final Item.EquipmentSlot slot : Item.EquipmentSlot.values() )
		{
			Button button = new Button(skin);

			slots.add( button ).expandX().fillX();
			slots.row();

			buttonMap.put( slot, button );

			fillSlotButton(slot);
		}

		hideUtilities();
	}

	private void fillSlotButton( final Item.EquipmentSlot slot )
	{
		Skin skin = Global.loadSkin();
		Button button = buttonMap.get( slot );
		button.clear();

		Label slotLabel = new Label( Global.capitalizeString( slot.toString() ), skin );
		slotLabel.setFontScale( 0.5f );
		button.add( slotLabel ).expandX(  ).left();
		button.row();

		final Item current = slotMap.get( slot );
		Table itemTable = new Table(  );
		itemTable.defaults().pad( 5 );

		if (current != null)
		{
			SpriteWidget sprite = new SpriteWidget( current.getIcon(), 24, 24 );
			itemTable.add( sprite );
			itemTable.add( new Label( current.getName(), skin ) );
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
				fillItemTable();
			}
		} );

		button.add( itemTable ).expandX().fillX().left().height( 32 );
	}

	private void fillDescriptionTable( Item item, Item.EquipmentSlot slot)
	{
		desc.clear();
		desc.add( item.createTable( Global.loadSkin(), slotMap.get( slot ) ) ).expand().fill();
	}

	private void fillItemTable()
	{
		items.clear();

		Skin skin = Global.loadSkin();

		ButtonGroup<Button> group = new ButtonGroup<Button>(  );

		for (final Item item : Global.UnlockedItems)
		{
			if (item.slot == activeSlot)
			{
				Button button = new Button( skin );
				group.add( button );

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
						slotMap.put( activeSlot, item );
						fillSlotButton( activeSlot );
						hideUtilities();
					}
				} );

				SpriteWidget sprite = new SpriteWidget( item.getIcon(), 24, 24 );
				button.add( sprite );
				button.add( new Label( item.getName(), skin ) );
				button.row();

				items.add( button ).expandX().fillX();
				items.row();

				if (item == slotMap.get( activeSlot ))
				{
					button.setChecked( true );
				}
			}
		}
	}

	private void hideUtilities()
	{
		int numUtilSlots = slotMap.get( Item.EquipmentSlot.ARMOUR ) != null ? slotMap.get( Item.EquipmentSlot.ARMOUR ).utilSlots : 0;
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
	}

	// ----------------------------------------------------------------------
	public OrthographicCamera camera;

	boolean created;

	Table table;

	Stage stage;

	SpriteBatch batch;

	public InputMultiplexer inputMultiplexer;
	public ButtonKeyboardHelper keyboardHelper;

	Texture background;

	FastEnumMap<Item.EquipmentSlot, Item> slotMap = new FastEnumMap<Item.EquipmentSlot, Item>( Item.EquipmentSlot.class );
	FastEnumMap<Item.EquipmentSlot, Button> buttonMap = new FastEnumMap<Item.EquipmentSlot, Button>( Item.EquipmentSlot.class );
	Item.EquipmentSlot activeSlot;

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
	}

	@Override
	public boolean keyDown( int keycode )
	{
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
//		keyboardHelper.clear();
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
