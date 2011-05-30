package com.ryanm.droid.rugl.geom.line;

import java.util.List;

import com.ryanm.droid.rugl.util.geom.LineUtils;
import com.ryanm.droid.rugl.util.geom.Vector2f;

/**
 * A simple bevelled join
 * 
 * @author ryanm
 */
public class BevelJoin implements LineJoin
{

	@Override
	public void createVerts( final Vector2f v1, final Vector2f join,
			final Vector2f v2, final Vector2f corner, final List<Vector2f> verts,
			final List<Short> indices )
	{
		final Vector2f e1 = Vector2f.sub( v1, join, null );
		e1.scale( 0.5f );
		Vector2f.add( corner, e1, e1 );

		final Vector2f e2 = Vector2f.sub( v2, join, null );
		e2.scale( 0.5f );
		Vector2f.add( corner, e2, e2 );

		final Short i1 = new Short( ( short ) ( verts.size() - 3 ) );
		final Short ij = new Short( ( short ) ( verts.size() - 2 ) );
		final Short i2 = new Short( ( short ) ( verts.size() - 1 ) );

		final Short b1 = new Short( ( short ) verts.size() );
		verts.add( e1 );
		final Short b2 = new Short( ( short ) verts.size() );
		verts.add( e2 );

		final int ccw = LineUtils.relativeCCW( v1, join, v2 );

		Line.addTriangle( ij, i1, b1, ccw, indices );

		Line.addTriangle( ij, b1, b2, ccw, indices );

		Line.addTriangle( ij, b2, i2, ccw, indices );
	}
}
