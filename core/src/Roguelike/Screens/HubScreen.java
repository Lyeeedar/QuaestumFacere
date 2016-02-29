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
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Created by Philip on 28-Feb-16.
 */
public class HubScreen implements Screen, InputProcessor
{

	public static HubScreen Instance;

	public HubScreen()
	{
		Instance = this;
	}

	public void create()
	{
		skin = Global.loadSkin();

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

		keyboardHelper = new ButtonKeyboardHelper(  );

		tabPanel = new TabPanel( skin );

		Array<UIWrapper> quests = new Array<UIWrapper>(  );
		Array<UIWrapper> items = new Array<UIWrapper>(  );

		Array<Quest> chosenQuests = Global.QuestManager.getQuests( );

		for (Quest quest : chosenQuests)
		{
			UIWrapper wrapper = new UIWrapper();
			wrapper.obj = quest;

			quests.add( wrapper );
		}

		for (int i = 0; i < 10; i++)
		{
			Item item = TreasureGenerator.generateWeapon( Global.QuestManager.difficulty, MathUtils.random ).get( 0 );

			UIWrapper wrapper = new UIWrapper();
			wrapper.obj = item;

			items.add(wrapper);
		}

		createTab( null, "Missions", quests );
		createTab( null, "Market", items );

		table.add( tabPanel ).colspan( 2 ).expand().fill().pad( 25 );
		table.row();
	}

	public void createTab(ButtonKeyboardHelper keyboardHelper, String title, Array<UIWrapper> items )
	{
		Table tab = new Table(  );
		Skin skin = Global.loadSkin();

		Table buttons = new Table(  );
		final Table content = new Table(  );

		for (final UIWrapper item : items)
		{
			Button button = item.getButton();

			button.addListener( new ClickListener(  )
			{
				public void clicked (InputEvent event, float x, float y)
				{
					content.clear();
					content.add( item.getContent() ).expand().fill();
				}
			} );

			buttons.add( button ).expandX().fillX();
			buttons.row();
		}

		ScrollPane scrollPane = new ScrollPane( buttons, skin );
		scrollPane.setScrollingDisabled( true, false );
		scrollPane.setVariableSizeKnobs( true );
		scrollPane.setFadeScrollBars( false );
		scrollPane.setScrollbarsOnTop( false );
		scrollPane.setForceScroll( false, true );
		scrollPane.setFlickScroll( false );

		tab.add( scrollPane ).width( Value.percentWidth( 0.4f, tab ) ).fillY();
		tab.add( content ).width( Value.percentWidth( 0.6f, tab ) ).fillY();

		tabPanel.addTab( title, tab );
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

	// ----------------------------------------------------------------------
	public OrthographicCamera camera;

	boolean created;

	TabPanel tabPanel;
	Table table;

	Stage stage;
	Skin skin;

	SpriteBatch batch;

	public InputMultiplexer inputMultiplexer;
	public ButtonKeyboardHelper keyboardHelper;

	Texture background;

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

	public class UIWrapper
	{
		public Object obj;

		private Sprite getIcon()
		{
			if (obj instanceof Quest)
			{
				Quest quest = (Quest)obj;
				return quest.icon;
			}
			else if (obj instanceof Item)
			{
				Item item = (Item)obj;
				return item.getIcon();
			}

			return null;
		}

		private String getName()
		{
			if (obj instanceof Quest)
			{
				Quest quest = (Quest)obj;
				return quest.name;
			}
			else if (obj instanceof Item)
			{
				Item item = (Item)obj;
				return item.getName();
			}

			return "";
		}

		private int getValue()
		{
			if (obj instanceof Quest)
			{
				Quest quest = (Quest)obj;
				return quest.reward;
			}
			else if (obj instanceof Item)
			{
				Item item = (Item)obj;
				return item.value;
			}

			return 0;
		}

		public Button getButton()
		{
			Skin skin = Global.loadSkin();
			Button button = new Button( skin );
			button.defaults().pad( 5 );

			SpriteWidget sprite = new SpriteWidget( getIcon(), 24, 24 );
			button.add( sprite );

			Table right = new Table(  );
			button.add( right ).expand().left();

			right.add( new Label( getName(), skin ) ).left();
			right.row();
			right.add( new Label( ""+getValue(), skin ) ).left();
			right.row();

			return button;
		}

		public Table getContent()
		{
			return new Table();
		}
	}
}
