package Roguelike.UI;

import Roguelike.AssetManager;
import Roguelike.Entity.Tasks.TaskAttack;
import Roguelike.Global;
import Roguelike.Items.Item;
import Roguelike.RoguelikeGame;
import Roguelike.Screens.GameScreen;
import Roguelike.Sprite.Sprite;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

public class EquipmentPanel extends TilePanel
{
	private final GlyphLayout layout = new GlyphLayout();
	private final BitmapFont font;
	private final TextureRegion white;

	public EquipmentPanel( Skin skin, Stage stage )
	{
		super( skin, stage, AssetManager.loadSprite( "GUI/TileBackground" ), AssetManager.loadSprite( "GUI/TileBorder" ), 1, 1, 48, false );

		drawHorizontalBackground = false;
		font = skin.getFont( "default" );
		padding = 10;

		this.white = AssetManager.loadTextureRegion( "Sprites/white.png" );
	}

	@Override
	public void populateTileData()
	{
		tileData.clear();

		int i = 0;
		for ( Item.EquipmentSlot slot : Item.EquipmentSlot.values() )
		{
			if (i < viewHeight)
			{
				Item item = Global.CurrentLevel.player.getInventory().getEquip( slot );

				if ( item == null )
				{
					tileData.add( slot );
				}
				else
				{
					tileData.add( item );
				}
			}

			i++;
		}
	}

	@Override
	public Sprite getSpriteForData( Object data )
	{
		if ( !( data instanceof Item ) ) { return null; }

		return ( (Item) data ).getIcon();
	}

	@Override
	public void handleDataClicked( final Object data, InputEvent event, float x, float y )
	{
		if ( data instanceof Item )
		{
			Item item = (Item)data;

			if ( item.getMainSlot() == Item.EquipmentSlot.WEAPON )
			{
				GameScreen.Instance.displayWeaponHitPattern();
			}
		}
	}

	@Override
	public Table getToolTipForData( Object data )
	{
		if ( data instanceof Item.EquipmentSlot )
		{
			Table table = new Table();

			table.add( new Label( Global.capitalizeString( data.toString() ), skin ) );

			return table;
		}

		return ( (Item) data ).createTable( skin, Global.CurrentLevel.player );
	}

	@Override
	public Color getColourForData( Object data )
	{
				if ( !( data instanceof Item ) ) { return Color.DARK_GRAY; }

		return null;
	}

	@Override
	public void onDrawItemBackground( Object data, Batch batch, int x, int y, int width, int height )
	{

	}

	@Override
	public void onDrawItem( Object data, Batch batch, int x, int y, int width, int height )
	{

	}

	@Override
	public void onDrawItemForeground( Object data, Batch batch, int x, int y, int width, int height )
	{

	}

}
