package Roguelike;

import Roguelike.Entity.Entity;
import Roguelike.Entity.GameEntity;
import Roguelike.GameEvent.Damage.DamageObject;
import Roguelike.GameEvent.GameEventHandler;
import Roguelike.Items.Item;
import Roguelike.Items.TreasureGenerator;
import Roguelike.Levels.Level;
import Roguelike.Lights.Light;
import Roguelike.Quests.Quest;
import Roguelike.Quests.QuestManager;
import Roguelike.RoguelikeGame.ScreenEnum;
import Roguelike.Save.SaveFile;
import Roguelike.Save.SaveLevel;
import Roguelike.Screens.GameScreen;
import Roguelike.Screens.LoadingScreen;
import Roguelike.Sound.Mixer;
import Roguelike.Sound.RepeatingSoundEffect;
import Roguelike.Tiles.GameTile;
import Roguelike.Tiles.GameTile.LightData;
import Roguelike.Tiles.Point;
import Roguelike.UI.ClassList;
import Roguelike.UI.LayeredDrawable;
import Roguelike.UI.Seperator;
import Roguelike.UI.TabPanel;
import Roguelike.UI.Tooltip.TooltipStyle;
import Roguelike.Util.Controls;
import Roguelike.Util.EnumBitflag;
import Roguelike.Util.FastEnumMap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.XmlReader.Element;
import exp4j.Helpers.EquationHelper;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class Global
{
	// ----------------------------------------------------------------------
	public static final boolean CanMoveDiagonal = false;

	// ----------------------------------------------------------------------
	public static boolean MovementTypePathfind = false;

	// ----------------------------------------------------------------------
	public static float MusicVolume = 1;
	public static float AmbientVolume = 1;
	public static float EffectVolume = 1;

	// ----------------------------------------------------------------------
	public static RoguelikeGame Game;

	// ----------------------------------------------------------------------
	public static Controls Controls = new Controls();

	// ----------------------------------------------------------------------
	public static AbstractApplicationChanger ApplicationChanger;

	// ----------------------------------------------------------------------
	public static boolean ANDROID = false;
	public static boolean RELEASE = false;

	// ----------------------------------------------------------------------
	public static GameEntity CurrentDialogue = null;

	// ----------------------------------------------------------------------
	public static float AnimationSpeed = 1;

	// ----------------------------------------------------------------------
	public static int FPS = 0;

	// ----------------------------------------------------------------------
	public static int[] ScreenSize = { 600, 400 };

	// ----------------------------------------------------------------------
	public static int[] Resolution = { 600, 400 };

	// ----------------------------------------------------------------------
	public static int[] TargetResolution = { 600, 400 };

	// ----------------------------------------------------------------------
	public static int TileSize = 32;

	// ----------------------------------------------------------------------
	public static QuestManager QuestManager;
	public static Array<Item> UnlockedItems = new Array<Item>(  );
	public static int Funds = 500;
	public static Array<Item> Market = new Array<Item>(  );
	public static Array<Quest> Missions = new Array<Quest>(  );
	public static FastEnumMap<Item.EquipmentSlot, Item> Loadout = new FastEnumMap<Item.EquipmentSlot, Item>( Item.EquipmentSlot.class );

	// ----------------------------------------------------------------------
	public static Level CurrentLevel;

	// ----------------------------------------------------------------------
	public static Mixer BGM;

	// ----------------------------------------------------------------------
	public static Pool<Point> PointPool = Pools.get( Point.class, Integer.MAX_VALUE );
	public static Pool<Light> LightPool = Pools.get( Light.class, Integer.MAX_VALUE );
	public static Pool<LightData> LightDataPool = Pools.get( LightData.class, Integer.MAX_VALUE );

	// ----------------------------------------------------------------------
	static
	{
		Colors.put( "Name", Color.GREEN );
		Colors.put( "Place", Color.CYAN );
	}

	// ----------------------------------------------------------------------
	public static void updateVolume()
	{
		if (BGM != null) { BGM.updateVolume(); }
		if (CurrentLevel != null)
		{
			for (RepeatingSoundEffect rse : CurrentLevel.ambientSounds)
			{
				rse.updateVolume();
			}
		}
	}

	// ----------------------------------------------------------------------
	public static void fillMarket()
	{
		Market.clear();
		for (int i = 0; i < 10; i++)
		{
			int quality = Global.QuestManager.difficulty;
			quality += MathUtils.random( -1, 1 );
			if (quality < 1) { quality = 1; }

			Item item = TreasureGenerator.generateRandom( quality, MathUtils.random ).get( 0 );
			Market.add(item);
		}
	}

	// ----------------------------------------------------------------------
	public static void fillMissions()
	{
		Missions = Global.QuestManager.getQuests( );
	}

	// ----------------------------------------------------------------------
	public static SaveFile load()
	{
		SaveFile save = null;
		try
		{
			save = new SaveFile();
			save.load();
		}
		catch (Exception e)
		{
			return null;
		}

		Global.QuestManager = save.questManager;
		Global.UnlockedItems = save.unlockedItems;
		Global.Funds = save.funds;
		Global.Market = save.market;
		Global.Missions = save.missions;
		Global.Loadout = save.loadout;

		return save;
	}

	// ----------------------------------------------------------------------
	public static void delete()
	{
		FileHandle actualFile = Gdx.files.local( "save.dat" );
		actualFile.delete();
	}

	// ----------------------------------------------------------------------
	public static void save()
	{
		SaveFile save = new SaveFile();

		save.questManager = QuestManager;
		save.unlockedItems = UnlockedItems;
		save.funds = Funds;
		save.market = Market;
		save.missions = Missions;
		save.loadout = Loadout;

		if (save.questManager.currentLevel != null)
		{
			save.questManager.currentLevel.store( Global.CurrentLevel );
		}

		save.save();
	}

	// ----------------------------------------------------------------------
	public static void newWorld()
	{
		QuestManager = new QuestManager();
		UnlockedItems.clear();
		Funds = 500;
		Market.clear();
		Missions.clear();

		fillMissions();
		fillMarket();

		GameScreen.Instance.queueMessage( "Controls",
										  "Click in a direction to move there (or use the arrow keys)." +
										  " Click on your character to pass a turn (or press space). " +
										  "Move into an enemy to attack them, or into an NPC to talk to them." +
										  " Click an ability on the left to ready it, then on a blue target to use it.");
	}

	// ----------------------------------------------------------------------
	public static void changeBGM(String bgmName)
	{
		if ( BGM != null )
		{
			BGM.mix( bgmName, 1 );
		}
		else
		{
			BGM = new Mixer( bgmName, 1f );
		}
	}

	// ----------------------------------------------------------------------
	public static void changeLevel( Level level, GameEntity player, Object travelData )
	{
		if ( CurrentLevel != null )
		{
			for ( RepeatingSoundEffect sound : CurrentLevel.ambientSounds )
			{
				sound.stop();
			}

			CurrentLevel.player.tile[0][0].entity = CurrentLevel.player;
		}

		CurrentLevel = level;

		changeBGM(level.bgmName);

		if ( player != null )
		{
			boolean placed = false;
			level.player = player;

			if ( travelData instanceof String )
			{
				String travelKey = (String) travelData;

				outer:
					for ( int x = 0; x < level.width; x++ )
					{
						for ( int y = 0; y < level.height; y++ )
						{
							GameTile tile = level.getGameTile( x, y );
							if ( tile.metaValue != null )
							{
								if ( tile.metaValue.equals( travelKey ) )
								{
									tile.addGameEntity( player );
									placed = true;
									break outer;
								}
							}
						}
					}
			}
			else if ( travelData instanceof Point )
			{
				GameTile tile = level.getGameTile( (Point) travelData );
				tile.addGameEntity( player );
				placed = true;
			}

			if (!placed)
			{
				while (!placed)
				{
					int x = MathUtils.random( level.width );
					int y = MathUtils.random( level.height );

					GameTile tile = level.getGameTile( x, y );

					if (tile.getPassable( player.getTravelType(), player ))
					{
						tile.addGameEntity( player );
						placed = true;
					}
				}
			}
		}

		CurrentLevel.advance( 0 );

		save();
	}

	// ----------------------------------------------------------------------
	public static void calculateDamage( Entity attacker, Entity defender, int atk, int def, int pen, boolean doEvents )
	{
		if ( atk <= 0 ) { return; }

		// attack range
		atk += atk * MathUtils.random( -0.1f, 0.1f );

		int applicableDef = Math.max( 0, def - pen );

		float reduction = applicableDef / 100.0f;
		int damage = atk - (int)Math.ceil((float)atk * reduction);

		if ( damage <= 0 )
		{
			damage = 1;
		}

		DamageObject damObj = new DamageObject(damage);

		if ( doEvents )
		{
			for ( GameEventHandler handler : attacker.getAllHandlers() )
			{
				handler.onDealDamage( defender, damObj );
			}

			for ( GameEventHandler handler : defender.getAllHandlers() )
			{
				handler.onReceiveDamage( defender, damObj );
			}
		}

		defender.applyDamage( damObj.damage, attacker );
	}

	// ----------------------------------------------------------------------
	public static int getQuality()
	{
		return QuestManager.difficulty;
	}

	// ----------------------------------------------------------------------
	public static boolean isNumber( String string )
	{
		if ( string == null || string.isEmpty() ) { return false; }
		int i = 0;
		if ( string.charAt( 0 ) == '-' )
		{
			if ( string.length() > 1 )
			{
				i++;
			}
			else
			{
				return false;
			}
		}
		for ( ; i < string.length(); i++ )
		{
			if ( !Character.isDigit( string.charAt( i ) ) ) { return false; }
		}
		return true;
	}

	// ----------------------------------------------------------------------
	public static String expandNames( String input )
	{
		String[] split = input.split( "\\$" );

		boolean skip = !input.startsWith( "\\$" );

		String output = "";

		for ( int i = 0; i < split.length; i++ )
		{
			if ( skip )
			{
				output += split[i];
			}
			else if ( QuestManager.flags.containsKey( split[i].toLowerCase() ) )
			{
				output += QuestManager.flags.get( split[i].toLowerCase() );
			}

			skip = !skip;
		}

		return output;
	}

	// ----------------------------------------------------------------------
	public static String capitalizeString( String s )
	{
		String[] parts = s.split( "_" );

		String output = "";
		for (String part : parts)
		{
			output += part.substring( 0, 1 ).toUpperCase() + part.substring( 1 ).toLowerCase() + " ";
		}

		return output.trim();
	}

	// ----------------------------------------------------------------------
	public static int TaxiDist(Point p1, Point p2)
	{
		return TaxiDist( p1.x, p1.y, p2.x, p2.y );
	}

	// ----------------------------------------------------------------------
	public static int TaxiDist(int x1, int y1, int x2, int y2)
	{
		int xdiff = Math.abs( x1 - x2 );
		int ydiff = Math.abs( y1 - y2 );

		return Math.max( xdiff, ydiff );
	}

	// ----------------------------------------------------------------------
	public static String join( String seperator, Iterable<String> strings )
	{
		Iterator<String> itr = strings.iterator();
		String out = itr.next();

		while (itr.hasNext())
		{
			out += seperator + itr.next();
		}

		return out;
	}

	// ----------------------------------------------------------------------
	private static Skin skin = null;
	public static Skin loadSkin()
	{
		if (skin != null)
		{
			return skin;
		}

		skin = new Skin();

		BitmapFont font = AssetManager.loadFont( "Sprites/Unpacked/font.ttf", 12, new Color( 0.97f, 0.87f, 0.7f, 1 ), 1, Color.BLACK, false );
		skin.add( "default", font );

		BitmapFont titlefont = AssetManager.loadFont( "Sprites/Unpacked/font.ttf", 20, new Color( 1f, 0.9f, 0.8f, 1 ), 1, Color.BLACK, true );
		skin.add( "title", titlefont );

		Pixmap pixmap = new Pixmap( 1, 1, Format.RGBA8888 );
		pixmap.setColor( Color.WHITE );
		pixmap.fill();
		skin.add( "white", new Texture( pixmap ) );

		TextFieldStyle textField = new TextFieldStyle();
		textField.fontColor = Color.WHITE;
		textField.font = skin.getFont( "default" );
		textField.background = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/TextField.png" ), 6, 6, 6, 6 ) );
		textField.focusedBackground = ((NinePatchDrawable)textField.background).tint( new Color( 0.9f, 0.9f, 0.9f, 1.0f ) );
		textField.cursor = skin.newDrawable( "white", Color.WHITE );
		textField.selection = skin.newDrawable( "white", Color.LIGHT_GRAY );
		skin.add( "default", textField );

		LabelStyle label = new LabelStyle();
		label.font = skin.getFont( "default" );
		skin.add( "default", label );

		LabelStyle titleLabel = new LabelStyle();
		titleLabel.font = skin.getFont( "title" );
		skin.add( "title", titleLabel );

		CheckBoxStyle checkButton = new CheckBoxStyle();
		checkButton.checkboxOff = new TextureRegionDrawable( AssetManager.loadTextureRegion( "Sprites/GUI/Unchecked.png" ) );
		checkButton.checkboxOn = new TextureRegionDrawable( AssetManager.loadTextureRegion( "Sprites/GUI/Checked.png" ) );
		checkButton.font = skin.getFont( "default" );
		checkButton.fontColor = Color.LIGHT_GRAY;
		checkButton.overFontColor = Color.WHITE;
		skin.add( "default", checkButton );

		TextButton.TextButtonStyle textButton = new TextButton.TextButtonStyle();
		textButton.up = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) );
		textButton.font = skin.getFont( "default" );
		textButton.fontColor = Color.LIGHT_GRAY;
		textButton.overFontColor = Color.WHITE;
		//textButton.checked = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/ButtonDown.png" ), 12, 12, 12, 12 ) );
		textButton.over = ((NinePatchDrawable)textButton.up).tint( new Color( 0.9f, 0.9f, 0.9f, 1.0f ) );
		skin.add( "default", textButton );

		TextButton.TextButtonStyle bigTextButton = new TextButton.TextButtonStyle();
		bigTextButton.up = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) );
		bigTextButton.font = skin.getFont( "title" );
		bigTextButton.fontColor = Color.LIGHT_GRAY;
		bigTextButton.overFontColor = Color.WHITE;
		//bigTextButton.checked = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/ButtonDown.png" ), 12, 12, 12, 12 ) );
		bigTextButton.over = ((NinePatchDrawable)bigTextButton.up).tint( new Color( 0.9f, 0.9f, 0.9f, 1.0f ) );
		skin.add( "big", bigTextButton );

		TextButton.TextButtonStyle keyBindingButton = new TextButton.TextButtonStyle();
		keyBindingButton.up = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/TextField.png" ), 6, 6, 6, 6 ) );
		keyBindingButton.font = skin.getFont( "default" );
		keyBindingButton.fontColor = Color.LIGHT_GRAY;
		keyBindingButton.overFontColor = Color.WHITE;
		//textButton.checked = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/ButtonDown.png" ), 12, 12, 12, 12 ) );
		keyBindingButton.over = ((NinePatchDrawable)keyBindingButton.up).tint( new Color( 0.9f, 0.9f, 0.9f, 1.0f ) );
		skin.add( "keybinding", keyBindingButton );

		TooltipStyle toolTip = new TooltipStyle();
		toolTip.background = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Tooltip.png" ), 21, 21, 21, 21 ) );
		skin.add( "default", toolTip );

		ProgressBarStyle progressBar = new ProgressBarStyle();
		progressBar.background = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/TextField.png" ), 6, 6, 6, 6 ) );
		progressBar.knobBefore = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/ProgressIndicator.png" ), 8, 8, 8, 8 ) );
		skin.add( "default-horizontal", progressBar );

		Button.ButtonStyle buttonStyle = new Button.ButtonStyle(  );
		buttonStyle.up = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) );
		buttonStyle.over = ((NinePatchDrawable)buttonStyle.up).tint( new Color( 0.8f, 0.8f, 0.8f, 1.0f ) );
		skin.add( "default", buttonStyle );

		Button.ButtonStyle sheathButton = new Button.ButtonStyle(  );
		sheathButton.up = new LayeredDrawable(
				new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) ),
				new TextureRegionDrawable( AssetManager.loadTextureRegion( "Sprites/GUI/Unsheathed.png" ) ) );
		sheathButton.checked = new LayeredDrawable(
				new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) ),
				new TextureRegionDrawable( AssetManager.loadTextureRegion( "Sprites/GUI/Sheathed.png" ) ) );
		skin.add( "sheath", sheathButton );

		Button.ButtonStyle examineButton = new Button.ButtonStyle(  );
		examineButton.up = new LayeredDrawable(
				new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) ),
				new TextureRegionDrawable( AssetManager.loadTextureRegion( "Sprites/GUI/QuestionMark.png" ) ) );
		examineButton.checked = new LayeredDrawable(
				new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) ),
				new TextureRegionDrawable( AssetManager.loadTextureRegion( "Sprites/GUI/Eye.png" ) ) );
		skin.add( "examine", examineButton );

		Button.ButtonStyle menuButton = new Button.ButtonStyle(  );
		menuButton.up = new LayeredDrawable(
				new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) ),
				new TextureRegionDrawable( AssetManager.loadTextureRegion( "Sprites/GUI/Cog.png" ) ) );
		skin.add( "menu", menuButton );

		Seperator.SeperatorStyle horiSeperatorStyle = new Seperator.SeperatorStyle(  );
		horiSeperatorStyle.vertical = false;
		horiSeperatorStyle.thickness = 6;
		horiSeperatorStyle.background = new TextureRegionDrawable( AssetManager.loadTextureRegion( "Sprites/GUI/SeperatorHorizontal.png" ) );
		skin.add( "horizontal", horiSeperatorStyle );

		Seperator.SeperatorStyle vertSeperatorStyle = new Seperator.SeperatorStyle(  );
		vertSeperatorStyle.vertical = true;
		vertSeperatorStyle.thickness = 6;
		vertSeperatorStyle.background = new TextureRegionDrawable( AssetManager.loadTextureRegion( "Sprites/GUI/SeperatorVertical.png" ) );
		skin.add( "vertical", vertSeperatorStyle );

		ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle(  );
		scrollPaneStyle.vScroll = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/TextField.png" ), 6, 6, 6, 6 ) );
		scrollPaneStyle.vScrollKnob = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) );
		skin.add( "default", scrollPaneStyle );

		List.ListStyle listStyle = new List.ListStyle(  );
		listStyle.background = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Tooltip.png" ), 21, 21, 21, 21 ) );
		listStyle.font = skin.getFont( "default" );
		listStyle.selection = skin.newDrawable( "white", Color.LIGHT_GRAY );
		skin.add( "default", listStyle );

		SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle(  );
		selectBoxStyle.fontColor = Color.WHITE;
		selectBoxStyle.font = skin.getFont( "default" );
		selectBoxStyle.background = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/TextField.png" ), 6, 6, 6, 6 ) );
		selectBoxStyle.scrollStyle = scrollPaneStyle;
		selectBoxStyle.listStyle = listStyle;
		selectBoxStyle.backgroundOver = ((NinePatchDrawable)selectBoxStyle.background).tint( new Color( 0.9f, 0.9f, 0.9f, 1.0f ) );
		skin.add( "default", selectBoxStyle );

		Slider.SliderStyle sliderStyle = new Slider.SliderStyle(  );
		sliderStyle.background = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/TextField.png" ), 6, 6, 6, 6 ) );
		sliderStyle.knob = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) );
		sliderStyle.knobOver = ((NinePatchDrawable)sliderStyle.knob).tint( new Color( 0.9f, 0.9f, 0.9f, 1.0f ) );
		sliderStyle.knobDown = ((NinePatchDrawable)sliderStyle.knob).tint( Color.LIGHT_GRAY );
		skin.add( "default-horizontal", sliderStyle );

		TabPanel.TabPanelStyle tabPanelStyle = new TabPanel.TabPanelStyle(  );
		tabPanelStyle.font = skin.getFont( "default" );
		tabPanelStyle.fontColor = Color.LIGHT_GRAY;
		tabPanelStyle.overFontColor = Color.WHITE;
		tabPanelStyle.bodyBackground = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/TextField.png" ), 6, 6, 6, 6 ) ).tint( new Color( 1, 1, 1, 0.9f ) );
		tabPanelStyle.titleButtonUnselected = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/Button.png" ), 12, 12, 12, 12 ) );
		tabPanelStyle.titleButtonSelected = ((NinePatchDrawable)tabPanelStyle.titleButtonUnselected).tint( new Color( 0.8f, 0.8f, 0.8f, 1.0f ) );
		skin.add( "default", tabPanelStyle );

		return skin;
	}

	// ----------------------------------------------------------------------
	public enum Rarity
	{
		VERYCOMMON,
		COMMON,
		UNCOMMON,
		RARE,
		LEGENDARY,
		MYTHICAL
	}

	// ----------------------------------------------------------------------
	public enum ScaleLevel
	{
		F,
		E,
		D,
		C,
		B,
		A,
		S,
		SS
	}

	// ----------------------------------------------------------------------
	public enum Direction
	{
		CENTER( 0, 0, "C" ),
		NORTH( 0, 1, "N" ),
		SOUTH( 0, -1, "S" ),
		EAST( 1, 0, "E" ),
		WEST( -1, 0, "W" ),
		NORTHEAST( 1, 1, "NE" ),
		NORTHWEST( -1, 1, "NW" ),
		SOUTHEAST( 1, -1, "SE" ),
		SOUTHWEST(-1, -1, "SW" );

		static
		{
			// Setup neighbours
			Direction.CENTER.clockwise = Direction.CENTER;
			Direction.CENTER.anticlockwise = Direction.CENTER;

			Direction.NORTH.anticlockwise = Direction.NORTHWEST;
			Direction.NORTH.clockwise = Direction.NORTHEAST;

			Direction.NORTHEAST.anticlockwise = Direction.NORTH;
			Direction.NORTHEAST.clockwise = Direction.EAST;

			Direction.EAST.anticlockwise = Direction.NORTHEAST;
			Direction.EAST.clockwise = Direction.SOUTHEAST;

			Direction.SOUTHEAST.anticlockwise = Direction.EAST;
			Direction.SOUTHEAST.clockwise = Direction.SOUTH;

			Direction.SOUTH.anticlockwise = Direction.SOUTHEAST;
			Direction.SOUTH.clockwise = Direction.SOUTHWEST;

			Direction.SOUTHWEST.anticlockwise = Direction.SOUTH;
			Direction.SOUTHWEST.clockwise = Direction.WEST;

			Direction.WEST.anticlockwise = Direction.SOUTHWEST;
			Direction.WEST.clockwise = Direction.NORTHWEST;

			Direction.NORTHWEST.anticlockwise = Direction.WEST;
			Direction.NORTHWEST.clockwise = Direction.NORTH;

			// Setup is cardinal
			Direction.NORTH.isCardinal = true;
			Direction.SOUTH.isCardinal = true;
			Direction.EAST.isCardinal = true;
			Direction.WEST.isCardinal = true;
		}

		public final String identifier;
		private final int x;
		private final int y;
		private final float angle; // In degrees
		private Direction clockwise;
		private Direction anticlockwise;
		private boolean isCardinal = false;

		Direction( int x, int y, String identifier )
		{
			this.x = x;
			this.y = y;
			this.identifier = identifier;

			// basis vector = 0, 1
			double dot = 0 * x + 1 * y; // dot product
			double det = 0 * y - 1 * x; // determinant
			angle = (float) Math.atan2( det, dot ) * MathUtils.radiansToDegrees;
		}

		public static Direction getDirection( int[] dir )
		{
			return getDirection( dir[0], dir[1] );
		}

		public static Direction getDirection( int dx, int dy )
		{
			dx = MathUtils.clamp( dx, -1, 1 );
			dy = MathUtils.clamp( dy, -1, 1 );

			Direction d = Direction.CENTER;

			for ( Direction dir : Direction.values() )
			{
				if ( dir.getX() == dx && dir.getY() == dy )
				{
					d = dir;
					break;
				}
			}

			return d;
		}

		public static Direction getCardinalDirection( int dx, int dy )
		{
			if (dx == 0 && dy == 0)
			{
				return Direction.CENTER;
			}

			if (Math.abs( dx ) > Math.abs( dy ))
			{
				if (dx < 0)
				{
					return Direction.WEST;
				}
				else
				{
					return Direction.EAST;
				}
			}
			else
			{
				if (dy < 0)
				{
					return Direction.SOUTH;
				}
				else
				{
					return Direction.NORTH;
				}
			}
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		public static Direction getDirection( GameTile t1, GameTile t2 )
		{
			return getDirection( t2.x - t1.x, t2.y - t1.y );
		}

		public static Array<Point> buildCone( Direction dir, Point start, int range )
		{
			Array<Point> hitTiles = new Array<Point>();

			Direction anticlockwise = dir.getAnticlockwise();
			Direction clockwise = dir.getClockwise();

			Point acwOffset = PointPool.obtain().set( dir.getX() - anticlockwise.getX(), dir.getY() - anticlockwise.getY() );
			Point cwOffset = PointPool.obtain().set( dir.getX() - clockwise.getX(), dir.getY() - clockwise.getY() );

			hitTiles.add( PointPool.obtain().set( start.x + anticlockwise.getX(), start.y + anticlockwise.getY() ) );

			hitTiles.add( PointPool.obtain().set( start.x + dir.getX(), start.y + dir.getY() ) );

			hitTiles.add( PointPool.obtain().set( start.x + clockwise.getX(), start.y + clockwise.getY() ) );

			for ( int i = 2; i <= range; i++ )
			{
				int acx = start.x + anticlockwise.getX() * i;
				int acy = start.y + anticlockwise.getY() * i;

				int nx = start.x + dir.getX() * i;
				int ny = start.y + dir.getY() * i;

				int cx = start.x + clockwise.getX() * i;
				int cy = start.y + clockwise.getY() * i;

				// add base tiles
				hitTiles.add( PointPool.obtain().set( acx, acy ) );
				hitTiles.add( PointPool.obtain().set( nx, ny ) );
				hitTiles.add( PointPool.obtain().set( cx, cy ) );

				// add anticlockwise - mid
				for ( int ii = 1; ii <= range; ii++ )
				{
					int px = acx + acwOffset.x * ii;
					int py = acy + acwOffset.y * ii;

					hitTiles.add( PointPool.obtain().set( px, py ) );
				}

				// add mid - clockwise
				for ( int ii = 1; ii <= range; ii++ )
				{
					int px = cx + cwOffset.x * ii;
					int py = cy + cwOffset.y * ii;

					hitTiles.add( PointPool.obtain().set( px, py ) );
				}
			}

			PointPool.free( acwOffset );
			PointPool.free( cwOffset );

			return hitTiles;
		}

		public Direction getClockwise()
		{
			return clockwise;
		}

		public Direction getAnticlockwise()
		{
			return anticlockwise;
		}

		public boolean isCardinal()
		{
			return isCardinal;
		}

		public float getAngle()
		{
			return angle;
		}

		public Direction getOpposite()
		{
			return getDirection( x * -1, y * -1 );
		}
	}

	// ----------------------------------------------------------------------
	public enum Passability
	{
		WALK( Statistic.WALK ), LEVITATE( Statistic.LEVITATE ), LIGHT( Statistic.LIGHT ), ENTITY( Statistic.ENTITY );

		public final Statistic stat;

		Passability( Statistic stat )
		{
			this.stat = stat;
		}

		public static EnumBitflag<Passability> variableMapToTravelType( HashMap<String, Integer> stats )
		{
			EnumBitflag<Passability> travelType = new EnumBitflag<Passability>();

			for ( Passability p : Passability.values() )
			{
				String checkString = p.stat.toString().toLowerCase();
				if ( stats.containsKey( checkString ) )
				{
					int val = stats.get( checkString );

					if ( val > 0 )
					{
						travelType.setBit( p );
					}
				}
			}

			return travelType;
		}

		public static EnumBitflag<Passability> parse( String passable )
		{
			EnumBitflag<Passability> passableBy = new EnumBitflag<Passability>();

			if ( passable != null )
			{
				if ( passable.equalsIgnoreCase( "true" ) )
				{
					// all
					for ( Passability p : Passability.values() )
					{
						passableBy.setBit( p );
					}
				}
				else if ( passable.equalsIgnoreCase( "false" ) )
				{
					// none
				}
				else
				{
					String[] split = passable.split( "," );
					for ( String p : split )
					{
						passableBy.setBit( Passability.valueOf( p.toUpperCase() ) );
					}
				}
			}

			return passableBy;
		}

		public static EnumBitflag<Passability> parseArray( String passable )
		{
			EnumBitflag<Passability> passableBy = new EnumBitflag<Passability>();

			if ( passable != null )
			{
				if ( passable.equalsIgnoreCase( "true" ) )
				{
					// all
					for ( Passability p : Passability.values() )
					{
						passableBy.setBit( p );
					}
				}
				else if ( passable.equalsIgnoreCase( "false" ) )
				{
					// none
				}
				else
				{
					String[] split = passable.split( "," );
					for ( String p : split )
					{
						passableBy.setBit( Passability.valueOf( p.toUpperCase() ) );
					}
				}
			}

			return passableBy;
		}

	}

	// ----------------------------------------------------------------------
	public enum Statistic
	{
		// Passability
		WALK,
		LEVITATE,
		LIGHT,
		ENTITY,

		// Damage stats
		ATTACK, // Base Damage
		DEFENSE, // Defense
		PENETRATION, // Penetration
		ACCURACY, // Chance to hit

		// Other stats
		VITALITY, // Health
		SIGHT, // sight range
		SPEED; // Speed

		public static Statistic[] PassabilityValues = { Statistic.WALK, Statistic.LEVITATE, Statistic.LIGHT, Statistic.ENTITY };

		public static Statistic[] DamageValues = { Statistic.ATTACK, Statistic.DEFENSE, Statistic.PENETRATION, Statistic.ACCURACY };

		public static Statistic[] OtherValues = {
			Statistic.VITALITY,
			Statistic.SIGHT,
			Statistic.SPEED };

		public static HashMap<String, Integer> emptyMap = new HashMap<String, Integer>();
		static
		{
			for ( Statistic s : Statistic.values() )
			{
				emptyMap.put( s.toString().toLowerCase(), 0 );
			}
		}

		public static HashMap<String, Integer> statsBlockToVariableBlock( FastEnumMap<Statistic, Integer> stats )
		{
			HashMap<String, Integer> variableMap = new HashMap<String, Integer>();

			for ( Statistic key : Statistic.values() )
			{
				Integer val = stats.get( key );
				if ( val != null )
				{
					variableMap.put( key.toString().toLowerCase(), val );
				}
			}

			return variableMap;
		}

		public static FastEnumMap<Statistic, Integer> getStatisticsBlock()
		{
			FastEnumMap<Statistic, Integer> stats = new FastEnumMap<Statistic, Integer>( Statistic.class );

			for ( Statistic stat : Statistic.values() )
			{
				stats.put( stat, 0 );
			}

			return stats;
		}

		public static FastEnumMap<Statistic, Integer> load( Element xml, FastEnumMap<Statistic, Integer> values )
		{
			for ( int i = 0; i < xml.getChildCount(); i++ )
			{

				Element el = xml.getChild( i );

				Statistic stat = Statistic.valueOf( el.getName().toUpperCase() );
				String eqn = el.getText().toLowerCase();

				int newVal = values.get( stat );

				if ( Global.isNumber( eqn ) )
				{
					newVal = Integer.parseInt( eqn );
				}
				else
				{
					ExpressionBuilder expB = EquationHelper.createEquationBuilder( eqn );
					expB.variable( "value" );
					expB.variable( "val" );

					Expression exp = EquationHelper.tryBuild( expB );
					if ( exp != null )
					{
						exp.setVariable( "value", newVal );
						exp.setVariable( "val", newVal );

						newVal = (int) exp.evaluate();
					}
				}

				values.put( stat, newVal );
			}

			return values;
		}

		public static FastEnumMap<Statistic, Integer> copy( FastEnumMap<Statistic, Integer> map )
		{
			FastEnumMap<Statistic, Integer> newMap = new FastEnumMap<Statistic, Integer>( Statistic.class );
			for ( Statistic e : Statistic.values() )
			{
				newMap.put( e, map.get( e ) );
			}
			return newMap;
		}

		public static String formatString( String input )
		{
			String[] words = input.split( "_" );

			String output = "";

			for ( String word : words )
			{
				word = word.toLowerCase();
				output += word + " ";
			}

			return output.trim();
		}
	}
}
