
package com.ryanm.droid.rugl.input;

import com.ryanm.droid.config.annote.DirtyFlag;
import com.ryanm.droid.config.annote.Summary;
import com.ryanm.droid.config.annote.Variable;
import com.ryanm.droid.rugl.geom.ColouredShape;
import com.ryanm.droid.rugl.geom.ShapeUtil;
import com.ryanm.droid.rugl.gl.GLUtil;
import com.ryanm.droid.rugl.gl.StackedRenderer;
import com.ryanm.droid.rugl.input.Touch.Pointer;
import com.ryanm.droid.rugl.util.Colour;
import com.ryanm.droid.rugl.util.geom.BoundingRectangle;

/**
 * An area that causes a {@link TouchStick} to appear when and where a
 * touch is placed. This solves the unwanted initial input when you
 * don't manage to place your touch exactly in the center of a static
 * {@link TouchStick}
 * 
 * @author ryanm
 */
@Variable( "Touchstick area" )
public class TouchStickArea extends AbstractTouchStick
{
	/***/
	@Variable( "Draw" )
	@Summary( "Outline the sensitive area" )
	public boolean draw = true;

	/***/
	@Variable( "Pad area" )
	@Summary( "Position and size of sensitive area" )
	public BoundingRectangle pad = new BoundingRectangle();

	/***/
	@Variable
	public final TouchStick stick;

	private ColouredShape outline;

	/**
	 * @param x
	 *           left edge of area
	 * @param y
	 *           lower edge of area
	 * @param width
	 * @param height
	 * @param stickRadius
	 *           radius of stick that appears
	 */
	public TouchStickArea( float x, float y, float width, float height, float stickRadius )
	{
		pad.set( x, x + width, y, y + height );

		stick = new TouchStick( x, y, stickRadius );

		// redirect clicks to our listener
		stick.listener = new ClickListener() {
			@Override
			public void onClick()
			{
				if( listener != null )
				{
					listener.onClick();
				}
			}
		};
	}

	@Override
	public void pointerAdded( Pointer p )
	{
		if( pad.contains( p.x, p.y ) )
		{
			touch = p;

			stick.setPosition( p.x, p.y );

			stick.pointerAdded( p );
		}
	}

	@Override
	public void pointerRemoved( Pointer p )
	{
		if( p == touch )
		{
			stick.pointerRemoved( p );
			touch = null;
		}
	}

	@Override
	public void advance()
	{
		stick.advance();
		x = stick.x;
		y = stick.y;
	}

	@Override
	public void draw( StackedRenderer sr )
	{
		if( draw && touch == null )
		{
			if( outline == null )
			{
				outline =
						new ColouredShape( ShapeUtil.innerQuad( pad.x.getMin(), pad.y.getMin(),
								pad.x.getMax(), pad.y.getMax(), 5, 0 ), Colour.packFloat( 1, 1,
								1, 0.25f ), GLUtil.typicalState );
			}

			outline.render( sr );
		}
	}

	/***/
	@DirtyFlag
	public void oulineDirty()
	{
		outline = null;
	}
}