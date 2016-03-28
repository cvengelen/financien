package financien;

import java.net.*;
import java.io.*;
import java.util.regex.*;
import java.util.logging.*;

public class KoersenUsd {
    public KoersenUsd( ) {
	final Logger logger = Logger.getLogger( "KoersenUsd.KoersenUsd" );
	// Empty constructor
    }

    public float getKoers( ) {
        final Logger logger = Logger.getLogger( "KoersenUsd.getKoers" );

	URL url = null;
	URL tekstUrl = null;
	final Pattern koersPattern = Pattern.compile( ">Amerikaanse dollar.*>(\\d*,\\d*)" );

	try {
	    url = new URL( "http://teletekst.nos.nl/?543-01.html" );
	    // Tekst versie van NOS teletekst
	    tekstUrl = new URL( "http://teletekst.nos.nl/tekst/543-01.html" );
	}
	catch ( MalformedURLException malformedURLException ) {
	    logger.severe( "Malformed URL exception: " + malformedURLException );
	    return 0;
	}

	try {
	    InputStreamReader urlInputStreamReader = null;
	    urlInputStreamReader = new InputStreamReader( url.openStream( ) );
	}
	catch ( IOException ioException ) {
	    logger.severe( "IO exception: " + ioException );
	    return 0;
	}

	try {
	    InputStreamReader urlInputStreamReader = null;
	    urlInputStreamReader = new InputStreamReader( tekstUrl.openStream( ) );
	    BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader );
	    String lineString;
	    while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		// Find line with USD koers
		Matcher koersMatcher = koersPattern.matcher( lineString );
		if ( koersMatcher.find( ) ) {
		    int groupCount = koersMatcher.groupCount( ) ;
		    if ( groupCount > 0 ) {
			return Float.parseFloat( koersMatcher.group( 1 ).replace( ',', '.' ) );
		    }
		    else {
			logger.severe( "Invalid group count" );
			return 0;
		    }
		}
	    }
	}
	catch ( IOException ioException ) {
	    logger.severe( "IO exception: " + ioException );
	}

	logger.severe( "USD koers not found" );
	return 0;
    }
}
