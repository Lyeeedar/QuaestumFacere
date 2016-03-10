package Roguelike.Screens;

import Roguelike.AssetManager;
import Roguelike.GameEvent.IGameObject;
import Roguelike.Global;
import Roguelike.Items.Item;
import Roguelike.Items.TreasureGenerator;
import Roguelike.Quests.Quest;
import Roguelike.RoguelikeGame;
import Roguelike.Sprite.Sprite;
import Roguelike.UI.*;
import Roguelike.UI.Tooltip;
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
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.io.IOException;

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

		lastFunds = Global.Funds;

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

	// ----------------------------------------------------------------------
	public void clearContextMenu( )
	{
		if (keyboardHelper != null)
		{
			keyboardHelper.clear();
			keyboardHelper = null;
		}

		if ( contextMenu != null )
		{
			contextMenu.addAction( new SequenceAction( Actions.fadeOut( 0.25f ), Actions.removeActor() ) );
			contextMenu = null;

			if (contextMenuQueue.size > 0)
			{
				GameScreen.ContextMenuData data = contextMenuQueue.removeIndex( 0 );
				displayContextMenu( data.content, data.keyboardHelper );
			}
			else
			{
				createUI();
			}
		}
	}

	// ----------------------------------------------------------------------
	public void displayContextMenu(Table content, ButtonKeyboardHelper keyboardHelper)
	{
		if ( !created )
		{
			create();
			created = true;
		}

		table.clear();
		missionList.clear();
		missionContent.clear();
		marketList.clear();
		marketContent.clear();
		stashList.clear();
		stashContent.clear();

		missionHelper.clearGrid();
		marketHelper.clearGrid();
		stashHelper.clearGrid();

		Skin skin = Global.loadSkin();

		contextMenu = new Tooltip( content, skin, stage );
		contextMenu.setWidth( stage.getWidth() - 40 );
		contextMenu.setHeight( stage.getHeight() - 40 );

		contextMenu.show( stage.getWidth() / 2 - contextMenu.getWidth() / 2 - 10, stage.getHeight() / 2 - contextMenu.getHeight() / 2 - 30, true );

		ParallelAction parallelAction = new ParallelAction(
				new SequenceAction( Actions.alpha( 0 ), Actions.fadeIn( 0.25f ) ),
				new SequenceAction( Actions.scaleTo( 0, 0 ), Actions.scaleTo( 1, 1, 0.25f ) ) );

		contextMenu.addAction( new SequenceAction( parallelAction, Actions.removeAction( parallelAction ) ) );

		this.keyboardHelper = keyboardHelper;
	}

	// ----------------------------------------------------------------------
	public void queueEndGame(String titleString, String messageString)
	{
		Skin skin = Global.loadSkin();

		Table message = new Table();
		message.defaults().pad( 10 );

		Label title = new Label(titleString, skin, "title");
		message.add( title ).expandX().left();
		message.row();

		message.add( new Seperator( skin ) ).expandX().fillX();
		message.row();

		Table messageBody = new Table();
		Label messageText = new Label( messageString, skin);
		messageText.setWrap( true );
		messageBody.add( messageText ).expand().fillX();
		messageBody.row();

		message.add( messageBody ).expand().fill();
		message.row();

		message.add( new Seperator( skin ) ).expandX().fillX();
		message.row();

		TextButton continueButton = new TextButton( "Continue", skin );
		continueButton.addListener( new ClickListener(  )
		{
			public void clicked( InputEvent event, float x, float y )
			{
				clearContextMenu();
				Global.delete();
				RoguelikeGame.Instance.switchScreen( RoguelikeGame.ScreenEnum.MAINMENU );
			}
		} );
		message.add( continueButton ).colspan( 2 ).expandX().fillX();
		message.row();

		HubScreen.Instance.queueContextMenu( message, new ButtonKeyboardHelper( continueButton ) );
	}

	// ----------------------------------------------------------------------
	public void queueMessage(String titleString, String messageString)
	{
		Skin skin = Global.loadSkin();

		Table message = new Table();
		message.defaults().pad( 10 );

		Label title = new Label(titleString, skin, "title");
		message.add( title ).expandX().left();
		message.row();

		message.add( new Seperator( skin ) ).expandX().fillX();
		message.row();

		Table messageBody = new Table();
		Label messageText = new Label( messageString, skin);
		messageText.setWrap( true );
		messageBody.add( messageText ).expand().fillX();
		messageBody.row();

		message.add( messageBody ).expand().fill();
		message.row();

		message.add( new Seperator( skin ) ).expandX().fillX();
		message.row();

		TextButton continueButton = new TextButton( "Continue", skin );
		continueButton.addListener( new ClickListener(  )
		{
			public void clicked( InputEvent event, float x, float y )
			{
				HubScreen.Instance.clearContextMenu( );
			}
		} );
		message.add( continueButton ).colspan( 2 ).expandX().fillX();
		message.row();

		HubScreen.Instance.queueContextMenu( message, new ButtonKeyboardHelper( continueButton ) );
	}

	// ----------------------------------------------------------------------
	public void showTreasureSellMessage( final int val )
	{
		Skin skin = Global.loadSkin();

		Table message = new Table();
		message.defaults().pad( 10 );

		Label title = new Label("Reward", skin, "title");
		message.add( title ).expandX().left();
		message.row();

		message.add( new Seperator( skin ) ).expandX().fillX();
		message.row();

		Table messageBody = new Table();
		Label messageText = new Label( "Selling the treasures you found nets you some extra funds.", skin);
		messageText.setWrap( true );
		messageBody.add( messageText ).expand().fillX();
		messageBody.row();

		message.add( messageBody ).expand().fill();
		message.row();

		Label rewardMessage = new Label( "Reward: " +val, skin );
		rewardMessage.setColor( Color.GOLD );
		message.add( rewardMessage ).expandX();
		message.row();

		message.add( new Seperator( skin ) ).expandX().fillX();
		message.row();

		TextButton continueButton = new TextButton( "Continue", skin );
		continueButton.addListener( new ClickListener(  )
		{
			public void clicked( InputEvent event, float x, float y )
			{
				Global.Funds += val;

				clearContextMenu();
			}
		} );
		message.add( continueButton ).expandX();
		message.row();

		queueContextMenu(message, new ButtonKeyboardHelper( continueButton ));
	}

	// ----------------------------------------------------------------------
	public void showRewardMessage( String messageString, final int reward )
	{
		Skin skin = Global.loadSkin();

		Table message = new Table();
		message.defaults().pad( 10 );

		Label title = new Label("Reward", skin, "title");
		message.add( title ).expandX().left();
		message.row();

		message.add( new Seperator( skin ) ).expandX().fillX();
		message.row();

		Table messageBody = new Table();
		Label messageText = new Label( messageString, skin);
		messageText.setWrap( true );
		messageBody.add( messageText ).expand().fillX();
		messageBody.row();

		message.add( messageBody ).expand().fill();
		message.row();

		Label rewardMessage = new Label( "Reward: " +reward, skin );
		rewardMessage.setColor( Color.GOLD );
		message.add( rewardMessage ).expandX();
		message.row();

		message.add( new Seperator( skin ) ).expandX().fillX();
		message.row();

		TextButton continueButton = new TextButton( "Continue", skin );
		continueButton.addListener( new ClickListener(  )
		{
			public void clicked( InputEvent event, float x, float y )
			{
				Global.Funds += reward;

				int treasure = 0;
				for (Item item : Global.CurrentLevel.player.inventory.m_items)
				{
					if ( item.category == Item.ItemCategory.TREASURE )
					{
						treasure += item.value;
					}
				}

				Global.QuestManager.currentQuest = null;
				Global.QuestManager.currentLevel = null;
				Global.QuestManager.count++;
				Global.QuestManager.seed++;

				if (Global.QuestManager.count == 2)
				{
					Global.QuestManager.difficulty++;
					Global.QuestManager.count = 0;
				}

				Global.fillMarket();
				Global.fillMissions();

				if (treasure > 0)
				{
					showTreasureSellMessage( treasure );
				}

				Global.save();

				HubScreen.Instance.queueMessage( "Market",
												 "New items are available for purchase in the market.");

				clearContextMenu();
			}
		} );
		message.add( continueButton ).expandX();
		message.row();

		queueContextMenu(message, new ButtonKeyboardHelper( continueButton ));
	}

	// ----------------------------------------------------------------------
	public void queueContextMenu(Table table, ButtonKeyboardHelper keyboardHelper)
	{
		if (contextMenu == null)
		{
			displayContextMenu( table, keyboardHelper );
		}
		else
		{
			contextMenuQueue.add( new GameScreen.ContextMenuData( table, keyboardHelper ) );
		}
	}

	public void createUI()
	{
		table.clear();
		missionList.clear();
		missionContent.clear();
		marketList.clear();
		marketContent.clear();
		stashList.clear();
		stashContent.clear();
		locationList.clear();
		locationContent.clear();

		missionHelper.clearGrid();
		marketHelper.clearGrid();
		stashHelper.clearGrid();
		locationHelper.clearGrid();

		missionList.defaults().padRight( 5 );
		marketList.defaults().padRight( 5 );
		stashList.defaults().padRight( 5 );
		locationList.defaults().padRight( 5 );

		keyboardHelper = new ButtonKeyboardHelper(  );

		tabPanel = new TabPanel( Global.loadSkin() );
		funds = new Label( "Funds: " + Global.Funds, Global.loadSkin(), "title" );

		table.add( funds ).expandX().left();
		table.row();

		Skin skin = Global.loadSkin();

		ScrollPane missionScrollPane = new ScrollPane( missionList, skin );
		missionScrollPane.setScrollingDisabled( true, false );
		missionScrollPane.setVariableSizeKnobs( true );
		missionScrollPane.setFadeScrollBars( false );
		missionScrollPane.setScrollbarsOnTop( false );
		missionScrollPane.setForceScroll( false, true );

		ScrollPane marketScrollPane = new ScrollPane( marketList, skin );
		marketScrollPane.setScrollingDisabled( true, false );
		marketScrollPane.setVariableSizeKnobs( true );
		marketScrollPane.setFadeScrollBars( false );
		marketScrollPane.setScrollbarsOnTop( false );
		marketScrollPane.setForceScroll( false, true );

		ScrollPane stashScrollPane = new ScrollPane( stashList, skin );
		stashScrollPane.setScrollingDisabled( true, false );
		stashScrollPane.setVariableSizeKnobs( true );
		stashScrollPane.setFadeScrollBars( false );
		stashScrollPane.setScrollbarsOnTop( false );
		stashScrollPane.setForceScroll( false, true );

		final ScrollPane locationScrollPane = new ScrollPane( locationList, skin );
		locationScrollPane.setScrollingDisabled( true, false );
		locationScrollPane.setVariableSizeKnobs( true );
		locationScrollPane.setFadeScrollBars( false );
		locationScrollPane.setScrollbarsOnTop( false );
		locationScrollPane.setForceScroll( false, true );

		Table missionTab = new Table(  );
		missionTab.add( missionScrollPane ).expandY().fillY().width( Value.percentWidth( 0.4f, missionTab ) );
		missionTab.add( missionContent ).expandY().fillY().width( Value.percentWidth( 0.6f, missionTab ) );
		tabPanel.addTab( "Missions", missionTab );

		Table marketTab = new Table(  );
		marketTab.add( marketScrollPane ).expandY().fillY().width( Value.percentWidth( 0.4f, marketTab ) );
		marketTab.add( marketContent ).expandY().fillY().width( Value.percentWidth( 0.6f, marketTab ) );
		tabPanel.addTab( "Market", marketTab );

		Table stashTab = new Table(  );
		stashTab.add( stashScrollPane ).expandY().fill().width( Value.percentWidth( 0.4f, stashTab ) );
		stashTab.add( stashContent ).expandY().fillY().width( Value.percentWidth( 0.6f, stashTab ) );
		tabPanel.addTab( "Stash", stashTab );

		Table locationTab = new Table(  );
		locationTab.add( locationScrollPane ).expandY().fill().width( Value.percentWidth( 0.4f, locationTab ) );
		locationTab.add( locationContent ).expandY().fillY().width( Value.percentWidth( 0.6f, locationTab ) );
		tabPanel.addTab( "Retirement", locationTab );

		table.add( tabPanel ).colspan( 2 ).expand().fill().pad( 25 );
		table.row();

		createMissions( Global.Missions, missionHelper );
		createMarket( Global.Market, marketHelper );
		createStash( stashHelper );
		createLocation( locationHelper );

		missionHelper.scrollPane = missionScrollPane;
		marketHelper.scrollPane = marketScrollPane;
		stashHelper.scrollPane = stashScrollPane;
		locationHelper.scrollPane = locationScrollPane;

		tabPanel.addListener( new ChangeListener() {
			@Override
			public void changed( ChangeEvent event, Actor actor )
			{
				ButtonKeyboardHelper oldHelper = keyboardHelper;

				if (tabPanel.getSelectedIndex() == 0)
				{
					keyboardHelper = missionHelper;
				}
				else if (tabPanel.getSelectedIndex() == 1)
				{
					keyboardHelper = marketHelper;
				}
				else if (tabPanel.getSelectedIndex() == 2)
				{
					keyboardHelper = stashHelper;
				}
				else if (tabPanel.getSelectedIndex() == 3)
				{
					keyboardHelper = locationHelper;
				}

				keyboardHelper.trySetCurrent( oldHelper.currentx, oldHelper.currenty, oldHelper.currentz );
			}
		} );

		keyboardHelper = missionHelper;
	}

	public void createMissions( Array<Quest> items, final ButtonKeyboardHelper helper )
	{
		for (Actor a : tabPanel.tabTitleTable.getChildren())
		{
			helper.add( a, 0, 0 );
		}

		final Skin skin = Global.loadSkin();

		for (final Quest item : items)
		{
			Button button = new Button( skin );
			button.defaults().pad( 5 );

			SpriteWidget sprite = new SpriteWidget( item.icon, 24, 24 );
			button.add( sprite );

			Table right = new Table(  );
			button.add( right ).expand().fill().left();

			Label name = new Label( item.name, skin );
			name.setEllipsis( true );
			right.add( name ).width( Value.percentWidth( 1, right ) );
			right.row();
			right.add( new Label( ""+item.reward, skin ) ).left();
			right.row();

			button.addListener( new ClickListener(  )
			{
				public void clicked (InputEvent event, float x, float y)
				{
					missionContent.clear();

					missionContent.add( item.createTable( skin ) ).expand().fill();
					missionContent.row();

					TextButton embark = new TextButton( "Embark", skin );
					embark.addListener( new ClickListener(  )
					{
						public void clicked (InputEvent event, float x, float y)
						{
							Global.QuestManager.currentQuest = item;
							RoguelikeGame.Instance.switchScreen( RoguelikeGame.ScreenEnum.LOADOUT );
						}
					} );

					missionContent.add( embark ).expandX().right();
					missionContent.row();

					helper.replace( embark, 1, 1 );
				}
			} );

			missionList.add( button ).expandX().fillX();
			missionList.row();

			helper.add( button );
		}
	}

	public void createMarket( final Array<Item> items, final ButtonKeyboardHelper helper )
	{
		ScrollPane scrollPane = helper.scrollPane;
		helper.clearGrid();

		for (Actor a : tabPanel.tabTitleTable.getChildren())
		{
			helper.add( a, 0, 0 );
		}

		final Skin skin = Global.loadSkin();

		marketList.clear();
		marketContent.clear();

		int i = 0;
		for ( Item.ItemCategory category : Item.ItemCategory.values() )
		{
			Array<Item> categoryItems = new Array<Item>(  );
			for (Item item : items)
			{
				if (item.category == category)
				{
					categoryItems.add( item );
				}
			}

			if (categoryItems.size > 0)
			{
				marketList.add( new Label( Global.capitalizeString( category.toString() ), skin ) ).expandX().left().pad( 10 );
				marketList.row();
			}

			for (final Item item : categoryItems)
			{
				Button button = new Button( skin );
				button.defaults().pad( 5 );

				SpriteWidget sprite = new SpriteWidget( item.getIcon(), 24, 24 );
				button.add( sprite );

				Table right = new Table();
				button.add( right ).expand().fill().left();

				Label name = new Label( item.getName(), skin );
				name.setEllipsis( true );
				right.add( name ).width( Value.percentWidth( 1, right ) );
				right.row();

				Label valueLabel = new Label( "" + item.value, skin );

				if ( Global.Funds < item.value )
				{
					valueLabel.setColor( Color.RED );
				}

				right.add( valueLabel ).left();
				right.row();

				final int currenti = i++;

				button.addListener( new ClickListener()
				{
					public void clicked( InputEvent event, float x, float y )
					{
						marketContent.clear();

						ScrollPane scrollPane1 = new ScrollPane( item.createTable( skin ), skin );
						marketContent.add( scrollPane1 ).expand().fill();
						marketContent.row();

						if ( Global.Funds >= item.value )
						{
							TextButton buy = new TextButton( "Purchase for " + item.value, skin );
							buy.addListener( new ClickListener()
							{
								public void clicked( InputEvent event, float x, float y )
								{

									items.removeValue( item, true );
									Global.UnlockedItems.add( item );

									lastFunds = Global.Funds;
									Global.Funds -= item.value;

									item.value /= 2;

									Global.save();

									createMarket( items, helper );
									createStash( stashHelper );
								}
							} );

							marketContent.add( buy ).expandX().right();
							marketContent.row();

							helper.clearColumn( 1 );
							helper.replace( buy, 1, currenti );
						}
					}
				} );

				marketList.add( button ).expandX().fillX();
				marketList.row();

				helper.add( button );
			}
		}

		helper.scrollPane = scrollPane;
		helper.trySetCurrent(  );
	}

	public void createStash( final ButtonKeyboardHelper helper )
	{
		ScrollPane scrollPane = helper.scrollPane;
		helper.clearGrid();

		for (Actor a : tabPanel.tabTitleTable.getChildren())
		{
			helper.add( a, 0, 0 );
		}

		final Skin skin = Global.loadSkin();

		stashList.clear();
		stashContent.clear();

		int i = 0;
		for ( Item.ItemCategory category : Item.ItemCategory.values() )
		{
			Array<Item> categoryItems = new Array<Item>(  );
			for (Item item : Global.UnlockedItems)
			{
				if (item.category == category)
				{
					categoryItems.add( item );
				}
			}

			if (categoryItems.size > 0)
			{
				stashList.add( new Label( Global.capitalizeString( category.toString() ), skin ) ).expandX().left().pad( 10 );
				stashList.row();
			}

			for (final Item item : categoryItems)
			{
				Button button = new Button( skin );
				button.defaults().pad( 5 );

				SpriteWidget sprite = new SpriteWidget( item.getIcon(), 24, 24 );
				button.add( sprite );

				Table right = new Table();
				button.add( right ).expand().fill().left();

				Label name = new Label( item.getName(), skin );
				name.setEllipsis( true );
				right.add( name ).width( Value.percentWidth( 1, right ) );
				right.row();
				right.add( new Label( "" + item.value, skin ) ).left();
				right.row();

				final int currenti = i++;

				button.addListener( new ClickListener()
				{
					public void clicked( InputEvent event, float x, float y )
					{
						stashContent.clear();

						ScrollPane scrollPane1 = new ScrollPane( item.createTable( skin ), skin );
						stashContent.add( scrollPane1 ).expand().fill();
						stashContent.row();

						TextButton sell = new TextButton( "Sell for " + item.value, skin );
						sell.addListener( new ClickListener()
						{
							public void clicked( InputEvent event, float x, float y )
							{
								Global.UnlockedItems.removeValue( item, true );

								lastFunds = Global.Funds;
								Global.Funds += item.value;

								for ( Item.EquipmentSlot slot : Item.EquipmentSlot.values() )
								{
									if (Global.Loadout.get( slot ) != null && Global.Loadout.get( slot ) == item.hashCode)
									{
										Global.Loadout.put( slot, null );
									}
								}

								Global.save();

								createMarket( Global.Market, marketHelper );
								createStash( helper );
							}
						} );

						stashContent.add( sell ).expandX().right();
						stashContent.row();

						helper.clearColumn( 1 );
						helper.replace( sell, 1, currenti );
					}
				} );

				stashList.add( button ).expandX().fillX().width( Value.percentWidth( 1, stashList ) );
				stashList.row();

				helper.add( button );
			}
		}

		helper.scrollPane = scrollPane;
		helper.trySetCurrent(  );
	}

	public void createLocation( final ButtonKeyboardHelper helper )
	{
		ScrollPane scrollPane = helper.scrollPane;
		helper.clearGrid();

		for (Actor a : tabPanel.tabTitleTable.getChildren())
		{
			helper.add( a, 0, 0 );
		}

		final Skin skin = Global.loadSkin();

		locationList.clear();
		locationContent.clear();

		int i = 0;
		for (final LocationData location : locationDataList.locations)
		{
			Button button = new Button( skin );
			button.defaults().pad( 5 );

			Table right = new Table();
			button.add( right ).expand().fill().left();

			Label name = new Label( location.name, skin );
			name.setEllipsis( true );
			right.add( name ).width( Value.percentWidth( 1, right ) );
			right.row();

			Label valueLabel = new Label( "" + location.cost, skin );

			if ( Global.Funds < location.cost )
			{
				valueLabel.setColor( Color.RED );
			}

			right.add( valueLabel ).left();
			right.row();

			final int currenti = i++;
			button.addListener( new ClickListener()
			{
				public void clicked( InputEvent event, float x, float y )
				{
					locationContent.clear();

					locationContent.add( location.createTable( ) ).expand().fill();
					locationContent.row();

					helper.clearColumn( 1 );

					if (Global.Funds >= location.cost)
					{
						TextButton retire = new TextButton( "Retire here for " + location.cost, skin );
						retire.addListener( new ClickListener()
						{
							public void clicked( InputEvent event, float x, float y )
							{

								queueEndGame( "Retirement", location.message );
							}
						} );

						locationContent.add( retire ).expandX().right();
						locationContent.row();

						helper.replace( retire, 1, currenti );
					}
				}
			} );

			locationList.add( button ).expandX().fillX().width( Value.percentWidth( 1, locationList ) );
			locationList.row();

			helper.add( button );
		}

		helper.scrollPane = scrollPane;
		helper.trySetCurrent(  );
	}

	// ----------------------------------------------------------------------
	public OrthographicCamera camera;

	boolean created;

	Tooltip contextMenu;

	// ----------------------------------------------------------------------
	public Array<GameScreen.ContextMenuData> contextMenuQueue = new Array<GameScreen.ContextMenuData>(  );

	TabPanel tabPanel;
	Table table;

	Table missionList = new Table(  );
	Table missionContent = new Table(  );

	Table marketList = new Table(  );
	Table marketContent = new Table(  );

	Table stashList = new Table(  );
	Table stashContent = new Table(  );

	Table locationList = new Table(  );
	Table locationContent = new Table(  );

	Label funds;
	int lastFunds;

	final ButtonKeyboardHelper missionHelper = new ButtonKeyboardHelper(  );
	final ButtonKeyboardHelper marketHelper = new ButtonKeyboardHelper(  );
	final ButtonKeyboardHelper stashHelper = new ButtonKeyboardHelper(  );
	final ButtonKeyboardHelper locationHelper = new ButtonKeyboardHelper(  );

	Stage stage;

	SpriteBatch batch;

	public InputMultiplexer inputMultiplexer;

	public ButtonKeyboardHelper keyboardHelper;

	Texture background;

	LocationList locationDataList = new LocationList();

	@Override
	public void show()
	{
		if ( !created )
		{
			create();
			createUI();
			created = true;
		}

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
		if ( !created )
		{
			create();
			createUI();
			created = true;
		}

		if (funds != null)
		{
			if ( lastFunds != Global.Funds )
			{
				int fundsChange = Global.Funds - lastFunds;
				if ( fundsChange < 0 )
				{
					int change = fundsChange / 10 - 1;
					fundsChange -= change;
					lastFunds += change;

					funds.setText( "Funds: " + lastFunds + " [RED]" + fundsChange + "[]" );
				}
				else
				{
					int change = fundsChange / 10 + 1;
					fundsChange -= change;
					lastFunds += change;

					funds.setText( "Funds: " + lastFunds + " [GREEN]+" + fundsChange + "[]" );
				}
			}
			else
			{
				funds.setText( "Funds: " + Global.Funds );
			}
		}

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

	private class LocationList
	{
		public Array<LocationData> locations = new Array<LocationData>(  );

		public LocationList()
		{
			XmlReader reader = new XmlReader();
			XmlReader.Element xml = null;

			try
			{
				xml = reader.parse( Gdx.files.internal( "Retirement.xml" ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			for (int i = 0; i < xml.getChildCount(); i++)
			{
				XmlReader.Element el = xml.getChild( i );

				locations.add( new LocationData( el ) );
			}
		}
	}

	private class LocationData
	{
		public String name;
		public String description;
		public Texture image;
		public int cost;
		public String message;

		public LocationData( XmlReader.Element xml )
		{
			name = xml.get( "Name" );
			description = xml.get( "Description" );
			cost = xml.getInt( "Cost" );

			String texName = xml.get( "Image" );

			texName = "Sprites/" + texName + ".png";
			image = AssetManager.loadTexture( texName );

			message = xml.get( "Message" );
		}

		public Table createTable()
		{
			Skin skin = Global.loadSkin();
			Table table = new Table();

			table.defaults().pad( 10 );

			table.add( new Label( name, skin, "title" ) ).expandX().left();
			table.row();

			table.add( new Seperator( skin ) ).expandX().fillX();
			table.row();

			Label desc = new Label( description, skin );
			desc.setWrap( true );
			table.add( desc ).expandX().fillX().left();
			table.row();

			Image image = new Image( this.image );
			table.add( image ).expandX().fillX();
			table.row();

			Label rew = new Label( "Cost: " + cost, skin );
			rew.setColor( Color.GOLD );

			table.add( rew ).expandX().left();
			table.row();

			return table;
		}

	}
}
