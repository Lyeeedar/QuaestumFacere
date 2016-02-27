package Roguelike.UI;

import Roguelike.Tiles.Point;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;

/**
 * Created by Philip on 25-Feb-16.
 */
public class ButtonKeyboardHelper
{
	public Array<Column> grid = new Array<Column>(  );
	public Actor cancel;
	public ScrollPane scrollPane;

	public int currentx;
	public int currenty;
	public int currentz;

	private Actor active;

	private float updateAccumulator;

	private boolean first = true;

	// ----------------------------------------------------------------------
	public ButtonKeyboardHelper()
	{

	}

	// ----------------------------------------------------------------------
	public ButtonKeyboardHelper( Actor cancel )
	{
		this.cancel = cancel;
		add( cancel );
	}

	// ----------------------------------------------------------------------
	public void add( Actor actor )
	{
		add(actor, 0);
	}

	// ----------------------------------------------------------------------
	public void add( Actor... actors )
	{
		int x = 0;
		int y = grid.size > 0 ? grid.get( 0 ).cells.size : 0;

		for (Actor a : actors)
		{
			add( a, x, y );
		}
	}

	// ----------------------------------------------------------------------
	public void add( Actor actor, int x )
	{
		add(actor, x, grid.size > 0 ? grid.get( 0 ).cells.size : 0);
	}

	// ----------------------------------------------------------------------
	public void add( Actor actor, int x, int y )
	{
		Column column = null;
		for (int i = 0; i < grid.size; i++)
		{
			if (grid.get( i ).x == x)
			{
				column = grid.get( i );
				break;
			}
			else if (grid.get( i ).x > x)
			{
				column = new Column();
				column.x = x;

				grid.insert( i, column );
				break;
			}
		}

		if (column == null)
		{
			column = new Column();
			column.x = x;
			grid.add( column );
		}

		Cell cell = null;
		for (int i = 0; i < column.cells.size; i++)
		{
			if (column.cells.get( i ).y == y)
			{
				cell = column.cells.get( i );
				break;
			}
			else if (column.cells.get( i ).y > y)
			{
				cell = new Cell();
				cell.y = y;

				column.cells.insert( i, cell );
				break;
			}
		}

		if (cell == null)
		{
			cell = new Cell();
			cell.y = y;
			column.cells.add( cell );
		}

		cell.actors.add( actor );

		if (first)
		{
			trySetCurrent( x, y, 0 );

			first = false;
		}
	}

	// ----------------------------------------------------------------------
	public Actor getCurrent()
	{
		return get( currentx, currenty, currentz );
	}

	// ----------------------------------------------------------------------
	public Actor get(int x, int y, int z)
	{
		return getColumn( x ).getCell( y ).getActor( z );
	}

	// ----------------------------------------------------------------------
	public Column getColumn(int x)
	{
		for (Column col : grid)
		{
			if (col.x == x)
			{
				return col;
			}
			else if (col.x > x)
			{
				return col;
			}
		}

		return grid.get( grid.size - 1 );
	}

	// ----------------------------------------------------------------------
	public void clear()
	{
		exit( getCurrent() );
	}

	// ----------------------------------------------------------------------
	public void trySetCurrent(int x, int y, int z)
	{
		exit( getCurrent() );

		currentx = x;
		currenty = y;
		currentz = z;

		currentx = getColumn( currentx ).x;
		currenty = getColumn( currentx ).getCell( currenty ).y;

		Cell cell = getColumn( currentx ).getCell( currenty );
		currentz = cell.actors.indexOf( cell.getActor( currentz ), true );

		Actor current = getCurrent();
		enter( current );

		if (scrollPane != null)
		{
			scrollPane.scrollTo( current.getX(), current.getY(), current.getWidth(), current.getHeight() );
		}
	}

