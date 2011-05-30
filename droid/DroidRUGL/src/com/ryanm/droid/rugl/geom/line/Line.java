package com.ryanm.droid.rugl.geom.line;

import java.util.LinkedList;
import java.util.List;

import com.ryanm.droid.rugl.geom.Shape;
import com.ryanm.droid.rugl.geom.ShapeUtil;
import com.ryanm.droid.rugl.util.geom.LineUtils;
import com.ryanm.droid.rugl.util.geom.Vector2f;
import com.ryanm.droid.rugl.util.geom.VectorUtils;

/**
 * Renders a sequence of line segments into a line or line loop. Doesn't behave
 * very well when the angle between adjacent segments gets small, so watch it.
 * On the other hand, there's no overdraw, so go nuts with the transparency.
 * 
 * @author ryanm
 */
public class Line
{
	private final LinkedList<Vector2f> points = new LinkedList<Vector2f>();

	/**
	 * The width of the renderer line
	 */
	public float width = 1;

	/**
	 * The decoration for the ends of the line
	 */
	public LineCap cap = null;

	/**
	 * The decoration for line joins
	 */
	public LineJoin join = null;

	private Vector2f lastAdded = null;

	private Vector2f lastButOneAdded = null;

	/**
	 * Adds a point to the line sequence. Sequential coincident points will be
	 * ignored.
	 * 
	 * @param p
	 */
	public void addPoint( final Vector2f p )
	{
		if( lastAdded == null || lastAdded.x != p.x || lastAdded.y != p.y )
		{
			if( lastAdded == null || lastButOneAdded == null )
			{
				points.add( p );
				lastButOneAdded = lastAdded;
				lastAdded = p;
			}
			else if( p.x != lastAdded.x || p.y != lastAdded.y )
			{
				assert points.size() >= 2 : points.size();

				final int ccw =
						LineUtils.relativeCCW( lastButOneAdded, lastAdded, p );

				if( ccw == 0 )
				{
					points.removeLast();

					points.add( p );
					lastAdded = p;
				}
				else
				{
					points.add( p );
					lastButOneAdded = lastAdded;
					lastAdded = p;
				}
			}
		}
	}

	/**
	 * Gets the number of points in this line
	 * 
	 * @return The number of points
	 */
	public int pointCount()
	{
		return points.size();
	}

	/**
	 * Clears the current point sequence
	 */
	public void clear()
	{
		points.clear();
		lastAdded = null;
		lastButOneAdded = null;
	}

	/**
	 * Builds a {@link Shape} that represents the current line.
	 * 
	 * @param z
	 *           The z coordinate of the line
	 * @return The shape that represents the line
	 */
	public Shape buildLine( final float z )
	{
		Shape s = null;

		if( points.size() >= 2 )
		{
			final List<Vector2f> v = new LinkedList<Vector2f>();
			final List<Short> i = new LinkedList<Short>();

			Vector2f last = null;
			Vector2f current = points.removeFirst();
			Vector2f next = points.removeFirst();

			start( current, next, v, i );

			short lastLeft = 0, lastRight = 1, nextLeft, nextRight;
			short lastIndex;
			int turn;
			while( !points.isEmpty() )
			{
				last = current;
				current = next;
				next = points.removeFirst();

				lastIndex = ( short ) ( v.size() - 1 );
				turn = corner( last, current, next, v, i );

				assert turn != 0;

				if( turn == -1 )
				{
					// right
					nextLeft = ( short ) ( lastIndex + 2 );
					nextRight = ( short ) ( lastIndex + 1 );

					addQuad( i, lastRight, lastLeft, nextRight, nextLeft );

					lastLeft = nextLeft;
					lastRight = ( short ) ( lastIndex + 3 );
				}
				else if( turn == 1 )
				{
					// left
					nextLeft = ( short ) ( lastIndex + 1 );
					nextRight = ( short ) ( lastIndex + 2 );

					addQuad( i, lastRight, lastLeft, nextRight, nextLeft );

					lastLeft = ( short ) ( lastIndex + 3 );
					lastRight = nextRight;
				}
				else
				{
					// wasn't a turn
					assert false;
				}
			}

			lastIndex = ( short ) ( v.size() - 1 );
			end( current, next, v, i );

			nextLeft = ( short ) ( lastIndex + 1 );
			nextRight = ( short ) ( lastIndex + 2 );

			addQuad( i, lastRight, lastLeft, nextRight, nextLeft );

			assert points.isEmpty();

			s =
					new Shape( ShapeUtil.extractVerts( v, z ),
							ShapeUtil.extractIndices( i ) );
		}

		clear();

		return s;
	}

