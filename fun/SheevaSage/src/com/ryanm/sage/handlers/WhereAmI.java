package com.ryanm.sage.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.LinkedList;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

import com.ryanm.sage.Handler;
import com.ryanm.sage.SheevaSage;

/**
 * Reports the local IP address
 * 
 * @author ryanm
 */
public class WhereAmI extends Handler
{
	@Override
	public boolean handle( final Message m, final XMPPConnection connection )
	{
		if( m.getBody().toLowerCase().equals( "where" ) )
		{
			final LinkedList<InetAddress> addrList = getNetworkAddresses();

			final StringBuilder buff = new StringBuilder( "Locally, I'm at " );

			String addr = "";

			while( !addrList.isEmpty() )
			{
				addr = addrList.removeFirst().getHostAddress();
				buff.append( addr ).append( addrList.isEmpty() ? "" : "\nor " );
			}

			SheevaSage.reply( m, buff.toString(), connection );

			final String wa = getWorldAddress();

			if( wa != null )
			{
				SheevaSage.reply( m, "Globally, I'm at " + wa, connection );
			}

			return true;
		}

		return false;
	}

	private static String getWorldAddress()
	{
		try
		{
			final URLConnection uc =
					new URL( "http://www.whatismyip.org/" ).openConnection();
			final BufferedReader br =
					new BufferedReader( new InputStreamReader( uc.getInputStream() ) );
			return br.readLine();
		}
		catch( final MalformedURLException e )
		{
			e.printStackTrace();
		}
		catch( final IOException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	private static LinkedList<InetAddress> getNetworkAddresses()
	{
		final LinkedList<InetAddress> addrList = new LinkedList<InetAddress>();

		try
		{
			final Enumeration<NetworkInterface> nis =
					NetworkInterface.getNetworkInterfaces();

			while( nis.hasMoreElements() )
			{
				final NetworkInterface ni = nis.nextElement();

				if( !ni.isLoopback() )
				{
					final Enumeration<InetAddress> addresses = ni.getInetAddresses();

					while( addresses.hasMoreElements() )
					{
						final InetAddress addr = addresses.nextElement();
						if( !addr.isLoopbackAddress() && addr instanceof Inet4Address )
						{
							addrList.add( addr );
						}
					}
				}
			}
		}
		catch( final SocketException e )
		{
			e.printStackTrace();
		}

		return addrList;
	}
}
