package com.ryanm.droid.rugl.input;

import com.ryanm.droid.rugl.geom.ColouredShape;
import com.ryanm.droid.rugl.geom.ShapeUtil;
import com.ryanm.droid.rugl.gl.GLUtil;
import com.ryanm.droid.rugl.gl.StackedRenderer;
import com.ryanm.droid.rugl.input.Touch.Pointer;
import com.ryanm.droid.rugl.util.Colour;
import com.ryanm.droid.rugl.util.geom.BoundingRectangle;
import com.ryanm.preflect.annote.Category;
import com.ryanm.preflect.annote.DirtyFlag;
import com.ryanm.preflect.annote.Summary;
import com.ryanm.preflect.annote.Variable;
import com.ryanm.preflect.annote.WidgetHint;

/**
 * An area that causes a {@link TouchStick} to appear when and where a touch is
 * placed. This solves the unwanted initial input when you don't manage to place
 * your touch exactly in the center of a static {@link TouchStick}
 * 
 * @author ryanm
 */
@Variable( "Touchstick area" )
public class TouchStickArea extends AbstractTouchStick
{
	/***/
	@Variable( "Pad area" )
	@Summary( "Position and size of sensitive area" )
	public BoundingRectangle pad = new BoundingRectangle();

	/***/
	@Variable( "Draw" )
	@Summary( "Outline the sensitive area" )
	@Category( "Appearance" )
	public boolean draw = true;

	/***/
	@Variable( "Bounds colour" )
	@Summary( "Colour of pad area outline" )
	@WidgetHint( Colour.class )
	@Category( "Appearance" )
	public int boundsColour = Colour.packFloat( 1, 1, 1, 0.3f );

	/***/
	@Variable
	@Category( "Interaction" )
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
	public TouchStickArea( final float x, final float y, final float width,
			final float height, final float stickRadius )
	{
		pad.set( x, x + width, y, y + height );

		stick = new TouchStick( x, y, stickRadius );

		// redirect clicks to our listener
		stick.listener = new ClickListener(){
			@Override
			public void onClick()
			{
				if( listener != null )
				{
					listener.onClick();
				}
			}

			@Override
			public void onClickHold( final boolean active )
			{
				if( listener != null )
				{
					listener.onClickHold( active );
				}
			}
		};
	}

	@Override
	public boolean pointerAdded( final Pointer p )
	{
		if( pad.contains( p.x, p.y ) )
		{
			touch = p;

			stick.setPosition( p.x, p.y );

			stick.pointerAdded( p );

			return true;
		}

		return false;
	}

	@Override
	public void pointerRemoved( final Pointer p )
	{
		if( p == touch )
		{
			stick.pointerRemoved( p );
			touch = null;
		}
	}

	@Override
	public void reset()
	{
		touch = null;
		stick.reset();
	}

	@Override
	public void advance()
	{
		stick.advance();
		x = stick.x;
		y = stick.y;
	}

	@Override
	public void draw( final StackedRenderer sr )
	{
		if( draw && touch == null )
		{
			if( outline == null )
			{
				outline =
						new ColouredShape( ShapeUtil.innerQuad( pad.x.getMin(),
								pad.y.getMin(), pad.x.getMax(), pad.y.getMax(), 5, 0 ),
								boundsColour, GLUtil.typicalState );
			}

			outline.render( sr );
		}
	}

	/***/
	@DirtyFlag
	public void outLineDirty()
	{
		outline = null;
	}
}
