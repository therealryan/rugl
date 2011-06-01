package com.ryanm.droid.rugl.input;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.view.MotionEvent;

import com.ryanm.droid.rugl.Game;

/**
 * Provides a polling-style interface to multitouch pointers. Note that this
 * flips the y-axis so the the origin is at the bottom-left of the screen
 * 
 * @author ryanm
 */
public class Touch
{
	private static ArrayList<TouchListener> listeners =
			new ArrayList<Touch.TouchListener>();

	private static float xScale = 1, yScale = 1;

	/**
	 * An array of pointers. They can be active or not
	 */
	public static final Pointer[] pointers = new Pointer[8];
	private static final boolean[] wasActive = new boolean[8];
	static
	{
		for( int i = 0; i < pointers.length; i++ )
		{
			pointers[ i ] = new Pointer( i );
		}
	}

	private static final Queue<MotionEvent> touchEvents =
			new ConcurrentLinkedQueue<MotionEvent>();

	/**
	 * @param me
	 */
	public static void onTouchEvent( final MotionEvent me )
	{
		touchEvents.offer( me );
	}

	/**
	 * Call this once per frame to process touch events on the main thread
	 */
	public static void processTouches()
	{
		while( !touchEvents.isEmpty() )
		{
			final MotionEvent me = touchEvents.poll();

			updatePointers( me );

			if( me.getAction() == MotionEvent.ACTION_UP )
			{ // final touch has left
				for( int i = 0; i < pointers.length; i++ )
				{
					if( pointers[ i ].active )
					{
						pointers[ i ].active = false;

						for( int j = 0; j < listeners.size(); j++ )
						{
							listeners.get( j ).pointerRemoved( pointers[ i ] );
						}
					}
				}
			}
		}
	}

	private static void updatePointers( final MotionEvent me )
	{
		final int pointerCount = me.getPointerCount();

		for( int i = 0; i < pointers.length; i++ )
		{
			wasActive[ i ] = pointers[ i ].active;
			pointers[ i ].active = false;
		}

		for( int i = 0; i < pointerCount; i++ )
		{
			final Pointer p = pointers[ me.getPointerId( i ) ];

			p.active = true;
			p.x = me.getX( i ) * xScale;
			p.y = ( Game.screenHeight - me.getY( i ) ) * yScale;
			p.size = me.getSize( i );
		}

		for( int i = 0; i < pointers.length; i++ )
		{
			if( pointers[ i ].active && !wasActive[ i ] )
			{
				// added
				for( int j = 0; j < listeners.size(); j++ )
				{
					listeners.get( j ).pointerAdded( pointers[ i ] );
				}
			}
			else if( !pointers[ i ].active && wasActive[ i ] )
			{
				// removed
				for( int j = 0; j < listeners.size(); j++ )
				{
					listeners.get( j ).pointerRemoved( pointers[ i ] );
				}
			}
		}
	}

	/**
	 * Sets scaling factors for translation between physical and desired
	 * coordinate systems
	 * 
	 * @param desiredWidth
	 * @param desiredHeight
	 * @param actualWidth
	 * @param actualHeight
	 */
	public static void setScreenSize( final float desiredWidth,
			final float desiredHeight, final int actualWidth,
			final int actualHeight )
	{
		xScale = desiredWidth / actualWidth;
		yScale = desiredHeight / actualHeight;
	}

	/**
	 * @param l
	 *           The object to inform of pointer changes
	 */
	public static void addListener( final TouchListener l )
	{
		listeners.add( l );
	}

	/**
	 * @param l
	 *           The object to stop informing of pointer changes
	 */
	public static void removeListener( final TouchListener l )
	{
		listeners.remove( l );
	}

	/**
	 * Information on one pointer
	 * 
	 * @author ryanm
	 */
	public static class Pointer
	{
		/***/
		public final int id;

		/***/
		public float x;

		/***/
		public float y;

		/***/
		public float size;

		/***/
		public boolean active = false;

		private Pointer( final int id )
		{
			this.id = id;
		}

		@Override
		public String toString()
		{
			if( !active )
			{
				return "Inactive";
			}
			else
			{
				final StringBuilder buff = new StringBuilder();
				buff.append( id ).append( " ( " ).append( x ).append( ", " );
				buff.append( y ).append( " ) " ).append( size );
				return buff.toString();
			}
		}
	}

	/**
	 * @author ryanm
	 */
	public interface TouchListener
	{
		/**
		 * Called when a new pointer is added to the screen
		 * 
		 * @param p
		 *           This object's fields will be updated as the pointer changes
		 * @return <code>true</code> if the touch should be consumed. No other
		 *         listeners will be notified
		 */
		public boolean pointerAdded( Pointer p );

		/**
		 * Called when a pointer is removed from the screen
		 * 
		 * @param p
		 *           This object will no longer be updated
		 */
		public void pointerRemoved( Pointer p );

		/**
		 * Called when the Touch system is initiated
		 */
		public void reset();
	}

	/**
	 * Called at startup
	 */
	public static void reset()
	{
		for( int i = 0; i < pointers.length; i++ )
		{
			pointers[ i ].active = false;
		}

		for( final TouchListener l : listeners )
		{
			l.reset();
		}
	}
}
