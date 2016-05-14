package financien.loadingmutatiedata;

import financien.gui.PasswordPanel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;

import java.util.regex.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.lang.Runtime;

/**
 * Frame to load downloaded ING mutatie data into the financien database.
 *
 * @author Chris van Engelen
 */
class LoadIngMutatieDataFrame {
    private final Logger logger = Logger.getLogger( LoadIngMutatieDataFrame.class.getCanonicalName( ) );
    private final JFrame frame = new JFrame( "Load ING export file" );
    private File ingMutatieDataFile;
    private final JLabel ingMutatieDataFileLabel = new JLabel( );
    private final JButton okButton = new JButton( "OK" );
    private final JButton selectFileButton = new JButton( "Select other file" );

    LoadIngMutatieDataFrame( ) {

        class CsvFilenameFilter implements FilenameFilter {
	    public boolean accept( File directory, String filenameString ) {
		Matcher fileNameMatcher = Pattern.compile( "NL.*(2540650|1514423).*\\.csv" ).matcher( filenameString );
		return fileNameMatcher.find( );
	    }
	}

	// Find the last modified file in the directory with downloaded ING mutatie data files
	final String ingMutatieDataDirectoryString = "/Users/cvengelen/Downloads";
	File ingMutatieDataDirectoryFile = new File( ingMutatieDataDirectoryString );
	String[ ] filenameStringArray = ingMutatieDataDirectoryFile.list( new CsvFilenameFilter( ) );
	SortedMap< Long, File > filenameMap = new TreeMap<>( );
	for ( String filenameString:  filenameStringArray ) {
	    File ingMutatieDataFile = new File( ingMutatieDataDirectoryString + "/" + filenameString );
	    filenameMap.put( ingMutatieDataFile.lastModified( ), ingMutatieDataFile );
	}
	ingMutatieDataFile = filenameMap.get( filenameMap.lastKey( ) );
        logger.info( "Last modified file: " + ingMutatieDataFile.getName( ) );
        ingMutatieDataFileLabel.setText( ingMutatieDataFile.getName( ) );

	class CsvFileFilter extends FileFilter {
	    public boolean accept( File file ) {

		//Accept directories, and comma separated value files only
                if ( file.isDirectory( ) ) {
                    return true;
                }

		String fileNameString = file.getName( );
		Matcher fileNameMatcher = Pattern.compile( "NL.*(2540650|1514423).*\\.csv" ).matcher( fileNameString );
		return fileNameMatcher.find( );
	    }

	    public String getDescription() {
		return "ING export files";
	    }
	}

        final Container container = frame.getContentPane( );

        // Set grid bag layout manager
        container.setLayout( new GridBagLayout( ) );
        final GridBagConstraints constraints = new GridBagConstraints( );

        final JFileChooser loadIngMutatieDataFileChooser = new JFileChooser( ingMutatieDataDirectoryString );

	loadIngMutatieDataFileChooser.setFileFilter( new CsvFileFilter( ) );
        loadIngMutatieDataFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
	loadIngMutatieDataFileChooser.setSelectedFile( ingMutatieDataFile );
        loadIngMutatieDataFileChooser.ensureFileIsVisible( ingMutatieDataFile );
        loadIngMutatieDataFileChooser.setDialogType( JFileChooser.OPEN_DIALOG );
        loadIngMutatieDataFileChooser.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        loadIngMutatieDataFileChooser.setVisible( false );
        loadIngMutatieDataFileChooser.addActionListener(
                ( ActionEvent actionEvent ) -> {
                    logger.info( "event: " + actionEvent.getActionCommand( ) );
                    if ( actionEvent.getActionCommand().equals( JFileChooser.APPROVE_SELECTION ) ) {
                        ingMutatieDataFile = loadIngMutatieDataFileChooser.getSelectedFile( );
                        ingMutatieDataFileLabel.setText( ingMutatieDataFile.getName( ) );
                    }
                    loadIngMutatieDataFileChooser.setVisible( false );
                    okButton.setEnabled( true );
                    selectFileButton.setEnabled( true );
                    frame.getRootPane( ).setDefaultButton( okButton );
                    frame.setSize( 600, 150 );
                } );

        constraints.insets = new Insets( 10, 10, 5, 10 );
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        container.add( loadIngMutatieDataFileChooser, constraints );

        final JPanel filePanel = new JPanel( );
        final JLabel filePrefix = new JLabel( "ING export file:" );
        final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );
        filePrefix.setFont( dialogFont );
        filePanel.add( filePrefix, constraints );
        ingMutatieDataFileLabel.setFont( dialogFont );
        filePanel.add( ingMutatieDataFileLabel, constraints );

        constraints.insets = new Insets( 5, 10, 5, 10 );
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        container.add( filePanel, constraints );

        final JButton cancelButton = new JButton( "Cancel" );
        cancelButton.addActionListener(
                ( ActionEvent actionEvent ) -> {
                    logger.info( "event: " + actionEvent.getActionCommand( ) );
                    if ( actionEvent.getActionCommand().equals( "Cancel" ) ) {
                        System.exit( 1 );
                    }
                } );

        selectFileButton.addActionListener(
                ( ActionEvent actionEvent ) -> {
                    logger.info( "event: " + actionEvent.getActionCommand( ) );
                    if ( actionEvent.getActionCommand().equals( "Select other file" ) ) {
                        frame.setSize( 600, 550 );
                        loadIngMutatieDataFileChooser.setVisible( true );
                        okButton.setEnabled( false );
                        selectFileButton.setEnabled( false );
                    }
                } );

        okButton.addActionListener(
                ( ActionEvent actionEvent ) -> {
                  logger.info( "event: " + actionEvent.getActionCommand( ) );
                    if ( actionEvent.getActionCommand().equals( "OK" ) ) {
                        // Get the password for the financien account, which gives access to schema financien.
                        final PasswordPanel passwordPanel = new PasswordPanel();
                        final String password = passwordPanel.getPassword();
                        if (password == null) {
                            logger.info("No password");
                            System.err.println("Geen password gegeven");
                            System.exit( 1 );
                        }
                        int exitStatus = 0;
                        String loadIngMutatieDataCmd = "/Users/cvengelen/bin/load-ing-mutatie-data -f " +
                                ingMutatieDataFile.getAbsolutePath( ) + " -p " + password;
                        try {
                            logger.fine( "Executing command: " + loadIngMutatieDataCmd );
                            Process process = Runtime.getRuntime( ).exec( loadIngMutatieDataCmd );

                            // The thread must wait for the process to finish
                            exitStatus = process.waitFor( );
                            logger.info( "Process exit status: " + exitStatus );
                            if ( exitStatus != 0 ) {
                                System.err.println( "Error in executing " + loadIngMutatieDataCmd );
                            }
                        } catch ( InterruptedException | IOException exception ) {
                            logger.severe( exception.getMessage( ) );
                            exitStatus = 1;
                        }
                       System.exit( exitStatus );
                    }
                } );

        final JPanel buttonPanel = new JPanel( );
        buttonPanel.add( okButton );
        buttonPanel.add( selectFileButton );
        buttonPanel.add( cancelButton );

        constraints.insets = new Insets( 5, 10, 10, 10 );
        constraints.gridx = 0;
        constraints.gridy = 2;
        container.add( buttonPanel, constraints );

        frame.setSize( 600, 150 );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.getRootPane( ).setDefaultButton( okButton );
        logger.info( "set frame visible" );
        frame.setVisible(true);
    }
}