	/**
	 * Builds a line loop shape
	 * 
	 * @param z
	 *           The z-coordinate of the loop
	 * @return The loop shape, or null if there were less than three points
	 */
	public Shape buildLoop( final float z )
	{
		Shape s = null;

		if( points.size() == 2 )
		{
			return buildLine( z );
		}

		if( points.size() >= 3 )
		{
			// check if penultimate-last-first is colinear, and remove
			// last if needed
			{
				final Vector2f first = points.getFirst();
				final Vector2f last = points.removeLast();
				final Vector2f penultimate = points.getLast();

				if( LineUtils.relativeCCW( penultimate, last, first ) != 0 )
				{
					points.add( last );
				}
			}

			if( points.size() == 2 )
			{
				return buildLine( z );
			}

			final List<Vector2f> v = new LinkedList<Vector2f>();
			final List<Short> i = new LinkedList<Short>();

			final Vector2f first = points.removeFirst();
			final Vector2f second = points.removeFirst();

			points.add( first );
			points.add( second );

			Vector2f prev = first;
			Vector2f current = second;
			Vector2f next = points.removeFirst();

			int turn = corner( prev, current, next, v, i );

			short firstLeft = -1, firstRight = -1;
			short lastLeft = -1, lastRight = -1, nextLeft, nextRight;
			int lastIndex;

			if( turn == 1 )
			{
				firstLeft = 1;
				firstRight = 0;
				lastLeft = 1;
				lastRight = 2;
			}
			else if( turn == -1 )
			{
				firstLeft = 0;
				firstRight = 1;
				lastLeft = 2;
				lastRight = 1;
			}
			else
			{
				assert false;
			}

			while( !points.isEmpty() )
			{
				prev = current;
				current = next;
				next = points.removeFirst();

				lastIndex = v.size() - 1;
				turn = corner( prev, current, next, v, i );

				if( turn == 1 )
				{
					nextLeft = ( short ) ( lastIndex + 2 );
					nextRight = ( short ) ( lastIndex + 1 );

					addQuad( i, lastLeft, lastRight, nextLeft, nextRight );

					lastLeft = nextLeft;
					lastRight = ( short ) ( lastIndex + 3 );
				}
				else if( turn == -1 )
				{
					nextLeft = ( short ) ( lastIndex + 1 );
					nextRight = ( short ) ( lastIndex + 2 );

					addQuad( i, lastLeft, lastRight, nextLeft, nextRight );

					lastLeft = ( short ) ( lastIndex + 3 );
					lastRight = nextRight;
				}
				else
				{
					assert false;
				}

			}

			assert lastLeft != -1;
			addQuad( i, lastLeft, lastRight, firstLeft, firstRight );

			s =
					new Shape( ShapeUtil.extractVerts( v, z ),
							ShapeUtil.extractIndices( i ) );
		}

		clear();

		return s;
	}

	/**
	 * Computes the starting vertices, the left vertex is computed first
	 * 
	 * @param first
	 * @param second
	 * @param verts
	 * @param indices
	 */
	private void start( final Vector2f first, final Vector2f second,
			final List<Vector2f> verts, final List<Short> indices )
	{
		final Vector2f dir = Vector2f.sub( second, first, null );
		dir.normalise();
		dir.scale( width / 2.0f );

		VectorUtils.rotate90( dir );

		verts.add( Vector2f.add( first, dir, null ) );
		verts.add( Vector2f.sub( first, dir, null ) );

		if( cap != null )
		{
			// rotate the direction vector back
			VectorUtils.rotateMinus90( dir );
			dir.normalise();

			cap.createVerts( first, dir, ( short ) ( verts.size() - 2 ),
					( short ) ( verts.size() - 1 ), width, verts, indices );
		}
	}

	/**
	 * Computes the ending vertices, the left vertex is computed first. Also does
	 * line capping
	 * 
	 * @param penultimate
	 * @param last
	 * @param verts
	 * @param indices
	 */
	private void end( final Vector2f penultimate, final Vector2f last,
			final List<Vector2f> verts, final List<Short> indices )
	{
		final Vector2f dir = Vector2f.sub( penultimate, last, null );
		if( dir.x == 0 && dir.y == 0 )
		{
			dir.set( 1, 0 );
		}

		dir.normalise();
		dir.scale( width / 2.0f );

		VectorUtils.rotate90( dir );

		// note that we sub first here to get the left corner
		verts.add( Vector2f.sub( last, dir, null ) );
		verts.add( Vector2f.add( last, dir, null ) );

		if( cap != null )
		{
			// rotate the direction vector back
			VectorUtils.rotateMinus90( dir );
			dir.normalise();

			cap.createVerts( last, dir, ( short ) ( verts.size() - 1 ),
					( short ) ( verts.size() - 2 ), width, verts, indices );
		}
	}

