package financien;

import java.net.*;
import java.io.*;
import java.util.regex.*;
import java.util.logging.*;

public class KoersenPostbank {
    URL urlBeschermdBeleggen = null;

    public KoersenPostbank( ) {
    final Logger logger = Logger.getLogger( "KoersenPostbank.KoersenPostbank" );
	try {
	    // urlBeschermdBeleggen = new URL( "http://www.postbank.nl/ing/pp/page/stockexchange/quotes/index/doel/0,2836,1859_271362,00.html" );
	    urlBeschermdBeleggen = new URL( "http://www.postbank.nl/ing/pp/page/stockexchange/quotes/index/generallist/0,11567,1859_271362,00.html" );
	}
	catch ( MalformedURLException malformedURLException ) {
	    logger.severe( "Malformed URL exception: " + malformedURLException );
	    return;
	}
    }

    public float getAexClicker( ) {
	return getBeschermdBeleggenKoers( "aex clicker" );
    }

    public float getChinaClicker( ) {
	return getBeschermdBeleggenKoers( "china clicker" );
    }

    private float getBeschermdBeleggenKoers( String fondsString ) {
        final Logger logger = Logger.getLogger( "KoersenPostbank.getBeschermdBeleggenKoers" );
	final Pattern fondsPattern = Pattern.compile( fondsString );
	final Pattern koersPattern = Pattern.compile( ">(\\d*,\\d*)<" );

	InputStreamReader urlInputStreamReader = null;

	try {
	    urlInputStreamReader = new InputStreamReader( urlBeschermdBeleggen.openStream( ) );
	    BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader );
	    String lineString;
	    while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		Matcher fondsMatcher = fondsPattern.matcher( lineString );
		if ( fondsMatcher.find( ) ) {
		    // Read the next line, which should contain the closing stock rate
		    lineString = bufferedReader.readLine( );
		    Matcher koersMatcher = koersPattern.matcher( lineString );
		    if ( koersMatcher.find( ) ) {
			int groupCount = koersMatcher.groupCount( ) ;
			if ( groupCount > 0 ) {
			    return Float.parseFloat( koersMatcher.group( 1 ).replace( ',', '.' ) );
			}
			else {
			    logger.severe( "Invalid group count for koers for fonds " + fondsString + " in Postbank Beschermd Beleggen" );
			    return 0;
			}
		    }
		    else {
			logger.severe( "Koers not found for fonds " + fondsString + " in Postbank Beschermd Beleggen" );
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

	logger.severe( "Fonds " + fondsString + " not found in Postbank Beschermd Beleggen" );
	return 0;
    }
}
