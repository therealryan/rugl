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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES10;

/**
 * Represents a chunk of image data.
 * 
 * @author ryanm
 */
public abstract class Image
{
	/**
	 * Flags for different image formats
	 * 
	 * @author ryanm
	 */
	public static enum Format
	{
		/**
		 * Uses 8 bits for each component
		 */
		RGBA( 4, GLES10.GL_RGBA ) {
			@Override
			public void write( final int pixel, final ByteBuffer buffer )
			{
				buffer.put( ( byte ) ( pixel >> 16 & 0xFF ) );
				buffer.put( ( byte ) ( pixel >> 8 & 0xFF ) );
				buffer.put( ( byte ) ( pixel >> 0 & 0xFF ) );
				buffer.put( ( byte ) ( pixel >> 24 & 0xFF ) );
			}
		},

		/**
		 * Uses 8 bits for each component.
		 */
		LUMINANCE_ALPHA( 2, GL10.GL_LUMINANCE_ALPHA ) {
			@Override
			public void write( final int pixel, final ByteBuffer buffer )
			{
				buffer.put( ( byte ) ( pixel >> 8 & 0xFF ) );
				buffer.put( ( byte ) ( pixel >> 24 & 0xFF ) );
			}
		};

		/**
		 * The number of bytes that the format uses per pixel
		 */
		public final int bytes;

		/**
		 * The OpenGL format, e.g.: GL_RGBA, GL_LUMINANCE_ALPHA
		 */
		public final int glFormat;

		private Format( final int bytes, final int glFormat )
		{
			this.bytes = bytes;
			this.glFormat = glFormat;
		}

		/**
		 * Writes the appropriate values to a buffer for a given pixel
		 * 
		 * @param argb
		 *           A pixel, in packed argb form
		 * @param buffer
		 *           The buffer to write to
		 */
		public abstract void write( int argb, ByteBuffer buffer );
	}

	/**
	 * The image format
	 */
	public final Format format;

	/**
	 * The image width
	 */
	public final int width;

	/**
	 * The image height
	 */
	public final int height;

	/**
	 * @param width
	 * @param height
	 * @param format
	 */
	protected Image( final int width, final int height, final Format format )
	{
		this.width = width;
		this.height = height;
		this.format = format;
	}

	/**
	 * Reads an image from a stream
	 * 
	 * @param is
	 * @throws IOException
	 */
	protected Image( final InputStream is ) throws IOException
	{
		final DataInputStream dis = new DataInputStream( is );

		width = dis.readInt();
		height = dis.readInt();

		final int formatOrdinal = dis.readInt();
		assert formatOrdinal >= 0 && formatOrdinal < Format.values().length : "Format with ordinal "
				+ formatOrdinal + " not found";
		format = Format.values()[ formatOrdinal ];
	}

	/**
	 * Reads an image from a file
	 * 
	 * @param fileName
	 * @return An {@link Image}
	 * @throws IOException
	 */
	public static Image loadImage( final String fileName ) throws IOException
	{
		final RandomAccessFile raf = new RandomAccessFile( fileName, "r" );
		final FileChannel ch = raf.getChannel();
		final MappedByteBuffer buffer =
				ch.map( FileChannel.MapMode.READ_ONLY, 0, raf.length() );

		final Image i = new BufferImage( buffer );

		ch.close();

		return i;
	}

	/**
	 * Write the image to a texture
	 * 
	 * @param x
	 *           the x-coord of where to insert
	 * @param y
	 *           The y-coord of where to insert
	 */
	public abstract void writeToTexture( int x, int y );

	@Override
	public String toString()
	{
		return "Image " + format + " " + width + "x" + height;
	}
}
