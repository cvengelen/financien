package financien.millenniummutaties;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Frame to load downloaded Millennium mutaties into the financien database.
 * @author Chris van Engelen
 */
public class LoadMillenniumMutaties extends JInternalFrame {
	private final Logger logger = Logger.getLogger( LoadMillenniumMutaties.class.getCanonicalName( ) );
	private File millenniumMutatieDataFile;
	private final JLabel millenniumMutatieDataFileLabel = new JLabel( );
	private final JButton okButton = new JButton( "OK" );
	private final JButton selectFileButton = new JButton( "Select other file" );

	public LoadMillenniumMutaties(String password, int x, int y) {
		super("Load Millennium mutaties", false, true, true, true);

		class TransactionsFilenameFilter implements FilenameFilter {
			public boolean accept( File directory, String filenameString ) {
				Matcher fileNameMatcher = Pattern.compile( "MOVS_1.*\\.csv" ).matcher( filenameString );
				return fileNameMatcher.find( );
			}
		}

		// Find the last modified file in the directory with downloaded Millennium mutatie data files
		final String millenniumMutatieDataDirectory = "/Users/cvengelen/Downloads";
		File millenniumMutatieDataDirectoryFile = new File( millenniumMutatieDataDirectory );
		String[ ] fileNamesArray = millenniumMutatieDataDirectoryFile.list( new TransactionsFilenameFilter( ) );
		if ( ( fileNamesArray == null ) || ( fileNamesArray.length == 0 ) ) {
			System.err.println("Geen Millennium mutatie data files gevonden in " + millenniumMutatieDataDirectory);
			JOptionPane.showMessageDialog(this,
					"Geen Millennium mutatie data files gevonden in " + millenniumMutatieDataDirectory,
					"Load Millennium mutaties error",
					JOptionPane.ERROR_MESSAGE);
			dispose();
			return;
		}

		SortedMap< Long, File > fileNamesMap = new TreeMap<>( );
		for ( String fileName: fileNamesArray ) {
			File mutatieDataFile = new File( millenniumMutatieDataDirectory + "/" + fileName );
			fileNamesMap.put( mutatieDataFile.lastModified( ), mutatieDataFile );
		}
		millenniumMutatieDataFile = fileNamesMap.get( fileNamesMap.lastKey( ) );
		logger.info( "Last modified file: " + millenniumMutatieDataFile.getName( ) );
		millenniumMutatieDataFileLabel.setText( millenniumMutatieDataFile.getName( ) );

		class TransactionsFileFilter extends FileFilter {
			public boolean accept( File file ) {
				//Accept directories, and comma separated value files only
				if ( file.isDirectory( ) ) {
					return true;
				}

				String fileNameString = file.getName( );
				Matcher fileNameMatcher = Pattern.compile( "MOVS_1.*\\.csv" ).matcher( fileNameString );
				return fileNameMatcher.find( );
			}

			public String getDescription() {
				return "Millennium transactions files";
			}
		}

		final JFileChooser loadMillenniumMutatieDataFileChooser = new JFileChooser( millenniumMutatieDataDirectory );

		loadMillenniumMutatieDataFileChooser.setFileFilter( new TransactionsFileFilter( ) );
		loadMillenniumMutatieDataFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		loadMillenniumMutatieDataFileChooser.setDialogType( JFileChooser.OPEN_DIALOG );

		// When the JFileChooser dialog is opened with showOpenDialog, a bug in the underlying Swing/Aqua code
		// sets the selected file back to null. See AquaFileChooserUI.SelectionListener.valueChanged,
		// which calls JFileChooser.setSelectedFile with a null value:   var4.setSelectedFile(var2);
		// Therefore, the AncestorListener is used to set the selected file after the JFileChooser window has been opened.
		loadMillenniumMutatieDataFileChooser.addAncestorListener( new AncestorListener() {
			@Override
			public void ancestorAdded( AncestorEvent event ) {
				loadMillenniumMutatieDataFileChooser.setSelectedFile( millenniumMutatieDataFile );
			}

			@Override
			public void ancestorRemoved( AncestorEvent event ) { }

			@Override
			public void ancestorMoved( AncestorEvent event ) { }
		} );

		// Use the grid bag layout manager
		final Container container = getContentPane( );
		container.setLayout( new GridBagLayout( ) );
		final GridBagConstraints constraints = new GridBagConstraints( );

		final JPanel filePanel = new JPanel( );
		final JLabel filePrefix = new JLabel( "Millennium transactions file:" );
		final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );
		filePrefix.setFont( dialogFont );
		filePanel.add( filePrefix, constraints );
		millenniumMutatieDataFileLabel.setFont( dialogFont );
		filePanel.add( millenniumMutatieDataFileLabel, constraints );

