
package com.ryanm.sage.handlers;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

import com.ryanm.sage.Handler;
import com.ryanm.sage.ProcessUtil;
import com.ryanm.sage.ProcessUtil.Listener;
import com.ryanm.sage.SheevaSage;

/**
 * Searches the torrent directory tree and unrars anything it finds in
 * there
 * 
 * @author ryanm
 */
public class Unrar extends Handler
{
	private static final FileFilter dirFilter = new FileFilter() {
		@Override
		public boolean accept( File pathname )
		{
			return pathname.isDirectory();
		}
	};

	private static final FilenameFilter rarFilter = new FilenameFilter() {
		@Override
		public boolean accept( File dir, String name )
		{
			return name.toLowerCase().contains( ".rar" );
		}
	};

	private Map<String, String> status = new TreeMap<String, String>();

	@Override
	public boolean handle( final Message m, final XMPPConnection connection )
	{
		if( m.getBody().toLowerCase().equals( "unrar" ) )
		{
			SheevaSage.reply( m, "Rar-ed torrents eh? Let me get that for you.", connection );
			String tdirName = System.getProperty( "sheevasage.torrentdir" );
			boolean found = false;
			if( tdirName == null )
			{
				SheevaSage.reply( m, "I don't know where the torrents live!", connection );
			}
			else
			{
				File tdir = new File( tdirName );

				for( final File dir : tdir.listFiles( dirFilter ) )
				{
					final File[] rars = dir.listFiles( rarFilter );

					if( rars.length > 0 )
					{
						found = true;
						SheevaSage.reply( m, "I've found some rars in " + dir.getName()
								+ "\nthis may take a little while.", connection );

						Thread t = new Thread( "unraring " + dir.getName() ) {
							@Override
							public void run()
							{
								Listener l = new Listener() {
									@Override
									public void line( String line )
									{
										if( line != null )
										{
											int i = line.lastIndexOf( "%" );
											if( i >= 3 )
											{
												status.put( dir.getName(), line.substring( i - 3, i + 1 ) );
											}
										}
									}
								};

								try
								{
									ProcessUtil.execute( true, l, dir, "unrar", "e", rars[ 0 ].getName() );

									TwonkyRefresh.refresh();
								}
								catch( IOException e )
								{
									e.printStackTrace();
									SheevaSage.reply( m, "Something went wrong there :-(", connection );
								}
							}
						};
						t.start();
					}
				}
			}

			if( !found )
			{
				SheevaSage.reply( m, "I can't find any rars. Are you playing a trick on me?",
						connection );
			}

			return true;
		}

		return false;
	}

	@Override
	public String status()
	{
		if( !status.isEmpty() )
		{
			StringBuilder buff = new StringBuilder( "Unraring\n" );
			for( String dir : status.keySet() )
			{
				buff.append( dir );
				buff.append( "\n\t\t" );
				buff.append( status.get( dir ) );
				buff.append( "% done\n" );
			}

			status.clear();
			return buff.toString();
		}

		return null;
	}
}
