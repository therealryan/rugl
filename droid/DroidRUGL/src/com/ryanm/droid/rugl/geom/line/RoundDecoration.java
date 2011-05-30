package com.ryanm.droid.rugl.geom.line;

import java.util.List;

import com.ryanm.droid.rugl.util.geom.LineUtils;
import com.ryanm.droid.rugl.util.geom.Vector2f;
import com.ryanm.droid.rugl.util.geom.VectorUtils;

/**
 * Rounded line decorations
 * 
 * @author ryanm
 */
public class RoundDecoration implements LineCap, LineJoin
{
	/**
	 * The maximum length of the segments used to form the rounded bits
	 */
	public float maxSegmentLength = 10;

	@Override
	public void createVerts( final Vector2f endPoint,
			final Vector2f lineDirection, final short leftIndex,
			final short rightIndex, final float width, final List<Vector2f> verts,
			final List<Short> indices )
	{
		final float c = ( float ) ( Math.PI * width / 2.0f );
		final int segments = ( int ) Math.ceil( c / maxSegmentLength );

		VectorUtils.rotate90( lineDirection );
		lineDirection.scale( width / 2 );

		final float angleIncrement = ( float ) ( Math.PI / segments );

		final Short left = new Short( leftIndex );
		Short prev = null;
		for( int i = 0; i < segments - 1; i++ )
		{
			VectorUtils.rotate( lineDirection, angleIncrement );

			verts.add( Vector2f.add( endPoint, lineDirection, null ) );
			final Short index = new Short( ( short ) ( verts.size() - 1 ) );

			if( prev != null )
			{
				indices.add( left );
				indices.add( prev );
				indices.add( index );
			}

			prev = index;
		}

		if( prev != null )
		{
			indices.add( left );
			indices.add( prev );
			indices.add( new Short( rightIndex ) );
		}
	}

	@Override
	public void createVerts( final Vector2f v1, final Vector2f join,
			final Vector2f v2, final Vector2f corner, final List<Vector2f> verts,
			final List<Short> indices )
	{
		final Short i1 = new Short( ( short ) ( verts.size() - 3 ) );
		final Short root = new Short( ( short ) ( verts.size() - 2 ) );
		final Short i2 = new Short( ( short ) ( verts.size() - 1 ) );

		final Vector2f n1 = Vector2f.sub( v1, join, null );
		n1.scale( 0.5f );

		final Vector2f n2 = Vector2f.sub( v2, join, null );
		n2.scale( 0.5f );

		final int ccw = LineUtils.relativeCCW( v1, join, v2 );

		final float angle = Vector2f.angle( n1, n2 );

		final float circ =
				( float ) ( n1.length() * 2 * Math.PI * angle / ( Math.PI * 2 ) );
		final int segments = ( int ) Math.ceil( circ / maxSegmentLength );
		final float angleIncrement = ccw * angle / segments;

		Short prev = new Short( ( short ) verts.size() );

		verts.add( Vector2f.add( corner, n1, null ) );

		Line.addTriangle( root, i1, prev, ccw, indices );

		for( int i = 0; i < segments - 1; i++ )
		{
			VectorUtils.rotate( n1, angleIncrement );
			verts.add( Vector2f.add( corner, n1, null ) );
			final Short current = new Short( ( short ) ( verts.size() - 1 ) );

			Line.addTriangle( root, prev, current, ccw, indices );

			prev = current;
		}

		verts.add( Vector2f.add( corner, n2, null ) );
		final Short j = new Short( ( short ) ( verts.size() - 1 ) );

		Line.addTriangle( root, prev, j, ccw, indices );

		Line.addTriangle( root, j, i2, ccw, indices );
	}
}
