
package com.rugl.text;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.util.WritableDimension;
import org.lwjgl.util.vector.Vector2f;

import com.rugl.texture.Image;
import com.rugl.texture.Texture;
import com.rugl.texture.TextureFactory.GLTexture;
import com.ryanm.util.geom.WritableDimensionf;

/**
 * Holds the glyph image data. Multiple characters may map to the same
 * image e.g.: in a font with only upper-case characters
 * 
 * @author ryanm
 */
public class GlyphImage
{
	/**
	 * The glyph image
	 */
	public final Image image;

	/**
	 * Array of characters that this glyph can represent
	 */
	public final char[] characters;

	/**
	 * The glyph texture
	 */
	private transient Texture texture;

	/**
	 * Texture coordinates for the origin of this glyph
	 */
	private transient Vector2f texOrigin = new Vector2f();

	/**
	 * Texture coordinates for the other corner of this glyph
	 */
	private transient Vector2f texExtent = new Vector2f();

	/**
	 * @param image
	 * @param characters
	 */
	public GlyphImage( Image image, char... characters )
	{
		this.image = image;
		this.characters = characters;
	}

	/**
	 * @param is
	 * @throws IOException
	 */
	public GlyphImage( InputStream is ) throws IOException
	{
		DataInputStream dis = new DataInputStream( is );
		characters = new char[ dis.readInt() ];
		for( int i = 0; i < characters.length; i++ )
		{
			characters[ i ] = dis.readChar();
		}

		image = new Image( is );
	}

	/**
	 * @param data
	 */
	public GlyphImage( ByteBuffer data )
	{
		characters = new char[ data.getInt() ];
		for( int i = 0; i < characters.length; i++ )
		{
			characters[ i ] = data.getChar();
		}

		image = new Image( data );
	}

	void write( ByteBuffer data )
	{
		data.putInt( characters.length );
		for( int i = 0; i < characters.length; i++ )
		{
			data.putChar( characters[ i ] );
		}

		image.write( data );
	}

	/**
	 * Tests if this {@link GlyphImage} can represent a given character
	 * 
	 * @param c
	 * @return <code>true</code> if this {@link GlyphImage} can be used
	 *         to draw c, <code>false</code> otherwise
	 */
	public boolean represents( char c )
	{
		for( int i = 0; i < characters.length; i++ )
		{
			if( c == characters[ i ] )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * If necessary, uploads the texture to OpenGL and builds the
	 * texCoords
	 * 
	 * @param fontTexture
	 *           the font texture
	 * @return <code>true</code> if successful, <code>false</code>
	 *         otherwise
	 */
	public boolean init( GLTexture fontTexture )
	{
		if( texture == null )
		{
			texture = fontTexture.addImage( image );

			if( texture != null )
			{
				texOrigin.set( 0, 0 );
				texExtent.set( 1, 1 );
				texOrigin = texture.getTexCoords( texOrigin, texOrigin );
				texExtent = texture.getTexCoords( texExtent, texExtent );
			}
		}

		return texture != null;
	}

	/**
	 * Gets the texture coordinate of this glyph's origin
	 * 
	 * @param dest
	 *           A {@link Vector2f} in which to store the value, or
	 *           null
	 * @return dest, or a new {@link Vector2f}
	 */
	public Vector2f getOrigin( Vector2f dest )
	{
		if( dest == null )
		{
			dest = new Vector2f();
		}
		dest.set( texOrigin );
		return dest;
	}

	/**
	 * Gets the texture coordinate of this glyph's extent - the
	 * opposite corner of the glyph from the origin
	 * 
	 * @param dest
	 *           A {@link Vector2f} in which to store the value, or
	 *           null
	 * @return dest, or a new {@link Vector2f}
	 */
	public Vector2f getExtent( Vector2f dest )
	{
		if( dest == null )
		{
			dest = new Vector2f();
		}
		dest.set( texExtent );
		return dest;
	}

	/**
	 * Gets the size of this glyph, in pixels
	 * 
	 * @param dest
	 *           A {@link WritableDimension} to store ths size in
	 */
	public void getSize( WritableDimensionf dest )
	{
		dest.setSize( image.getWidth(), image.getHeight() );
	}

	/**
	 * @return number of bytes needed to store this {@link GlyphImage}
	 */
	public int dataSize()
	{
		return image.dataSize() + 4 + characters.length * 2;
	}
}