	/**
	 * Computes the three vertices that make up a corner. If this corner is a
	 * left-hander, the vertices will be added in the order right-corner-right.
	 * If a right-hander, vertices will be in left-corner-left order. No vertices
	 * will be added if the three points are colinear. Also adds line join
	 * decorations
	 * 
	 * @param previous
	 *           The previous point
	 * @param current
	 *           The corner point
	 * @param next
	 *           The next point
	 * @param verts
	 *           The list to add to
	 * @return 1 if this was a left-hand corner, -1 if a right-hander, 0 if it
	 *         wasn't really a corner at all
	 */
	private int corner( final Vector2f previous, final Vector2f current,
			final Vector2f next, final List<Vector2f> verts,
			final List<Short> indices )
	{
		final int ccw = LineUtils.relativeCCW( previous, current, next );

		if( ccw != 0 )
		{
			final Vector2f pn = Vector2f.sub( current, previous, null );
			pn.normalise();
			pn.scale( width / 2.0f );
			float x = pn.x;
			pn.x = -pn.y;
			pn.y = x;

			final Vector2f nn = Vector2f.sub( next, current, null );
			nn.normalise();
			nn.scale( width / 2.0f );
			x = nn.x;
			nn.x = -nn.y;
			nn.y = x;

			pn.scale( ccw );
			nn.scale( ccw );

			// left
			final Vector2f pp1 = Vector2f.sub( previous, pn, null );
			final Vector2f pp2 = Vector2f.sub( current, pn, null );
			final Vector2f np1 = Vector2f.sub( current, nn, null );
			final Vector2f np2 = Vector2f.sub( next, nn, null );

			final Vector2f corner =
					LineUtils.lineIntersection( pp1, pp2, np1, np2, null );
			pn.scale( 2 );
			nn.scale( 2 );
			final Vector2f pleft = Vector2f.add( corner, pn, null );
			final Vector2f nleft = Vector2f.add( corner, nn, null );

			verts.add( pleft );
			verts.add( corner );
			verts.add( nleft );

			if( join != null )
			{
				join.createVerts( pleft, corner, nleft, current, verts, indices );
			}
		}

		return ccw;
	}

	/**
	 * Builds a capped line segment shape
	 * 
	 * @param start
	 *           The start point of the segment
	 * @param end
	 *           The end point of the segment
	 * @param z
	 *           The z-coordinate of the segment
	 * @return A {@link Shape} representing the segment
	 */
	public Shape buildSegmentShape( final Vector2f start, final Vector2f end,
			final float z )
	{
		final List<Vector2f> v = new LinkedList<Vector2f>();
		final List<Short> i = new LinkedList<Short>();

		start( start, end, v, i );

		final short lastLeft = 0, lastRight = 1;
		short nextLeft, nextRight;

		final int lastIndex = v.size() - 1;
		end( start, end, v, i );

		nextLeft = ( short ) ( lastIndex + 1 );
		nextRight = ( short ) ( lastIndex + 2 );

		addQuad( i, lastLeft, lastRight, nextLeft, nextRight );

		return new Shape( ShapeUtil.extractVerts( v, z ),
				ShapeUtil.extractIndices( i ) );
	}

	static void addQuad( final List<Short> indices, final short lastLeft,
			final short lastRight, final short nextLeft, final short nextRight )
	{
		assert lastLeft != lastRight;
		assert lastLeft != nextLeft;
		assert lastLeft != nextRight;

		assert lastRight != nextLeft;
		assert lastRight != nextRight;

		assert nextLeft != nextRight;

		final Short pl = new Short( lastLeft );
		final Short pr = new Short( lastRight );
		final Short nl = new Short( nextLeft );
		final Short nr = new Short( nextRight );

		indices.add( pl );
		indices.add( nl );
		indices.add( pr );

		indices.add( nl );
		indices.add( nr );
		indices.add( pr );
	}

	static void addTriangle( final Short i1, final Short i2, final Short i3,
			final int ccw, final List<Short> indices )
	{
		if( ccw == 1 )
		{
			indices.add( i1 );
			indices.add( i2 );
			indices.add( i3 );
		}
		else
		{
			indices.add( i1 );
			indices.add( i3 );
			indices.add( i2 );
		}
	}
}
