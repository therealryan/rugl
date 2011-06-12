package com.ryanm.droid.rugl;

import java.util.ArrayList;
import java.util.Date;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.hardware.SensorManager;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

import com.ryanm.droid.rugl.gl.GLUtil;
import com.ryanm.droid.rugl.gl.GLVersion;
import com.ryanm.droid.rugl.input.Touch;
import com.ryanm.droid.rugl.res.ResourceLoader;
import com.ryanm.droid.rugl.util.CodeTimer;
import com.ryanm.droid.rugl.util.CodeTimer.Output;
import com.ryanm.droid.rugl.util.ExceptionHandler;
import com.ryanm.preflect.Persist;
import com.ryanm.preflect.Preflect;
import com.ryanm.preflect.annote.Summary;
import com.ryanm.preflect.annote.Variable;

/**
 * A convenient {@link Phase}-based game model
 * 
 * @author ryanm
 */
@Variable( "DroidRUGL" )
@Summary( "General settings" )
public class Game implements Renderer
{
	/**
	 * Info logging tag
	 */
	public static final String RUGL_TAG = "RUGL";

	/**
	 * Screen width
	 */
	public static int screenWidth;

	/**
	 * Screen height
	 */
	public static int screenHeight;

	/**
	 * Desired width of screen for rendering and input purposes
	 */
	public static float gameWidth = 800;

	/**
	 * Desired height of screen for rendering and input purposes
	 */
	public static float gameHeight = 480;

	/**
	 * OpenGLES version
	 */
	public static GLVersion glVersion;

	private static Object[] confRoots = null;

	private static ArrayList<SurfaceListener> surfaceListeners =
			new ArrayList<Game.SurfaceListener>();

	private boolean resetTouches = true;

	/**
	 * @param sl
	 */
	public static void addSurfaceLIstener( final SurfaceListener sl )
	{
		surfaceListeners.add( sl );
	}

	/**
	 * @param sl
	 */
	public static void removeSurfaceListener( final SurfaceListener sl )
	{
		surfaceListeners.remove( sl );
	}

	/**
	 * Set the roots of the tree that will be configured with
	 * {@link #launchConfiguration()}
	 * 
	 * @param roots
	 * @see #launchConfiguration()
	 */
	public static void setConfigurationRoots( final Object... roots )
	{
		confRoots = roots;
	}

	/**
	 * Launches a configuration activity for the objects previously passed to
	 * {@link #setConfigurationRoots(Object...)}
	 * 
	 * @see #setConfigurationRoots(Object...)
	 */
	public void launchConfiguration()
	{
		Preflect.configure( ga, true, true, confRoots );
	}

	/**
	 * @return The names of saved configurations
	 */
	public String[] listSavedConfigurations()
	{
		return Persist.listSaves( ga );
	}

	/**
	 * Loads a named configuration
	 * 
	 * @param name
	 */
	public void loadConfiguration( final String name )
	{
		Persist.load( ga, name, confRoots );
	}

	/**
	 * Call this when we may have lost track of touchscreen activity e.g.: when
	 * we were in another activity, etc
	 */
	public void resetTouches()
	{
		resetTouches = true;
	}

	/**
	 * Gets the sensor manager
	 * 
	 * @return the {@link SensorManager}
	 */
	public SensorManager getSensorManager()
	{
		return ( SensorManager ) ga.getSystemService( Context.SENSOR_SERVICE );
	}

	private final GameActivity ga;

	/**
	 * The desired logic advance, in seconds, or -1 to disable fixed interval
	 * advances. Be careful when setting a fixed logic advance - things will not
	 * go well if executing the logic code takes longer than the logic advance
	 * value.
	 */
	@Variable( "Logic advance" )
	@Summary( "Fixed logic tick delta in seconds, or <0 for variable advance" )
	public static float logicAdvance = -1;

	/**
	 * Profiles game logic, drawing and GL Rendering
	 */
	@Variable( "Loop timer" )
	public final CodeTimer timer = new CodeTimer( "RUGL loop", Output.Millis,
			Output.Millis );

	private Phase currentPhase;

	private boolean phaseInited = false;

	private long lastLogic = System.currentTimeMillis();

	private final GLVersion requiredVersion;

	/**
	 * @param ga
	 *           used solely to quit when we run out of phases
	 * @param requiredVersion
	 *           The {@link GLVersion} that will be required in the game, or
	 *           <code>null</code> not to bother checking
	 * @param phase
	 *           The initial phase
	 */
	public Game( final GameActivity ga, final GLVersion requiredVersion,
			final Phase phase )
	{
		this.ga = ga;
		this.requiredVersion = requiredVersion;
		currentPhase = phase;
	}

