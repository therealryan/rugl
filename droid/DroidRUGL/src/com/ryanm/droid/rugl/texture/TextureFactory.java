/*
 * Copyright (c) 2007, Ryan McNally All rights reserved. Redistribution and use
 * in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. Neither
 * the name of the <ORGANIZATION> nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS
 * AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ryanm.droid.rugl.texture;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES10;
import android.opengl.GLES11;
import android.opengl.GLException;
import android.util.Log;

import com.ryanm.droid.rugl.Game;
import com.ryanm.droid.rugl.Game.SurfaceListener;
import com.ryanm.droid.rugl.gl.BufferUtils;
import com.ryanm.droid.rugl.gl.GLUtil;
import com.ryanm.droid.rugl.gl.State;
import com.ryanm.droid.rugl.texture.Image.Format;
import com.ryanm.droid.rugl.util.RectanglePacker;
import com.ryanm.droid.rugl.util.geom.Vector2f;

/**
 * Builds and manages {@link Texture}s.
 * 
 * @author ryanm
 */
public class TextureFactory
{
	static
	{
		Game.addSurfaceLIstener( new SurfaceListener(){
			@Override
			public void onSurfaceCreated()
			{
				recreateTextures();
			}
		} );
	}

	/**
	 * A list of OpenGL textures
	 */
	private static List<GLTexture> textures = new LinkedList<GLTexture>();

	/**
	 * The default dimension of the OpenGL textures
	 */
	private static int textureDimension = 1024;

	/**
	 * Called in response to the opengl context going away, perhaps as a result
	 * of the display mode changing
	 */
	private static void recreateTextures()
	{
		if( !textures.isEmpty() )
		{
			final IntBuffer texNames =
					BufferUtils.createIntBuffer( textures.size() );
			for( final GLTexture glt : textures )
			{
				texNames.put( glt.id );
			}
			texNames.flip();

			GLES10.glDeleteTextures( textures.size(), texNames );

			for( final GLTexture glt : textures )
			{
				glt.recreate();
			}
		}

		GLUtil.checkGLError();
	}

	/**
	 * Gets the current list of textures
	 * 
	 * @return an unmodifiable list of {@link GLTexture} objects
	 */
	public static List<GLTexture> getTextures()
	{
		return Collections.unmodifiableList( textures );
	}

	/**
	 * Loads the specified image into OpenGL. Does not specify which texture the
	 * image will reside on, and will create new OpenGL textures as needed
	 * 
	 * @param image
	 *           the texture image
	 * @param lonesome
	 *           <code>true</code> if this texture is to be allocated in a gl
	 *           texture of minimal size, <code>false</code> for the default size
	 * @param mipmap
	 *           <code>true</code> to generate mipmaps, false otherwise
	 * @return A {@link Texture} object, or null if it was not able to be
	 *         constructed
	 */
	public static Texture buildTexture( final Image image,
			final boolean lonesome, final boolean mipmap )
	{
		if( lonesome )
		{
			try
			{
				final GLTexture parent =
						new GLTexture( image.width, image.height, image.format,
								mipmap, lonesome ? 0 : 1 );

				textures.add( parent );
				final Texture texture = parent.addImage( image );

				Log.i( Game.RUGL_TAG, "Texture uploaded " + texture + " to "
						+ parent );

				return texture;
			}
			catch( final GLException e )
			{
				Log.e( "RUGL", "Problem creating texture", e );
				return null;
			}
		}
		else
		{
			for( final GLTexture tex : textures )
			{
				// match mipmap params
				if( tex.mipmap == mipmap )
				{
					// try to insert
					final Texture t = tex.addImage( image );

					if( t != null )
					{ // it worked!
						return t;
					}
					// else there was no room
				}
			}

			// build a new texture
			try
			{
				final GLTexture parent =
						new GLTexture( textureDimension, textureDimension,
								image.format, mipmap, 1 );
				textures.add( parent );
				return parent.addImage( image );
			}
			catch( final GLException e )
			{ // we were not able to build the texture
				Log.e( "RUGL", "Problem creating texture", e );
				return null;
			}
		}
	}

