package com.ryanm.sage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import com.ryanm.sage.handlers.Iplayer;
import com.ryanm.sage.handlers.Quoter;
import com.ryanm.sage.handlers.Restarter;
import com.ryanm.sage.handlers.Teller;
import com.ryanm.sage.handlers.TorrentStarter;
import com.ryanm.sage.handlers.TwonkyRefresh;
import com.ryanm.sage.handlers.URLGrabber;
import com.ryanm.sage.handlers.Unrar;
import com.ryanm.sage.handlers.WhereAmI;

/**
 * A helpful ghost in the machine - part slave, part philosopher.
 * 
 * @author ryanm
 */
public class SheevaSage
{
	/**
	 * @param args
	 */
	public static void main( final String[] args )
	{
		try
		{
			final Properties p = new Properties( System.getProperties() );
			p.load( new FileInputStream( args[ 0 ] ) );
			System.setProperties( p );

			final SheevaSage sage = new SheevaSage();

			boolean connected = false;

			do
			{
				try
				{
					sage.connect();
					connected = true;
				}
				catch( final XMPPException e )
				{
					System.out.println( "Connection failed! " + e.getMessage() );
					System.out.println( "I'll try again in 10 seconds" );

					try
					{
						Thread.sleep( 10000 );
					}
					catch( final InterruptedException e1 )
					{
						e1.printStackTrace();
					}
				}
			}
			while( !connected );

			sage.go();
		}
		catch( final IOException e )
		{
			e.printStackTrace();
		}
	}

	private final String id;

	private final String password;

	private final XMPPConnection connection =
			new XMPPConnection( new ConnectionConfiguration( "talk.google.com",
					5222, "googlemail.com" ) );

	private PacketCollector collector;

	/**
	 * Written your own handler? Construct one and put it in this array. When a
	 * message is received, each handler is tried in turn with
	 * {@link Handler#handle(Message, XMPPConnection)} until a handler returns
	 * <code>true</code> from that call. Note that {@link Quoter} always returns
	 * <code>true</code>, so put your handler into the array before that one
	 */
	private final Handler[] handlers = new Handler[] { new URLGrabber(),
			new TorrentStarter(), new WhereAmI(), new Teller(), new Unrar(),
			new TwonkyRefresh(), new Iplayer(), new Restarter(), new Quoter() };

	private final PacketFilter filter = new PacketFilter(){
		@Override
		public boolean accept( final Packet packet )
		{
			if( packet instanceof Message
					&& ( ( Message ) packet ).getType() == Type.chat )
			{
				return true;
			}

			return false;
		}
	};

	private final RosterListener rl = new RosterListener(){
		@Override
		public void presenceChanged( final Presence presence )
		{
			final String from = StringUtils.parseBareAddress( presence.getFrom() );

			if( presence.isAvailable() )
			{
				for( final Handler h : handlers )
				{
					h.online( from, connection );
				}
			}
		}

		@Override
		public void entriesUpdated( final Collection<String> addresses )
		{
		}

		@Override
		public void entriesDeleted( final Collection<String> addresses )
		{
		}

		@Override
		public void entriesAdded( final Collection<String> addresses )
		{
		}
	};

	/**
	 * 
	 */
	public SheevaSage()
	{
		id = System.getProperty( "sheevasage.googleID" );
		assert id != null : "sheevasage.googleID property not set";

		password = System.getProperty( "sheevasage.googlePass" );
		assert password != null : "sheevasage.googlePass property not set";

		Runtime.getRuntime().addShutdownHook( new Thread(){
			@Override
			public void run()
			{
				disconnect();
			}
		} );

		System.out.println( "SheevaSage active handlers:" );
		for( int i = 0; i < handlers.length; i++ )
		{
			System.out.println( "\t" + handlers[ i ].getClass().getSimpleName() );
		}
	}

	/**
	 * @throws XMPPException
	 */
	public void connect() throws XMPPException
	{
		connection.connect();

		collector = connection.createPacketCollector( filter );

		connection.login( id, password );

		final Presence p = new Presence( Presence.Type.available );
		p.setStatus( "A stalwart of the grabbing industry since " + new Date() );
		connection.sendPacket( p );

		connection.getRoster().addRosterListener( rl );

		System.out.println( "Connected at " + new Date() );
	}

	/**
	 * Starts the message processing loop
	 */
	public void go()
	{
		final boolean shouldstop = false;

		while( !shouldstop )
		{
			final Packet packet = collector.nextResult();

			if( packet instanceof Message )
			{
				final Message m = ( Message ) packet;

				if( m.getBody() != null )
				{
					m.setBody( m.getBody().trim() );

					if( m.getBody().toLowerCase().equals( "status" ) )
					{
						for( int i = 0; i < handlers.length; i++ )
						{
							final String status = handlers[ i ].status();
							if( status != null )
							{
								reply( m, status, connection );
							}
						}
					}
					else
					{
						// try each handler in turn
						for( int i = 0; i < handlers.length; i++ )
						{
							if( handlers[ i ].handle( m, connection ) )
							{ // we've found the appropriate handler, stop
								// looking
								break;
							}
						}
					}
				}
			}
		}

		// disconnect();
	}

	/**
	 * 
	 */
	public void disconnect()
	{
		connection.disconnect();

		System.out.println( "Disconnected" );
	}

	/**
	 * Convenience method to reply to an incoming packet
	 * 
	 * @param in
	 * @param reply
	 * @param connection
	 */
	public static void reply( final Message in, final String reply,
			final XMPPConnection connection )
	{
		final Message m = new Message( in.getFrom(), Type.chat );
		m.setBody( reply );
		connection.sendPacket( m );
	}
}
