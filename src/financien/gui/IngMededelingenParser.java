package financien.gui;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IngMededelingenParser {
    private final Logger logger = Logger.getLogger( "financien.gui.IngMededelingenParser" );

    private final String[ ] mutatieMededelingenSubString = new String[ 4 ];
    private String mutatieMededelingenStrippedString;
    private int nMatches;

    public IngMededelingenParser( final String mutatieMededelingenString ) {
        logger.fine( "mutatieMededelingenString : " + mutatieMededelingenString );

        // Create a Pattern object
        Pattern mutatieMededelingenPattern = Pattern.compile( ".*Naam:(.*?)Omschrijving:(.*?)Kenmerk:(.*?)(?:IBAN|BIC|Mandaat|Crediteur).*" );
        Matcher mutatieMededelingenMatcher = mutatieMededelingenPattern.matcher( mutatieMededelingenString );
        // logger.finer( "mutatieMededelingenMatcher: " + mutatieMededelingenMatcher + ", count:" + mutatieMededelingenMatcher.groupCount( ) );
        if ( mutatieMededelingenMatcher.matches( ) ) {
            nMatches = 3;
            mutatieMededelingenSubString[ 0 ] = mutatieMededelingenMatcher.group( 1 ).trim( );
            mutatieMededelingenSubString[ 1 ] = mutatieMededelingenMatcher.group( 2 ).trim( );
            mutatieMededelingenSubString[ 2 ] = mutatieMededelingenMatcher.group( 3 ).trim( );
            mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 1 ] + "; Kenmerk: " + mutatieMededelingenSubString[ 2 ];
            logger.fine( "match 1, naam: " + mutatieMededelingenSubString[ 0 ] + " rest: " + mutatieMededelingenStrippedString );
            return;
        }

        mutatieMededelingenPattern = Pattern.compile( ".*Naam:(.*?)Omschrijving:(.*?)Kenmerk:(.*)" );
        mutatieMededelingenMatcher = mutatieMededelingenPattern.matcher( mutatieMededelingenString );
        // logger.finer( "mutatieMededelingenMatcher: " + mutatieMededelingenMatcher + ", count:" + mutatieMededelingenMatcher.groupCount( ) );
        if ( mutatieMededelingenMatcher.matches( ) ) {
            nMatches = 3;
            mutatieMededelingenSubString[ 0 ] = mutatieMededelingenMatcher.group( 1 ).trim( );
            mutatieMededelingenSubString[ 1 ] = mutatieMededelingenMatcher.group( 2 ).trim( );
            mutatieMededelingenSubString[ 2 ] = mutatieMededelingenMatcher.group( 3 ).trim( );
            mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 1 ] + "; Kenmerk: " + mutatieMededelingenSubString[ 2 ];
            logger.fine( "match 2, naam: " + mutatieMededelingenSubString[ 0 ] + " rest: " + mutatieMededelingenStrippedString );
            return;
        }

        mutatieMededelingenPattern = Pattern.compile( ".*Naam:(.*?)Kenmerk:(.*?)Omschrijving:(.*?)(?:IBAN|BIC|Mandaat|Crediteur).*" );
        mutatieMededelingenMatcher = mutatieMededelingenPattern.matcher( mutatieMededelingenString );
        // logger.finer( "mutatieMededelingenMatcher: " + mutatieMededelingenMatcher + ", count:" + mutatieMededelingenMatcher.groupCount( ) );
        if ( mutatieMededelingenMatcher.matches( ) ) {
            nMatches = 3;
            mutatieMededelingenSubString[ 0 ] = mutatieMededelingenMatcher.group( 1 ).trim( );
            mutatieMededelingenSubString[ 2 ] = mutatieMededelingenMatcher.group( 2 ).trim( );
            mutatieMededelingenSubString[ 1 ] = mutatieMededelingenMatcher.group( 3 ).trim( );
            mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 1 ] + "; Kenmerk: " + mutatieMededelingenSubString[ 2 ];
            logger.fine( "match 3, naam: " + mutatieMededelingenSubString[ 0 ] + " rest: " + mutatieMededelingenStrippedString );
            return;
        }

        mutatieMededelingenPattern = Pattern.compile( ".*Naam:(.*?)Kenmerk:(.*?)Omschrijving:(.*)" );
        mutatieMededelingenMatcher = mutatieMededelingenPattern.matcher( mutatieMededelingenString );
        // logger.finer( "mutatieMededelingenMatcher: " + mutatieMededelingenMatcher + ", count:" + mutatieMededelingenMatcher.groupCount( ) );
        if ( mutatieMededelingenMatcher.matches( ) ) {
            nMatches = 3;
            mutatieMededelingenSubString[ 0 ] = mutatieMededelingenMatcher.group( 1 ).trim( );
            mutatieMededelingenSubString[ 2 ] = mutatieMededelingenMatcher.group( 2 ).trim( );
            mutatieMededelingenSubString[ 1 ] = mutatieMededelingenMatcher.group( 3 ).trim( );
            mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 1 ] + "; Kenmerk: " + mutatieMededelingenSubString[ 2 ];
            logger.fine( "match 4, naam: " + mutatieMededelingenSubString[ 0 ] + " rest: " + mutatieMededelingenStrippedString );
            return;
        }

        mutatieMededelingenPattern = Pattern.compile( ".*Naam:(.*?)Omschrijving:(.*?)(?:IBAN|BIC|Mandaat|Crediteur).*" );
        mutatieMededelingenMatcher = mutatieMededelingenPattern.matcher( mutatieMededelingenString );
        // logger.finer( "mutatieMededelingenMatcher: " + mutatieMededelingenMatcher + ", count:" + mutatieMededelingenMatcher.groupCount( ) );
        if ( mutatieMededelingenMatcher.matches( ) ) {
            nMatches = 2;
            mutatieMededelingenSubString[ 0 ] = mutatieMededelingenMatcher.group( 1 ).trim( );
            mutatieMededelingenSubString[ 1 ] = mutatieMededelingenMatcher.group( 2 ).trim( );
            mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 1 ];
            logger.fine( "match 5, naam: " + mutatieMededelingenSubString[ 0 ] + " rest: " + mutatieMededelingenStrippedString );
            return;
        }

        mutatieMededelingenPattern = Pattern.compile( ".*Naam:(.*?)Omschrijving:(.*)" );
        mutatieMededelingenMatcher = mutatieMededelingenPattern.matcher( mutatieMededelingenString );
        // logger.finer( "mutatieMededelingenMatcher: " + mutatieMededelingenMatcher + ", count:" + mutatieMededelingenMatcher.groupCount( ) );
        if ( mutatieMededelingenMatcher.matches( ) ) {
            nMatches = 2;
            mutatieMededelingenSubString[ 0 ] = mutatieMededelingenMatcher.group( 1 ).trim( );
            mutatieMededelingenSubString[ 1 ] = mutatieMededelingenMatcher.group( 2 ).trim( );
            mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 1 ];
            logger.fine( "match 6, naam: " + mutatieMededelingenSubString[ 0 ] + " rest: " + mutatieMededelingenStrippedString );
            return;
        }

        mutatieMededelingenSubString[ 0 ] = mutatieMededelingenString.trim();
        mutatieMededelingenStrippedString = mutatieMededelingenSubString[ 0 ];
        nMatches = 1;
        logger.fine( "geen match, rest: " + mutatieMededelingenStrippedString );
        return;
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
