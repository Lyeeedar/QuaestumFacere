package Roguelike.Util;

public final class FastEnumMap<T extends Enum<T>, V>
{
	public int size;
	private Class<T> keyType;
	private V[] items;

	@SuppressWarnings( "unchecked" )
	public FastEnumMap( Class<T> keyType )
	{
		this.keyType = keyType;
		items = (V[]) new Object[keyType.getEnumConstants().length];
	}

	@SuppressWarnings( "unchecked" )
	public FastEnumMap( FastEnumMap<?, ?> other )
	{
		this.keyType = (Class<T>) other.keyType;
		items = (V[]) new Object[keyType.getEnumConstants().length];
	}

	public int numItems()
	{
		return items.length;
	}

	public void put( T key, V value )
	{
		items[ key.ordinal() ] = value;

		calculateSize();
	}

	public void calculateSize()
	{
		int count = 0;

		for ( int i = 0; i < items.length; i++ )
		{
			if ( items[i] != null )
			{
				count++;
			}
		}

		size = count;
	}

	public void remove( T key )
	{
		items[key.ordinal()] = null;

		calculateSize();
	}

	public V get( T key )
	{
		return items[key.ordinal()];
	}

	public boolean containsValue( V value )
	{
		for (int i = 0; i < items.length; i++)
		{
			if (items[i] == value)
			{
				return true;
			}
		}

		return false;
	}

	public boolean containsKey( T key )
	{
		return items[key.ordinal()] != null;
	}

	public void remove( int index )
	{
		items[ index ] = null;

		calculateSize();
	}

	public V get( int index )
	{
		return items[ index ];
	}

	public boolean containsKey( int index )
	{
		return items[ index ] != null;
	}

	public FastEnumMap<T, V> copy()
	{
		FastEnumMap<T, V> cpy = new FastEnumMap<T, V>( this );

		for ( int i = 0; i < items.length; i++ )
		{
			cpy.put( i, items[ i ] );
		}

		return cpy;
	}

	public void put( int index, V value )
	{
		items[ index ] = value;

		calculateSize();
	}
}
