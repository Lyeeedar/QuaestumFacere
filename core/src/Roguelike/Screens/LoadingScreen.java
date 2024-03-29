package Roguelike.Screens;

import Roguelike.AssetManager;
import Roguelike.Global;
import Roguelike.Quests.Quest;
import Roguelike.RoguelikeGame;
import Roguelike.RoguelikeGame.ScreenEnum;
import Roguelike.DungeonGeneration.AbstractDungeonGenerator;
import Roguelike.Entity.GameEntity;
import Roguelike.Levels.Level;
import Roguelike.Save.SaveLevel;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class LoadingScreen implements Screen
{
	public static LoadingScreen Instance;
	boolean created = false;

	public LoadingScreen()
	{
		Instance = this;
	}

	private void create()
	{
		skin = Global.loadSkin();

		background = AssetManager.loadTexture( "Sprites/Unpacked/background.png" );
		background.setWrap( Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat );

		stage = new Stage( new ScreenViewport() );
		batch = new SpriteBatch();

		Table table = new Table();

		label = new Label( "Loading", skin, "title" );
		progressBar = new ProgressBar( 0, 100, 1, false, skin );

		table.add( progressBar ).expand().fillX().bottom().pad( 20 );
		table.row();
		table.add( label ).expand().top().pad( 20 );

		table.setFillParent( true );
		stage.addActor( table );
	}

	@Override
	public void show()
	{
		if ( !created )
		{
			create();
			created = true;
		}

		Gdx.input.setInputProcessor( stage );

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
		if ( !doCreate ) { return; }

		if ( complete && event != null )
		{
			generationString = "Executing Events";
		}

		label.setText( generationString );
		progressBar.setValue( percent );

		stage.act();

		Gdx.gl.glClearColor( 0.3f, 0.3f, 0.3f, 1 );
		Gdx.gl.glClear( GL20.GL_COLOR_BUFFER_BIT );

		batch.begin();
		batch.draw( background, 0, 0, stage.getWidth(), stage.getHeight(), 0, 0, stage.getWidth() / background.getWidth(), stage.getHeight() / background.getHeight() );
		batch.end();

		stage.draw();

		if ( complete )
		{
			onComplete( generator.getLevel() );
		}

		complete = generator.generate();
		percent = generator.percent;
		generationString = generator.generationText;

		// limit fps
		sleep( Global.FPS );
	}

	public void onComplete( Level level )
	{
		if ( event == null )
		{
			Global.changeLevel( level, player, travelData );

			Global.CurrentLevel.player.isVariableMapDirty = true;

			RoguelikeGame.Instance.switchScreen( ScreenEnum.GAME );
		}
		else
		{
			event.execute( level );
			event = null;
		}
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

		float w = Global.TargetResolution[0];
		float h = Global.TargetResolution[1];

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

	public boolean set( SaveLevel level, Quest quest, GameEntity player, Object travelData, PostGenerateEvent event )
	{
		this.player = player;
		this.travelData = travelData;
		this.event = event;
		this.level = level;
		this.quest = quest;

		generator = AbstractDungeonGenerator.load( level, quest );

		this.percent = generator.percent;
		this.generationString = generator.generationText;
		this.complete = false;
		doCreate = true;
		return true;
	}

	// ----------------------------------------------------------------------
	public OrthographicCamera camera;

	Stage stage;
	Skin skin;

	SpriteBatch batch;

	Label label;
	ProgressBar progressBar;

	boolean doCreate = false;
	boolean complete;
	String generationString;
	int percent = 0;
	SaveLevel level;
	Quest quest;
	GameEntity player;
	Object travelData;
	AbstractDungeonGenerator generator;
	PostGenerateEvent event;

	Texture background;

	public static abstract class PostGenerateEvent
	{
		public abstract void execute( Level level );
	}
}