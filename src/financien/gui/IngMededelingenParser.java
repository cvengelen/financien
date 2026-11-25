package financien.gui;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IngMededelingenParser {
	private final String[ ] mutatieMededelingenSubString = new String[ 4 ];
	private final String mutatieMededelingenStrippedString;
	private final int nMatches;

	public IngMededelingenParser( final String mutatieMededelingenString ) {
		final Logger logger = Logger.getLogger( "financien.gui.IngMededelingenParser" );
		logger.fine( "mutatieMededelingenString : " + mutatieMededelingenString );

		// Mededelingen met naam, omschrijving, IBAN (soms), kenmerk, doorlopende incasso (soms), en valutadatum
		// Naam, omschrijving, (IBAN)?, kenmerk, (Doorlopende incasso)?, (Valutadatum)?
		// Skip het IBAN, sla naam, omschrijving en kenmerk op
		Pattern mutatieMededelingenPattern = Pattern.compile( ".*Naam:(.*?)Omschrijving:(.*?)(?:IBAN:.*?)?(Kenmerk:.*?)(?:Doorlopende incasso )?(?:Valutadatum:.*?)?" );
		Matcher mutatieMededelingenMatcher = mutatieMededelingenPattern.matcher( mutatieMededelingenString );
		logger.finer( "mutatieMededelingenMatcher: " + mutatieMededelingenMatcher + ", count:" + mutatieMededelingenMatcher.groupCount( ) );
		if ( mutatieMededelingenMatcher.matches( ) ) {
			nMatches = 3;
			mutatieMededelingenSubString[ 0 ] = mutatieMededelingenMatcher.group( 1 ).trim( );	// Naam
			mutatieMededelingenSubString[ 1 ] = mutatieMededelingenMatcher.group( 2 ).trim( );	// Omschrijving
			mutatieMededelingenSubString[ 2 ] = mutatieMededelingenMatcher.group( 3 ).trim( );	// Kenmerk
			mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 1 ] + "; " + mutatieMededelingenSubString[ 2 ];
			logger.fine( "match 1, naam: " + mutatieMededelingenSubString[ 0 ] + " rest: " + mutatieMededelingenStrippedString );
			return;
		}

		// Mededelingen met naam, omschrijving, IBAN (soms), doorlopende incasso (soms), en valutadatum
		// Naam, omschrijving, (IBAN)?, (Doorlopende incasso)?, (Valutadatum)?
		// Skip het IBAN, sla enkel naam en omschrijving op
		mutatieMededelingenPattern = Pattern.compile( ".*Naam:(.*?)Omschrijving:(.*?)(?:IBAN:.*?)?(?:Doorlopende incasso )?(?:Valutadatum:.*?)" );
		mutatieMededelingenMatcher = mutatieMededelingenPattern.matcher( mutatieMededelingenString );
		logger.finer( "mutatieMededelingenMatcher: " + mutatieMededelingenMatcher + ", count:" + mutatieMededelingenMatcher.groupCount( ) );
		if ( mutatieMededelingenMatcher.matches( ) ) {
			nMatches = 2;
			mutatieMededelingenSubString[ 0 ] = mutatieMededelingenMatcher.group( 1 ).trim( );	// Naam
			mutatieMededelingenSubString[ 1 ] = mutatieMededelingenMatcher.group( 2 ).trim( );	// Omschrijving
			mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 1 ];
			logger.fine( "match 2, naam: " + mutatieMededelingenSubString[ 0 ] + " rest: " + mutatieMededelingenStrippedString );
			return;
		}

		// Mededelingen met naam, kenmerk, omschrijving, IBAN (soms), doorlopende incasso (soms), en valutadatum
		// Naam, kenmerk, omschrijving, (IBAN)?, (Doorlopende incasso)?, (Valutadatum)?
		// Skip het IBAN, sla naam, kenmerk (hier groep 2) en omschrijving (hier groep 3) op
		mutatieMededelingenPattern = Pattern.compile( ".*Naam:(.*?)(Kenmerk:.*?)Omschrijving:(.*?)(?:IBAN:.*?)?(?:Doorlopende incasso )?(?:Valutadatum:.*?)" );
		mutatieMededelingenMatcher = mutatieMededelingenPattern.matcher( mutatieMededelingenString );
		logger.finer( "mutatieMededelingenMatcher: " + mutatieMededelingenMatcher + ", count:" + mutatieMededelingenMatcher.groupCount( ) );
		if ( mutatieMededelingenMatcher.matches( ) ) {
			nMatches = 3;
			mutatieMededelingenSubString[ 0 ] = mutatieMededelingenMatcher.group( 1 ).trim( );	// Naam
			mutatieMededelingenSubString[ 1 ] = mutatieMededelingenMatcher.group( 3 ).trim( );	// Omschrijving
			mutatieMededelingenSubString[ 2 ] = mutatieMededelingenMatcher.group( 2 ).trim( );	// Kenmerk
			mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 1 ] + "; " + mutatieMededelingenSubString[ 2 ];
			logger.fine( "match 3, naam: " + mutatieMededelingenSubString[ 0 ] + " rest: " + mutatieMededelingenStrippedString );
			return;
		}

		// Geen Naam of omschrijving: betaling met kaart
		// Sla alles voor de valutadatum op
		mutatieMededelingenPattern = Pattern.compile( "(.*?)(?:Valutadatum:.*?)" );
		mutatieMededelingenMatcher = mutatieMededelingenPattern.matcher( mutatieMededelingenString );
		logger.finer( "mutatieMededelingenMatcher: " + mutatieMededelingenMatcher + ", count:" + mutatieMededelingenMatcher.groupCount( ) );
		if ( mutatieMededelingenMatcher.matches( ) ) {
			nMatches = 1;
			mutatieMededelingenSubString[ 0 ] = mutatieMededelingenMatcher.group( 1 ).trim( );	// Alles behalve de valutadatum
			mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 0 ];
			logger.fine( "match 4: " + mutatieMededelingenStrippedString );
			return;
		}

		// No match found: return the complete mededelingen
		nMatches = 1;
		mutatieMededelingenSubString[ 0 ] = mutatieMededelingenString.trim();
		mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 0 ];
		logger.fine( "geen match: " + mutatieMededelingenStrippedString );
	}

	public String getMutatieMededelingenSubString(int mutatieMededelingenSubStringIndex ) {
		return mutatieMededelingenSubString[ mutatieMededelingenSubStringIndex ];
	}

	public String getMutatieMededelingenStrippedString() {
		return mutatieMededelingenStrippedString;
	}

	public int getNMatches() {
		return nMatches;
	}
}
