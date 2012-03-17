NB: I've migrated this project accross to http://code.google.com/p/sheevasage/
All future development will be happening there

Build requirements:

	JDK 1.6
	ant

Build instructions:

	In the project directory, execute "ant jar clean".
	A SheevaSage.jar file will be generated

Run requirements:

	JRE 1.6 (included with the JDK if you have that)

Run instructions:

	execute "java -jar SheevaSage.jar conf.txt", where conf.txt is a text file containing:
	
		sheevasage.googleID = mygoogleid@googlemail.com
		sheevasage.googlePass = mygooglepassword
	
		sheevasage.torrentdir = /the/path/where/torrents/download/to/
		sheevasage.iplayerdir = /the/path/where/you/want/iplayer/to/download/to
	
	you'll probably want the sage to keep running after you close the terminal window, look 
	to linux's "screen" command for this.
	
Adding capabilities:

	Write a class that implements the com.ryanm.sage.Handler interface and edit 
	com.ryanm.sage.SheevaSage.java so that an instance of your new Handler is constructed 
	and inserted in the "handlers" array. Note the javadoc on that field.

Handling URLs:

	The com.ryanm.sage.handlers.URLGrabber handler will already respond to messages just 
	containing a URL - it will examine the content-type of that URL and delegate 
	responsibility to an appropriate instance of com.ryanm.sage.handlers.URLGrabber.ContentGrabber
	If you want to handle a particular content type, write a class that extends the abstract 
	ContentGrabber superclass to do what you want, and add an instance to the URLGrabber.grabbers 
	array.

Calling other programs:

	Check out com.ryanm.sage.ProcessUtil for a convenient way to call external programs 
	and monitor their output

If you have any comments/questions/feature requests, let me know at therealryan+sage@gmail.com
Similarly, if you done something cool with it, let me know and we'll see about getting
your extensions into the svn.