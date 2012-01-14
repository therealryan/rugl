package com.ryanm.sage.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

import com.ryanm.sage.Handler;
import com.ryanm.sage.SheevaSage;

/**
 * Refreshes twonky's content DB
 * 
 * @author ryanm
 */
public class TwonkyRefresh extends Handler
{
	@Override
	public boolean handle( final Message m, final XMPPConnection connection )
	{
		if( m.getBody().toLowerCase().startsWith( "refresh" ) )
		{
			try
			{
				refresh();
				SheevaSage.reply( m, "It's rescanning now, try again in a minute",
						connection );
			}
			catch( final IOException e )
			{
				e.printStackTrace();
				SheevaSage.reply( m,
						"Something went a bit wonky there : " + e.getMessage(),
						connection );
			}

			return true;
		}
		return false;
	}

	/**
	 * @throws IOException
	 */
	public static void refresh() throws IOException
	{
		final URL url = new URL( "http://127.0.0.1:9000/rpc/rescan" );
		final URLConnection con = url.openConnection();

		final StringBuilder sb = new StringBuilder();
		final BufferedReader br =
				new BufferedReader( new InputStreamReader( con.getInputStream() ) );
		String line;
		while( ( line = br.readLine() ) != null )
		{
			sb.append( line ).append( "\n" );
		}

		br.close();

		System.out.println( sb );
	}
}