	// ----------------------------------------------------------------------
	public void update( float delta )
	{
		updateAccumulator -= delta;

		if (updateAccumulator < 0)
		{
			updateAccumulator = 0.01f;

			if (active != null && active instanceof Slider)
			{
				Slider slider = (Slider)active;
				if ( Gdx.input.isKeyPressed( Input.Keys.LEFT ) )
				{
					slider.setValue( slider.getValue() - slider.getStepSize() );
				}
				else if ( Gdx.input.isKeyPressed( Input.Keys.RIGHT ) )
				{
					slider.setValue( slider.getValue() + slider.getStepSize() );
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	public boolean keyDown( int keycode )
	{
		if (active != null)
		{
			if (active instanceof Slider)
			{
				Slider slider = (Slider)active;

				if ( keycode == Input.Keys.ESCAPE || keycode == Input.Keys.ENTER )
				{
					float val = slider.getValue();
					touchUp( slider );
					slider.setValue( val );
					active = null;
				}
				else if ( keycode == Input.Keys.LEFT )
				{
					slider.setValue( slider.getValue() - slider.getStepSize() );
				}
				else if ( keycode == Input.Keys.RIGHT )
				{
					slider.setValue( slider.getValue() + slider.getStepSize() );
				}
			}
			else if (active instanceof SelectBox)
			{
				SelectBox selectBox = (SelectBox)active;

				if ( keycode == Input.Keys.ESCAPE || keycode == Input.Keys.ENTER )
				{
					selectBox.hideList();
					active = null;
				}
				else if ( keycode == Input.Keys.UP )
				{
					int newIndex = selectBox.getSelectedIndex() - 1;
					if (newIndex < 0) { newIndex = 0; }
					selectBox.setSelectedIndex( newIndex );
					selectBox.hideList();
					selectBox.showList();
				}
				else if ( keycode == Input.Keys.DOWN )
				{
					int newIndex = selectBox.getSelectedIndex() + 1;
					if (newIndex >= selectBox.getItems().size) { newIndex = selectBox.getItems().size-1; }
					selectBox.setSelectedIndex( newIndex );
					selectBox.hideList();
					selectBox.showList();
				}
			}
		}
		else
		{
			if ( keycode == Input.Keys.ESCAPE )
			{
				if (cancel != null)
				{
					pressButton( cancel );
				}
			}
			else if ( keycode == Input.Keys.ENTER )
			{
				Actor actor = getCurrent();
				if ( actor instanceof Button )
				{
					pressButton( actor );
				}
				else if ( actor instanceof Slider )
				{
					active = actor;
					Slider slider = (Slider)actor;

					float val = slider.getValue();
					touchDown( active );
					slider.setValue( val );
				}
				else if ( actor instanceof SelectBox )
				{
					active = actor;
					SelectBox selectBox = (SelectBox)active;
					selectBox.showList();
				}
			}
			else if ( keycode == Input.Keys.LEFT )
			{
				// check if move within cell
				if ( currentz > 0 )
				{
					trySetCurrent( currentx, currenty, currentz - 1 );
				}
				else
				{
					Column current = getColumn( currentx );
					if ( current != grid.first() )
					{
						int index = grid.indexOf( current, true );
						Column prev = grid.get( index - 1 );
						trySetCurrent( prev.x, currenty, 100 );
					}
				}
			}
			else if ( keycode == Input.Keys.RIGHT )
			{
				// check if move within cell
				Cell cell = getColumn( currentx ).getCell( currenty );
				if ( currentz < cell.actors.size - 1 )
				{
					trySetCurrent( currentx, currenty, currentz + 1 );
				}
				else
				{
					Column current = getColumn( currentx );
					if ( current != grid.first() )
					{
						int index = grid.indexOf( current, true );
						Column next = grid.get( index + 1 );
						trySetCurrent( next.x, currenty, 0 );
					}
				}
			}
			else if ( keycode == Input.Keys.UP )
			{
				trySetCurrent( currentx, currenty - 1, currentz );
			}
			else if ( keycode == Input.Keys.DOWN )
			{
				trySetCurrent( currentx, currenty + 1, currentz );
			}
		}

		return true;
	}

	// ----------------------------------------------------------------------
	private boolean pressButton( Actor actor )
	{
		for ( EventListener listener : actor.getListeners() )
		{
			if (listener instanceof ClickListener)
			{
				((ClickListener)listener).clicked( null, 0, 0 );
			}
		}
		return true;
	}

	// ----------------------------------------------------------------------
	private void enter( Actor actor )
	{
		InputEvent event = new InputEvent();
		event.setType(InputEvent.Type.enter);
		event.setPointer( -1 );
		actor.fire(event);
	}

	// ----------------------------------------------------------------------
	private void exit( Actor actor )
	{
		InputEvent event = new InputEvent();
		event.setType(InputEvent.Type.exit);
		event.setPointer( -1 );
		actor.fire(event);
	}

	// ----------------------------------------------------------------------
	private void touchDown( Actor actor )
	{
		InputEvent event = new InputEvent();
		event.setType(InputEvent.Type.touchDown);
		actor.fire(event);
	}

	// ----------------------------------------------------------------------
	private void touchUp( Actor actor )
	{
		InputEvent event = new InputEvent();
		event.setType(InputEvent.Type.touchUp);
		actor.fire(event);
	}

	// ----------------------------------------------------------------------
	private class Column
	{
		public int x;
		public Array<Cell> cells = new Array<Cell>(  );

		// ----------------------------------------------------------------------
		public Cell getCell( int y )
		{
			for (Cell cell : cells)
			{
				if (cell.y == y)
				{
					return cell;
				}
				else if ( cell.y > y )
				{
					return cell;
				}
			}

			return cells.get( cells.size - 1 );
		}
	}

	// ----------------------------------------------------------------------
	private class Cell
	{
		public int y;
		public Array<Actor> actors = new Array<Actor>(  );

		// ----------------------------------------------------------------------
		public Actor getActor( int z )
		{
			if ( z >= actors.size )
			{
				return actors.get( actors.size - 1 );
			}

			return actors.get( z );
		}
	}
}