	/**
	 * Requests that a new texture be built
	 * 
	 * @param minWidth
	 * @param minHeight
	 * @param format
	 * @param mipmap
	 *           <code>true</code> to build mipmaps, <code>false</code> otherwise
	 * @param border
	 *           The border, in pixels, that will be placed around images added
	 *           to this texture
	 * @return A dummy {@link Texture} object
	 */
	public static GLTexture createTexture( final int minWidth,
			final int minHeight, final Format format, final boolean mipmap,
			final int border )
	{
		try
		{
			final GLTexture parent =
					new GLTexture( minWidth, minHeight, format, mipmap, border );
			textures.add( parent );

			return parent;
		}
		catch( final GLException e )
		{ // we were not able to build the texture
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Frees up the space allocated to the supplied texture
	 * 
	 * @param t
	 *           The texture to delete
	 * @return <code>true</code> if the texture was released, <code>false</code>
	 *         if it could not be found
	 */
	public static boolean deleteTexture( final Texture t )
	{
		for( final GLTexture tex : textures )
		{
			if( tex.release( t ) )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Clears the state of all textures. Called in response to the OpenGL context
	 * being recreated. Does not release OpenGL resources
	 */
	public static void clear()
	{
		textures.clear();
	}

	/**
	 * Encapsulates an OpenGL texture object
	 * 
	 * @author ryanm
	 */
	public static class GLTexture
	{
		/**
		 * The texture id
		 */
		private int id;

		/**
		 * The texture size
		 */
		public final int width;

		/**
		 * The texture size
		 */
		public final int height;

		/**
		 * Texture data format
		 */
		public final Format format;

		/**
		 * If mipmaps are built for the texture
		 */
		public final boolean mipmap;

		private final List<Texture> residentTextures = new LinkedList<Texture>();

		private final RectanglePacker<Image> packer;

		private Texture pan = null;

		/**
		 * Builds a new GLTexture
		 * 
		 * @param width
		 * @param height
		 * @param mipmap
		 *           Whether to mipmap or not
		 * @param border
		 */
		private GLTexture( final int width, final int height,
				final Format format, final boolean mipmap, final int border )
		{
			if( width < 1 || height < 1 )
			{
				this.width = textureDimension;
				this.height = textureDimension;
			}
			else
			{
				this.width = GLUtil.nextPowerOf2( width );
				this.height = GLUtil.nextPowerOf2( height );
			}

			this.format = format;
			this.mipmap = mipmap;

			packer = new RectanglePacker<Image>( width, height, border );

			recreate();
		}

		private void recreate()
		{
			final int[] ib = new int[1];
			GLES10.glGenTextures( 1, ib, 0 );
			id = ib[ 0 ];

			State.getCurrentState().withTexture( id ).apply();

			final ByteBuffer data =
					BufferUtils.createByteBuffer( width * height * format.bytes );

			GLES10.glPixelStorei( GLES10.GL_UNPACK_ALIGNMENT, format.bytes );

			if( mipmap )
			{
				GLES10.glTexParameterf( id, GLES11.GL_GENERATE_MIPMAP,
						GLES10.GL_TRUE );
			}

			GLES10.glTexImage2D( GL10.GL_TEXTURE_2D, 0, format.glFormat, width,
					height, 0, format.glFormat, GL10.GL_UNSIGNED_BYTE, data );

			for( final Texture t : residentTextures )
			{
				final RectanglePacker.Rectangle rpr =
						packer.findRectangle( t.sourceImage );

				if( rpr != null )
				{
					writeToTexture( rpr, t.sourceImage );
				}
			}

			GLUtil.checkGLError();
		}

		/**
		 * Gets the texture ID
		 * 
		 * @return the opengl texture name
		 */
		public int id()
		{
			return id;
		}

		/**
		 * Gets the resident {@link Texture} objects
		 * 
		 * @return an unmodifiable list of the resident {@link Texture} objects
		 */
		public List<Texture> getResidents()
		{
			return Collections.unmodifiableList( residentTextures );
		}

		/**
		 * Gets the {@link Texture} that displays the whole of this
		 * {@link GLTexture}
		 * 
		 * @return A {@link Texture} that encompasses the entirety of this
		 *         {@link GLTexture}
		 */
		public Texture getTexture()
		{
			if( pan == null )
			{
				pan = new Texture( this );
			}

			return pan;
		}

		/**
		 * Attempts to add an image to this {@link GLTexture}
		 * 
		 * @param image
		 *           The {@link Image} to add
		 * @return The resulting {@link Texture}, or <code>null</code> if it
		 *         didn't fit
		 */
		public Texture addImage( final Image image )
		{
			final RectanglePacker.Rectangle rpr =
					packer.insert( image.width, image.height, image );

			if( rpr != null )
			{
				writeToTexture( rpr, image );

				final Vector2f bottomleft = new Vector2f( rpr.x, rpr.y );
				final Vector2f topRight =
						new Vector2f( rpr.x + rpr.width, rpr.y + rpr.height );

				bottomleft.x /= width;
				bottomleft.y /= height;
				topRight.x /= width;
				topRight.y /= height;

				final Texture t = new Texture( this, bottomleft, topRight, image );
				residentTextures.add( t );

				return t;
			}
			else
			{
				return null;
			}
		}

		/**
		 * Releases the space reserved for the supplied texture. Note that the
		 * data will still be resident on this {@link GLTexture} until it is
		 * overwritten or the mipmap is regenerated
		 * 
		 * @param t
		 *           The {@link Texture} to release
		 * @return <code>true</code> if the texture was released,
		 *         <code>false</code> if was not resident on this
		 *         {@link GLTexture}
		 */
		private boolean release( final Texture t )
		{
			if( residentTextures.remove( t ) )
			{
				packer.remove( t.sourceImage );
				return true;
			}
			else
			{
				return false;
			}
		}

		private void writeToTexture( final RectanglePacker.Rectangle r,
				final Image image )
		{
			State.getCurrentState().withTexture( id ).apply();

			image.writeToTexture( r.x, r.y );

			GLUtil.checkGLError();
		}

		@Override
		public String toString()
		{
			final StringBuilder buff = new StringBuilder( "GLTexture id = " );
			buff.append( id );
			buff.append( " format = " );
			buff.append( format );
			buff.append( " mimap = " );
			buff.append( mipmap );
			buff.append( " size = [" );
			buff.append( width );
			buff.append( "," );
			buff.append( height );
			buff.append( "]" );
			buff.append( " residents: " );
			buff.append( residentTextures.size() );

			if( residentTextures.size() < 10 )
			{
				for( final Texture t : residentTextures )
				{
					buff.append( "\n\t" );
					buff.append( t.toString() );
				}
			}

			return buff.toString();
		}
	}

	/**
	 * Describes the current state of the {@link TextureFactory}
	 * 
	 * @return the current state
	 */
	public static String getStateString()
	{
		final StringBuilder buff = new StringBuilder( "TextureFactory state" );
		buff.append( "\n\t" ).append( textures.size() )
				.append( " GL texture objects" );

		for( final GLTexture glt : textures )
		{
			buff.append( "\n" ).append( glt.toString() );
		}

		return buff.toString();
	}
}
