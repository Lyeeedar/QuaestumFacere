package Roguelike.desktop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import Roguelike.AbstractApplicationChanger;
import Roguelike.Global;
import Roguelike.RoguelikeGame;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglPreferences;

public class LwjglApplicationChanger extends AbstractApplicationChanger
{
	public Preferences prefs;

	public LwjglApplicationChanger()
	{
		super( new LwjglPreferences( "game-settings", "settings" ) );
	}

	@Override
	public Application createApplication( RoguelikeGame game, Preferences pref )
	{
		System.setProperty( "org.lwjgl.opengl.Window.undecorated", "" + pref.getBoolean( "borderless" ) );

		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

		cfg.title = pref.getString( "window-name" );
		cfg.width = pref.getInteger( "resolutionX" );
		cfg.height = pref.getInteger( "resolutionY" );
		cfg.fullscreen = pref.getBoolean( "fullscreen" );
		cfg.vSyncEnabled = pref.getBoolean( "vSync" );
		cfg.foregroundFPS = 0;
		cfg.backgroundFPS = 2;
		cfg.samples = pref.getInteger( "msaa" );

		Global.Resolution[0] = cfg.width;
		Global.Resolution[1] = cfg.height;
		Global.ScreenSize[0] = cfg.width;
		Global.ScreenSize[1] = cfg.height;

		Global.FPS = pref.getInteger( "fps" );
		Global.AnimationSpeed = 1.0f / pref.getFloat( "animspeed" );

		return new LwjglApplication( game, cfg );
	}

	@Override
	public void updateApplication( Preferences pref )
	{
		System.setProperty( "org.lwjgl.opengl.Window.undecorated", "" + pref.getBoolean( "borderless" ) );

		int width = pref.getInteger( "resolutionX" );
		int height = pref.getInteger( "resolutionY" );
		boolean fullscreen = pref.getBoolean( "fullscreen" );

		Global.Resolution[0] = width;
		Global.Resolution[1] = height;
		Global.FPS = pref.getInteger( "fps" );
		Global.AnimationSpeed = 1.0f / pref.getFloat( "animspeed" );

		Gdx.graphics.setDisplayMode( width, height, fullscreen );
		Gdx.graphics.setVSync( pref.getBoolean( "vSync" ) );
	}

	@Override
	public String[] getSupportedDisplayModes()
	{
		DisplayMode[] displayModes = Gdx.graphics.getDisplayModes();

		ArrayList<String> modes = new ArrayList<String>();

		for ( int i = 0; i < displayModes.length; i++ )
		{
			String mode = displayModes[i].width + "x" + displayModes[i].height;

			boolean contained = false;
			for ( String m : modes )
			{
				if ( m.equals( mode ) )
				{
					contained = true;
					break;
				}
			}
			if ( !contained )
			{
				modes.add( mode );
			}
		}

		Collections.sort( modes, new Comparator<String>()
		{

			@Override
			public int compare( String s1, String s2 )
			{
				int split = s1.indexOf( "x" );
				int rX1 = Integer.parseInt( s1.substring( 0, split ) );

				split = s2.indexOf( "x" );
				int rX2 = Integer.parseInt( s2.substring( 0, split ) );

				if ( rX1 < rX2 ) return -1;
				else if ( rX1 > rX2 ) return 1;
				return 0;
			}

		} );

		String[] m = new String[modes.size()];

		return modes.toArray( m );
	}

	@Override
	public void setToNativeResolution( Preferences prefs )
	{
		DisplayMode dm = Gdx.graphics.getDesktopDisplayMode();

		prefs.putInteger( "resolutionX", dm.width );
		prefs.putInteger( "resolutionY", dm.height );

		updateApplication( prefs );
	}

}