		constraints.insets = new Insets( 15, 15, 5, 15 );
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.weightx = 1d;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		container.add( filePanel, constraints );

		final JButton cancelButton = new JButton( "Cancel" );
		cancelButton.addActionListener( ( ActionEvent actionEvent ) -> {
			logger.fine( "event: " + actionEvent.getActionCommand( ) );
			if ( actionEvent.getActionCommand().equals( "Cancel" ) ) {
				dispose( );
			}
		} );

		selectFileButton.addActionListener( ( ActionEvent actionEvent ) -> {
			logger.fine( "event: " + actionEvent.getActionCommand( ) );
			if ( actionEvent.getActionCommand().equals( "Select other file" ) ) {
				okButton.setEnabled( false );
				selectFileButton.setEnabled( false );

				// Set the selected file to null, to make sure that there is a change in selected file
				// when the selected file is set in the AncestorListener.
				loadMillenniumMutatieDataFileChooser.setSelectedFile( null );

				if ( loadMillenniumMutatieDataFileChooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION ) {
					millenniumMutatieDataFile = loadMillenniumMutatieDataFileChooser.getSelectedFile( );
					millenniumMutatieDataFileLabel.setText( millenniumMutatieDataFile.getName( ) );
					logger.info( "Selected file: " + millenniumMutatieDataFile.getName( ) );
				}
				okButton.setEnabled( true );
				selectFileButton.setEnabled( true );
				getRootPane( ).setDefaultButton( okButton );
				okButton.requestFocusInWindow();
			}
		} );

		okButton.addActionListener( ( ActionEvent actionEvent ) -> {
			logger.fine( "event: " + actionEvent.getActionCommand( ) );
			if ( actionEvent.getActionCommand().equals( "OK" ) ) {
				String[ ] loadMillenniumMutatieDataCmdArray = { "/Users/cvengelen/bin/load-millennium-mutatie-data",
						"-f", millenniumMutatieDataFile.getAbsolutePath( ), "-p", password };
				String loadMillenniumMutatieDataCmd = String.join( " ", loadMillenniumMutatieDataCmdArray );
				logger.info( "Executing command: " + loadMillenniumMutatieDataCmd );
				try ( Process process = Runtime.getRuntime( ).exec( loadMillenniumMutatieDataCmdArray ) ) {

					// The thread must wait for the process to finish
					int exitStatus = process.waitFor( );
					logger.info( "Process exit status: " + exitStatus );
					if ( exitStatus != 0 ) {
						System.err.println( "Error in executing " + loadMillenniumMutatieDataCmd );
						JOptionPane.showMessageDialog( this,
													   "Error in executing " + loadMillenniumMutatieDataCmd,
													   "Load Millennium mutaties error",
													   JOptionPane.ERROR_MESSAGE );
					}
					else {
						JOptionPane.showMessageDialog( this,
													   "Successfully loaded Millennium mutatie data",
													   "Load Millennium mutaties",
													   JOptionPane.INFORMATION_MESSAGE );
					}
				} catch ( InterruptedException | IOException exception ) {
					logger.severe( exception.getMessage( ) );
				}
				dispose( );
			}
		} );

		final JPanel buttonPanel = new JPanel( );
		buttonPanel.add( okButton );
		buttonPanel.add( selectFileButton );
		buttonPanel.add( cancelButton );

		constraints.insets = new Insets( 5, 15, 15, 15 );
		constraints.gridx = 0;
		constraints.gridy = 1;
		container.add( buttonPanel, constraints );

		setSize( 600, 150 );
		setLocation(x, y);
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		getRootPane( ).setDefaultButton( okButton );
		setVisible(true);
	}
}