	@Override
	public void onSurfaceCreated( final GL10 gl, final EGLConfig config )
	{
		Log.i( RUGL_TAG, "Surface created at " + new Date() );

		final String glVersionString = GLES10.glGetString( GLES10.GL_VERSION );
		final String extensionsString = GLES10.glGetString( GLES10.GL_EXTENSIONS );

		final StringBuilder buff = new StringBuilder();
		buff.append( "\tVendor = " ).append(
				GLES10.glGetString( GLES10.GL_VENDOR ) );
		buff.append( "\n\tRenderer = " ).append(
				GLES10.glGetString( GLES10.GL_RENDERER ) );
		buff.append( "\n\tVersion = " ).append( glVersionString );
		buff.append( "\n\tExtensions" );
		if( extensionsString != null )
		{
			for( final String ex : extensionsString.split( " " ) )
			{
				buff.append( "\n\t\t" + ex );
			}
		}
		else
		{
			buff.append( "null" );
		}

		ExceptionHandler.addLogInfo( "GLInfo", buff.toString() );

		Log.i( RUGL_TAG, buff.toString() );

		glVersion = GLVersion.findVersion( glVersionString );

		Log.i( RUGL_TAG, "Detected " + glVersion );

		if( requiredVersion != null
				&& requiredVersion.ordinal() > glVersion.ordinal() )
		{
			// requirements fail!
			ga.showToast( "Required OpenGLES version " + requiredVersion
					+ " but found version " + glVersion, true );
			ga.finish();
		}

		GLUtil.enableVertexArrays();

		phaseInited = false;

		lastLogic = System.currentTimeMillis();

		GLUtil.checkGLError();

		for( int i = 0; i < surfaceListeners.size(); i++ )
		{
			surfaceListeners.get( i ).onSurfaceCreated();
		}
	}

	/**
	 * Default implementation is to set up a 1:1 orthographic projection. Touches
	 * are scaled are similarly scaled 1:1
	 */
	@Override
	public void onSurfaceChanged( final GL10 gl, final int width,
			final int height )
	{
		Game.screenWidth = width;
		Game.screenHeight = height;

		GLUtil.scaledOrtho( gameWidth, gameHeight, screenWidth, screenHeight, -1,
				1 );

		Log.i( RUGL_TAG, "Surface changed " + width + " x " + height );

		GLUtil.checkGLError();

		for( int i = 0; i < surfaceListeners.size(); i++ )
		{
			surfaceListeners.get( i ).onSurfaceChanged( width, height );
		}

		Touch.setScreenSize( gameWidth, Game.gameHeight, Game.screenWidth,
				Game.screenHeight );
	}

	@Override
	public void onDrawFrame( final GL10 gl )
	{
		if( currentPhase == null )
		{ // time to quit
			Log.i( RUGL_TAG, "Exiting" );
			ga.finish();
			return;
		}

		if( !phaseInited )
		{
			Log.i( RUGL_TAG, "Phase " + currentPhase + " initing" );
			currentPhase.openGLinit();
			currentPhase.init( this );
			phaseInited = true;
		}

		if( confRoots != null )
		{
			Preflect.applyDeferredConfigurations( confRoots );
		}

		if( resetTouches )
		{
			Touch.reset();
			resetTouches = false;
		}

		ResourceLoader.checkCompletion();

		timer.tick( "input" );

		Touch.processTouches();

		timer.tick( "logic" );

		final long now = System.currentTimeMillis();
		final long dur = now - lastLogic;

		if( logicAdvance > 0 )
		{
			while( lastLogic < now )
			{
				currentPhase.advance( logicAdvance );
				lastLogic += ( long ) ( 1000 * logicAdvance );
			}
		}
		else
		{
			currentPhase.advance( dur / 1000.0f );
		}

		lastLogic = now;

		timer.tick( "draw " );

		currentPhase.draw();

		timer.lastTick();

		// phase transition
		if( currentPhase.complete )
		{
			Log.i( RUGL_TAG, "Phase " + currentPhase + " complete" );
			currentPhase = currentPhase.next();
			phaseInited = false;
		}

		GLUtil.checkGLError();
	}

	/**
	 * @return the current phase
	 */
	public Phase currentPhase()
	{
		return currentPhase;
	}

	/**
	 * @author ryanm
	 */
	public abstract static class SurfaceListener
	{
		/**
		 * Called when the surface is created
		 */
		public void onSurfaceCreated()
		{
		}

		/**
		 * Called when the surface is changed
		 * 
		 * @param width
		 * @param height
		 */
		public void onSurfaceChanged( final int width, final int height )
		{
		}
	}
}
