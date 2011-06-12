/*
 * Copyright (c) 2002 Shaven Puppy Ltd All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. Neither
 * the name of 'Shaven Puppy' nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ryanm.droid.rugl.gl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import android.opengl.GLES10;
import android.opengl.GLException;

import com.ryanm.droid.rugl.gl.enums.ComparisonFunction;
import com.ryanm.droid.rugl.gl.enums.DestinationFactor;
import com.ryanm.droid.rugl.gl.enums.SourceFactor;
import com.ryanm.droid.rugl.gl.facets.AlphaTest;
import com.ryanm.droid.rugl.gl.facets.Blend;
import com.ryanm.droid.rugl.gl.facets.DepthTest;

/**
 * @author foo
 */
public class GLUtil
{
	private static ByteBuffer scratch = BufferUtils.createByteBuffer( 4 * 16 );

	/**
	 * A rendering state containing typical, but not default, state: blending,
	 * depth test, alpha test etc
	 */
	public static final State typicalState = new State()
			.with(
					new Blend( SourceFactor.SRC_ALPHA,
							DestinationFactor.ONE_MINUS_SRC_ALPHA ) )
			.with( new DepthTest( ComparisonFunction.LEQUAL ) )
			.with( new AlphaTest( ComparisonFunction.GREATER, 0 ) );

	/**
	 * @param size
	 * @return An {@link IntBuffer} handy for temporary use
	 */
	public static IntBuffer intScratch( final int size )
	{
		scratch.clear();
		scratch.limit( 4 * size );
		return scratch.asIntBuffer();
	}

	/**
	 * Returns the nearest power of 2, which is either n if n is already a power
	 * of 2, or the next higher number than n which is a power of 2.
	 * 
	 * @param n
	 * @return The smallest power of two that is larger than n
	 */
	public static int nextPowerOf2( final int n )
	{
		int x = 1;

		while( x < n )
		{
			x <<= 1;
		}

		return x;
	}

	/**
	 * Sets an orthographic projection, origin in the bottom-left
	 * 
	 * @param desiredWidth
	 * @param desiredHeight
	 * @param screenWidth
	 * @param screenHeight
	 * @param near
	 *           distance to near clip plane
	 * @param far
	 *           distance to far clip plane
	 */
	public static void scaledOrtho( final float desiredWidth,
			final float desiredHeight, final int screenWidth,
			final int screenHeight, final float near, final float far )
	{
		GLES10.glMatrixMode( GLES10.GL_PROJECTION );
		GLES10.glLoadIdentity();
		GLES10.glOrthof( 0, desiredWidth, 0, desiredHeight, near, far );

		GLES10.glMatrixMode( GLES10.GL_MODELVIEW );
		GLES10.glLoadIdentity();

		GLES10.glViewport( 0, 0, screenWidth, screenHeight );
	}

	/**
	 * Throws {@link GLException} if {@link GLES10#glGetError()} returns anything
	 * other than {@link GLES10#GL_NO_ERROR}
	 * 
	 * @throws GLException
	 */
	public static void checkGLError() throws GLException
	{
		final int err = GLES10.glGetError();
		if( err != GLES10.GL_NO_ERROR )
		{
			throw new GLException( err );
		}
	}

	/**
	 * Enables the client state for using vertex arrays and VBOs
	 */
	public static void enableVertexArrays()
	{
		GLES10.glEnableClientState( GLES10.GL_VERTEX_ARRAY );
		GLES10.glEnableClientState( GLES10.GL_TEXTURE_COORD_ARRAY );
		GLES10.glEnableClientState( GLES10.GL_COLOR_ARRAY );
	}
}
