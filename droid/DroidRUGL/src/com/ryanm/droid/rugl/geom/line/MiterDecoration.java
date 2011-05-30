package com.ryanm.droid.rugl.geom.line;

import java.util.List;

import com.ryanm.droid.rugl.util.geom.LineUtils;
import com.ryanm.droid.rugl.util.geom.Vector2f;
import com.ryanm.droid.rugl.util.geom.VectorUtils;

/**
 * A mitered decoration, for pointy corners and caps
 * 
 * @author ryanm
 */
public class MiterDecoration implements LineJoin, LineCap
{
	/**
	 * The distance from the corner point that the bevel will be initiated. The
	 * mitre point will not extend further than the maximum of this distance and
	 * the line thickness from the corner point. Thus, if the bevelLimit <= line
	 * width, corners of up to 60 degrees will be pointy, more acute than that
	 * will be bevelled
	 */
	public float bevelLimit = 0;

	/**
	 * The distance from the line end to the point of the cap, in terms of the
	 * line width. bevelLimit will be observed if it is larger than the line
	 * width
	 */
	public float capLength = 1;

	@Override
	public void createVerts( final Vector2f v1, final Vector2f join,
			final Vector2f v2, final Vector2f corner, final List<Vector2f> verts,
			final List<Short> indices )
	{
		final Vector2f e1 = Vector2f.sub( v1, join, null );

		final float bl = Math.max( bevelLimit, e1.length() );

		VectorUtils.rotate90( e1 );
		Vector2f.add( v1, e1, e1 );

		final Vector2f e2 = Vector2f.sub( v2, join, null );
		VectorUtils.rotate90( e2 );
		Vector2f.add( v2, e2, e2 );

		final Vector2f intersection =
				LineUtils.lineIntersection( v1, e1, v2, e2, null );

		final int ccw = LineUtils.relativeCCW( v1, join, v2 );

		assert intersection != null;

		final Short i1 = new Short( ( short ) ( verts.size() - 3 ) );
		final Short ij = new Short( ( short ) ( verts.size() - 2 ) );
		final Short i2 = new Short( ( short ) ( verts.size() - 1 ) );

		if( VectorUtils.distanceSquared( corner, intersection ) > bl * bl )
		{ // we need to bevel
			final Vector2f b1 = getFarthestIntersection( v1, e1, corner, bl, join );
			final Vector2f b2 = getFarthestIntersection( v2, e2, corner, bl, join );

			final Short bi1 = new Short( ( short ) verts.size() );
			verts.add( b1 );

			final Short bi2 = new Short( ( short ) verts.size() );
			verts.add( b2 );

			Line.addTriangle( ij, i1, bi1, ccw, indices );
			Line.addTriangle( ij, bi1, bi2, ccw, indices );
			Line.addTriangle( ij, bi2, i2, ccw, indices );
		}
		else
		{ // pointy
			final Short ii = new Short( ( short ) verts.size() );
			verts.add( intersection );

			Line.addTriangle( ij, i1, ii, ccw, indices );

			Line.addTriangle( ii, i2, ij, ccw, indices );
		}
	}

	/**
	 * Gets the intersection between a line and a circle that is farthest from
	 * some point
	 * 
	 * @param lp1
	 *           A point on the line
	 * @param lp2
	 *           Another point on the line
	 * @param circle
	 *           The center of the circle
	 * @param radius
	 *           The radius of the circle
	 * @param pt
	 *           The point to test distance with
	 * @return The intersection that is farthest from pt
	 */
	private static Vector2f getFarthestIntersection( final Vector2f lp1,
			final Vector2f lp2, final Vector2f circle, final float radius,
			final Vector2f pt )
	{
		final Vector2f[] intersections =
				LineUtils.lineCircleIntersection( lp1, lp2, circle, radius );

		assert intersections.length > 0;

		Vector2f i = LineUtils.closestPointOnLine( circle, lp1, lp2 );
		float md = VectorUtils.distanceSquared( i, pt );

		for( int j = 0; j < intersections.length; j++ )
		{
			final float d = VectorUtils.distanceSquared( intersections[ j ], pt );

			if( d > md )
			{
				md = d;
				i = intersections[ j ];
			}
		}

		return i;
	}

	@Override
	public void createVerts( final Vector2f endPoint,
			final Vector2f lineDirection, final short leftIndex,
			final short rightIndex, final float width, final List<Vector2f> verts,
			final List<Short> indices )
	{
		final float bl = Math.max( bevelLimit, width );

		final float pl = 0.5f * width * capLength;
		lineDirection.scale( -pl );
		final Vector2f p = new Vector2f();
		Vector2f.add( endPoint, lineDirection, p );

		if( pl < bl )
		{ // pointy!
			verts.add( p );
			indices.add( new Short( rightIndex ) );
			indices.add( new Short( leftIndex ) );
			indices.add( new Short( ( short ) ( verts.size() - 1 ) ) );
		}
		else
		{ // need to bevel
			final Vector2f li =
					LineUtils.segmentCircleIntersection( verts.get( leftIndex ), p,
							endPoint, bl )[ 0 ];
			final Vector2f ri =
					LineUtils.segmentCircleIntersection( verts.get( rightIndex ), p,
							endPoint, bl )[ 0 ];

			final int lii = verts.size();
			verts.add( li );
			final int rii = verts.size();
			verts.add( ri );

			indices.add( new Short( leftIndex ) );
			indices.add( new Short( ( short ) lii ) );
			indices.add( new Short( ( short ) rii ) );

			indices.add( new Short( leftIndex ) );
			indices.add( new Short( ( short ) rii ) );
			indices.add( new Short( rightIndex ) );
		}
	}
}
