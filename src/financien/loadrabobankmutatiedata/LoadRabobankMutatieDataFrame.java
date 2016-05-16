package financien.loadrabobankmutatiedata;

import financien.gui.PasswordPanel;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Frame to load downloaded Rabobank mutatie data into the financien database.
 *
 * @author Chris van Engelen
 */
class LoadRabobankMutatieDataFrame {
    private final Logger logger = Logger.getLogger( LoadRabobankMutatieDataFrame.class.getCanonicalName( ) );
    private final JFrame frame = new JFrame( "Load Rabobank transactions file" );
    private File rabobankMutatieDataFile;
    private final JLabel rabobankMutatieDataFileLabel = new JLabel( );
    private final JButton okButton = new JButton( "OK" );
    private final JButton selectFileButton = new JButton( "Select other file" );
    private Dimension frameDimension;
    private boolean windowGainedFocus = false;

    LoadRabobankMutatieDataFrame() {

	class TransactionsFilenameFilter implements FilenameFilter {
	    public boolean accept( File directory, String filenameString ) {
		Matcher fileNameMatcher = Pattern.compile( "transactions.*\\.txt" ).matcher( filenameString );
		return fileNameMatcher.find( );
	    }
	}

	// Find the last modified file in the directory with downloaded Rabobank mutatie data files
	final String rabobankMutatieDataDirectory = "/Users/cvengelen/Downloads";
	File raboMutatieDataDirectoryFile = new File( rabobankMutatieDataDirectory );
	String[ ] fileNamesArray = raboMutatieDataDirectoryFile.list( new TransactionsFilenameFilter( ) );
	SortedMap< Long, File > fileNamesMap = new TreeMap<>( );
	for ( String fileName: fileNamesArray ) {
	    File raboMutatieDataFile = new File( rabobankMutatieDataDirectory + "/" + fileName );
	    fileNamesMap.put( raboMutatieDataFile.lastModified( ), raboMutatieDataFile );
	}
        rabobankMutatieDataFile = fileNamesMap.get( fileNamesMap.lastKey( ) );
        logger.info( "Last modified file: " + rabobankMutatieDataFile.getName( ) );
        rabobankMutatieDataFileLabel.setText( rabobankMutatieDataFile.getName( ) );

	class TransactionsFileFilter extends FileFilter {
	    public boolean accept( File file ) {

		//Accept directories, and comma separated value files only
		if ( file.isDirectory( ) ) {
		    return true;
 		}

		String fileNameString = file.getName( );
		Matcher fileNameMatcher = Pattern.compile( "transactions.*\\.txt" ).matcher( fileNameString );
		return fileNameMatcher.find( );
	    }

	    public String getDescription() {
		return "Rabobank transactions files";
	    }
	}

        final Container container = frame.getContentPane( );

        // Set grid bag layout manager
        container.setLayout( new GridBagLayout( ) );
        final GridBagConstraints constraints = new GridBagConstraints( );

        final JFileChooser loadRabobankMutatieDataFileChooser = new JFileChooser( rabobankMutatieDataDirectory );

        loadRabobankMutatieDataFileChooser.setFileFilter( new TransactionsFileFilter( ) );
        loadRabobankMutatieDataFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        loadRabobankMutatieDataFileChooser.setSelectedFile( rabobankMutatieDataFile );
        loadRabobankMutatieDataFileChooser.ensureFileIsVisible( rabobankMutatieDataFile );
        loadRabobankMutatieDataFileChooser.setDialogType( JFileChooser.OPEN_DIALOG );
        loadRabobankMutatieDataFileChooser.setVisible( false );
        loadRabobankMutatieDataFileChooser.addActionListener(
                ( ActionEvent actionEvent ) -> {
                    logger.info( "event: " + actionEvent.getActionCommand( ) );
                    if ( actionEvent.getActionCommand().equals( JFileChooser.APPROVE_SELECTION ) ) {
                        rabobankMutatieDataFile = loadRabobankMutatieDataFileChooser.getSelectedFile( );
                        rabobankMutatieDataFileLabel.setText( rabobankMutatieDataFile.getName( ) );
                    }
                    frameDimension = frame.getSize();
                    loadRabobankMutatieDataFileChooser.setVisible( false );
                    okButton.setEnabled( true );
                    selectFileButton.setEnabled( true );
                    frame.getRootPane( ).setDefaultButton( okButton );
                    frame.setSize( 600, 150 );
                } );

        constraints.insets = new Insets( 15, 15, 5, 15 );
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        container.add( loadRabobankMutatieDataFileChooser, constraints );

        final JPanel filePanel = new JPanel( );
        final JLabel filePrefix = new JLabel( "Rabobank transactions file:" );
        final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );
        filePrefix.setFont( dialogFont );
        filePanel.add( filePrefix, constraints );
        rabobankMutatieDataFileLabel.setFont( dialogFont );
        filePanel.add( rabobankMutatieDataFileLabel, constraints );

        constraints.insets = new Insets( 5, 15, 5, 15 );
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
                        if (frameDimension == null) {
                            frame.setSize( 600, 550 );
                        } else {
                            frame.setSize( frameDimension );
                        }
                        loadRabobankMutatieDataFileChooser.setVisible( true );
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
                        String loadRabobankMutatieDataCmd = "/Users/cvengelen/bin/load-rabobank-mutatie-data -f " +
                                rabobankMutatieDataFile.getAbsolutePath( ) + " -p " + password;
                        try {
                            logger.fine( "Executing command: " + loadRabobankMutatieDataCmd );
                            Process process = Runtime.getRuntime( ).exec( loadRabobankMutatieDataCmd );

                            // The thread must wait for the process to finish
                            exitStatus = process.waitFor( );
                            logger.info( "Process exit status: " + exitStatus );
                            if ( exitStatus != 0 ) {
                                System.err.println( "Error in executing " + loadRabobankMutatieDataCmd );
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

        constraints.insets = new Insets( 5, 15, 15, 15 );
        constraints.gridx = 0;
        constraints.gridy = 2;
        container.add( buttonPanel, constraints );

        // Add a window focus listener: this seems to be necessary to ensure that the frame actually becomes visible
        frame.addWindowFocusListener( new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent windowEvent ) {
                logger.info( "window has gained focus" );
                windowGainedFocus = true;
            }
        } );

        frame.setSize( 600, 150 );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.getRootPane( ).setDefaultButton( okButton );
        logger.info( "set frame visible" );
        frame.setVisible(true);

        // Infinite loop to set frame visible if the initial call was not effective
        int frameActivations = 1;
        try {
            Thread.sleep( 1000 );
            while( !windowGainedFocus ) {
                logger.info( "set frame visible " + ++frameActivations );
                frame.setVisible( true );
                Thread.sleep( 1000 );
            }
        } catch (InterruptedException interruptedException) {
            logger.info("sleep interrupted: " + interruptedException.getLocalizedMessage());
        }
    }
}
