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
		Table items = new Table(  );
		Table desc = new Table(  );

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

		table.add( slotsScrollPane ).expand().fill();
		table.add( itemsScrollPane ).expand().fill();
		table.add( desc ).expand().fill();

		for ( Item.EquipmentSlot slot : Item.EquipmentSlot.values() )
		{

		}
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

	Table table;

	Stage stage;

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
}
