package financien;

import java.net.*;
import java.io.*;
import java.util.regex.*;
import java.util.logging.*;

public class KoersenAex {
    URL urlLargeCaps = null;
    URL urlEquities = null;
    URL urlIndices = null;

    public KoersenAex( ) {
    final Logger logger = Logger.getLogger( "KoersenAex.KoersenAex" );
	try {
	    urlIndices = new URL( "http://www.aex.nl/koersen/indices" );
	}
	catch ( MalformedURLException malformedURLException ) {
	    logger.severe( "Malformed URL exception: " + malformedURLException );
	    return;
	}
    }

    public float getAhold( ) {
	return getLargeCapsKoers( "AHOLD KON" );
    }

    public float getKpn( ) {
	return getLargeCapsKoers( "KPN KON" );
    }

    public float getTnt( ) {
	return getLargeCapsKoers( "TNT" );
    }

    public float getOhraAandelenFonds( ) {
	return getEquitiesKoers( urlEquities, "OHRA AANDELEN FNDS" );
    }

    public float getOhraTechnTrendFonds( ) {
	return getEquitiesKoers( urlEquities, "OHRA TECHN TRND FD" );
    }

    public int getIndex( ) {
        final Logger logger = Logger.getLogger( "KoersenAex.getIndex" );
	final Pattern aexIndexPattern = Pattern.compile( "^AEX-INDEX;" );
	final Pattern indexWaardePattern = Pattern.compile( "^(?:[^;]*;){9}(\\d*\\.\\d*);" );

	InputStreamReader urlInputStreamReader = null;

	try {
	    urlInputStreamReader = new InputStreamReader( urlIndices.openStream( ) );
	    BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader );
	    String lineString;
	    while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		Matcher aexIndexMatcher = aexIndexPattern.matcher( lineString );
		if ( aexIndexMatcher.find( ) ) {
		    // System.out.println( "lineString: "  + lineString );
		    Matcher koersMatcher = indexWaardePattern.matcher( lineString );
		    if ( koersMatcher.find( ) ) {
			int groupCount = koersMatcher.groupCount( ) ;
			if ( groupCount > 0 ) {
			    return ( new Float( koersMatcher.group( 1 ) ) ).intValue( );
			}
			else {
			    logger.severe( "Invalid group count for AEX index" );
			    return 0;
			}
		    }
		    else {
			logger.severe( "Index value not found for AEX index" );
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

	logger.severe( "AEX index not found" );
	return 0;
    }

    private float getLargeCapsKoers( String fondsString ) {
        final Logger logger = Logger.getLogger( "KoersenAex.getLargeCapsKoers" );
	final Pattern fondsPattern = Pattern.compile( "^" + fondsString + ";" );
	final Pattern koersWaardePattern = Pattern.compile( "EUR;(\\d*\\.\\d*);" );

	InputStreamReader urlInputStreamReader = null;

	try {
	    urlInputStreamReader = new InputStreamReader( urlLargeCaps.openStream( ) );
	    BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader );
	    String lineString;
	    while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		Matcher fondsMatcher = fondsPattern.matcher( lineString );
		if ( fondsMatcher.find( ) ) {
		    // System.out.println( "lineString: "  + lineString );
		    Matcher koersMatcher = koersWaardePattern.matcher( lineString );
		    if ( koersMatcher.find( ) ) {
			int groupCount = koersMatcher.groupCount( ) ;
			if ( groupCount > 0 ) {
			    return Float.parseFloat( koersMatcher.group( 1 ) );
			}
			else {
			    logger.severe( "Invalid group count for koers for fonds " + fondsString + " in AEX Large Caps" );
			    return 0;
			}
		    }
		    else {
			logger.severe( "Koers not found for fonds " + fondsString + " in AEX Large Caps" );
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

	logger.severe( "Fonds " + fondsString + " not found in AEX Large Caps" );
	return 0;
    }

    private float getEquitiesKoers( URL url, String fondsString ) {
        final Logger logger = Logger.getLogger( "KoersenAex.getEquitiesKoers" );
	final Pattern fondsPattern = Pattern.compile( "^" + fondsString + ";" );
	final Pattern koersWaardePattern = Pattern.compile( "EUR;(\\d*\\.\\d*);" );

	InputStreamReader urlInputStreamReader = null;

	try {
	    urlInputStreamReader = new InputStreamReader( url.openStream( ) );
	    BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader );
	    String lineString;
	    while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		Matcher fondsMatcher = fondsPattern.matcher( lineString );
		if ( fondsMatcher.find( ) ) {
		    // System.out.println( "lineString: "  + lineString );
		    Matcher koersMatcher = koersWaardePattern.matcher( lineString );
		    if ( koersMatcher.find( ) ) {
			int groupCount = koersMatcher.groupCount( ) ;
			if ( groupCount > 0 ) {
			    return Float.parseFloat( koersMatcher.group( 1 ) );
			}
			else {
			    logger.severe( "Invalid group count for koers for fonds " + fondsString + " in AEX Large Caps" );
			    return 0;
			}
		    }
		    else {
			logger.severe( "Koers not found for fonds " + fondsString + " in AEX Equities" );
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

	logger.severe( "Fonds " + fondsString + " not found in AEX Equities" );
	return 0;
    }
}
