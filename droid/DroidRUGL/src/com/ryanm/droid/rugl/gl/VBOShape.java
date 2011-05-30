package com.ryanm.droid.rugl.gl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES10;
import android.opengl.GLES11;
import android.opengl.GLException;

import com.ryanm.droid.rugl.Game;
import com.ryanm.droid.rugl.Game.SurfaceListener;
import com.ryanm.droid.rugl.geom.ColouredShape;
import com.ryanm.droid.rugl.geom.TexturedShape;

/**
 * @author ryanm
 */
public class VBOShape
{
	static
	{
		Game.addSurfaceLIstener( new SurfaceListener(){
			@Override
			public void onSurfaceCreated()
			{
				contextID++;
			}
		} );
	}

	/**
	 * Increment this to indicate that VBO state may have been invalidated, i.e.
	 * in This will cause all {@link VBOShape}s to refresh their data
	 */
	private static int contextID = 0;

	private static int vertexBytes = 3 * 4 + 4 + 2 * 4;

	/***/
	public State state;

	/**
	 * The {@link #contextID} when the data was uploaded. If this is ever
	 * different from {@link #contextID}, we know that the VBOs may have been
	 * squelched
	 */
	private int uploadedContextID;

	private int dataVBOID = -1;

	private final ByteBuffer dataBuffer;

	private int indexVBOID = -1;

	private final ShortBuffer indexBuffer;

	private final int indexCount;

	private final int vertexCount;

	/**
	 * @param ts
	 */
	public VBOShape( final TexturedShape ts )
	{
		state = ts.state;
		vertexCount = ts.vertexCount();

		dataBuffer = BufferUtils.createByteBuffer( vertexCount * vertexBytes );
		for( int i = 0; i < vertexCount; i++ )
		{
			dataBuffer.putFloat( ts.vertices[ 3 * i + 0 ] );
			dataBuffer.putFloat( ts.vertices[ 3 * i + 1 ] );
			dataBuffer.putFloat( ts.vertices[ 3 * i + 2 ] );

			dataBuffer.putInt( ts.colours[ i ] );

			dataBuffer.putFloat( ts.texCoords[ 2 * i ] );
			dataBuffer.putFloat( ts.texCoords[ 2 * i + 1 ] );
		}
		dataBuffer.flip();

		indexCount = ts.indices.length;
		indexBuffer = BufferUtils.createShortBuffer( indexCount );
		indexBuffer.put( ts.indices );
		indexBuffer.flip();
	}

	/**
	 * @param cs
	 */
	public VBOShape( final ColouredShape cs )
	{
		state = cs.state;
		vertexCount = cs.vertexCount();

		dataBuffer = BufferUtils.createByteBuffer( vertexCount * vertexBytes );
		for( int i = 0; i < vertexCount; i++ )
		{
			dataBuffer.putFloat( cs.vertices[ 3 * i + 0 ] );
			dataBuffer.putFloat( cs.vertices[ 3 * i + 1 ] );
			dataBuffer.putFloat( cs.vertices[ 3 * i + 2 ] );

			dataBuffer.putInt( cs.colours[ i ] );

			dataBuffer.putFloat( 0 );
			dataBuffer.putFloat( 0 );
		}
		dataBuffer.flip();

		indexCount = cs.indices.length;
		indexBuffer = BufferUtils.createShortBuffer( indexCount );
		indexBuffer.put( cs.indices );
		indexBuffer.flip();
	}

	/***/
	public void draw()
	{
		GLUtil.checkGLError();

		if( uploadedContextID != contextID )
		{ // the context may have changed - we need to refresh our
			// buffer handles
			delete();
		}

		if( dataVBOID == -1 )
		{
			final IntBuffer ib = GLUtil.intScratch( 2 );
			GLES11.glGenBuffers( 2, ib );
			dataVBOID = ib.get( 0 );
			indexVBOID = ib.get( 1 );

			if( dataVBOID == 0 || indexVBOID == 0 )
			{
				throw new GLException( GLES10.GL_INVALID_OPERATION,
						"Attempted to bind null buffer name : " + dataVBOID + " or "
								+ indexVBOID );
			}

			GLES11.glBindBuffer( GLES11.GL_ARRAY_BUFFER, dataVBOID );
			GLES11.glBufferData( GLES11.GL_ARRAY_BUFFER,
					vertexCount * vertexBytes, dataBuffer, GLES11.GL_STATIC_DRAW );

			GLES11.glBindBuffer( GLES11.GL_ELEMENT_ARRAY_BUFFER, indexVBOID );
			GLES11.glBufferData( GLES11.GL_ELEMENT_ARRAY_BUFFER, indexCount * 2,
					indexBuffer, GLES11.GL_STATIC_DRAW );

			uploadedContextID = contextID;

			GLUtil.checkGLError();
		}

		state.apply();

		GLES11.glBindBuffer( GLES11.GL_ARRAY_BUFFER, dataVBOID );

		GLES11.glVertexPointer( 3, GLES10.GL_FLOAT, vertexBytes, 0 );
		GLES11.glColorPointer( 4, GLES10.GL_UNSIGNED_BYTE, vertexBytes, 12 );
		GLES11.glTexCoordPointer( 2, GLES10.GL_FLOAT, vertexBytes, 16 );

		GLES11.glBindBuffer( GLES11.GL_ELEMENT_ARRAY_BUFFER, indexVBOID );
		GLES11.glDrawElements( state.drawMode.glValue, indexCount,
				GLES10.GL_UNSIGNED_SHORT, 0 );

		GLES11.glBindBuffer( GLES11.GL_ARRAY_BUFFER, 0 );
		GLES11.glBindBuffer( GLES11.GL_ELEMENT_ARRAY_BUFFER, 0 );

		GLUtil.checkGLError();
	}

	/***/
	public void delete()
	{
		GLUtil.checkGLError();

		final IntBuffer ib = GLUtil.intScratch( 2 );
		ib.put( 0, dataVBOID );
		ib.put( 1, indexVBOID );
		GLES11.glDeleteBuffers( 2, ib );
		dataVBOID = -1;
		indexVBOID = -1;

		GLUtil.checkGLError();
	}
}
