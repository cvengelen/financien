package financien;

import java.net.*;
import java.io.*;
import java.util.regex.*;
import java.util.logging.*;

public class KoersenRobeco {
    public KoersenRobeco( ) {
	final Logger logger = Logger.getLogger( "KoersenRobeco.KoersenRobeco" );
	// Empty constructor
    }

    public float getBalancedMix( ) {
	return getKoers( "http://www.robeco.com/extranet/f4i/cupido/quotes.mvc?pfndid=899&pdstchn=robecocom&plang=nld" );
    }

    private float getKoers( String urlString ) {
        final Logger logger = Logger.getLogger( "KoersenRobeco.getKoers" );

	URL url = null;

	try {
	    url = new URL( urlString );
	}
	catch ( MalformedURLException malformedURLException ) {
	    logger.severe( "Malformed URL exception: " + malformedURLException );
	    return 0;
	}

	final Pattern laatstePattern = Pattern.compile( ">Laatste<" );
	final Pattern koersPattern = Pattern.compile( ">(\\d*.\\d*)<" );
	InputStreamReader urlInputStreamReader = null;

	try {
	    urlInputStreamReader = new InputStreamReader( url.openStream( ) );
	    BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader );
	    String lineString;
	    while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		// Find line with "Laatste"
		Matcher laatsteMatcher = laatstePattern.matcher( lineString );
		if ( laatsteMatcher.find( ) ) {
		    // Read the next third line, which should contain the closing stock rate
		    lineString = bufferedReader.readLine( );
		    lineString = bufferedReader.readLine( );
		    lineString = bufferedReader.readLine( );
		    Matcher koersMatcher = koersPattern.matcher( lineString );
		    if ( koersMatcher.find( ) ) {
			int groupCount = koersMatcher.groupCount( ) ;
			if ( groupCount > 0 ) {
			    return Float.parseFloat( koersMatcher.group( 1 ) );
			}
			else {
			    logger.severe( "Invalid group count" );
			    return 0;
			}
		    }
		    else {
			logger.severe( "Koers not found" );
			return 0;
		    }
		}
	    }
	}
	catch ( IOException ioException ) {
	    logger.severe( "IO exception: " + ioException );
	}
	finally {
	    try {
		urlInputStreamReader.close( );
	    }
	    catch ( IOException ioException ) {
		logger.severe( "IO exception: " + ioException );
	    }
	}

	logger.severe( "Laatste koers not found" );
	return 0;
    }
}
