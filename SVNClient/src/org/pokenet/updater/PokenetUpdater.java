package org.pokenet.updater;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNEventAdapter;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class PokenetUpdater extends JPanel implements ActionListener,
		PropertyChangeListener {

	private static final long serialVersionUID = 5427544579927859151L;
	public static final String SVN_URL = "pokenet-release.svn.sourceforge.net/svnroot/pokenet-release";
	public static final String FOLDER_NAME = "pokenet-release";
	private String HEADER_IMAGE_URL = "http://pokedev.org/header.png";
	private static final String OS = System.getProperty("os.name");
	public static final String SAD_INSTALL_MESSAGE = "Hmm. Game installed, but we couldn't save the location.\nThis means that next time you run, you'll have to select the same installation directory.";
	public static final String TITLE = "Pokenet Installer and Updater";

	private static final int WIDTH = 740;
	private static final int HEIGHT = 470;
	private static final int OUTPUT_HEIGHT = 120;

	private static JFrame m_masterFrame = new JFrame("Pokenet: Valiant Venonat");
	private JProgressBar m_progressBar;
	private JButton m_hideButton;
	private JTextArea m_taskOutput;
	private Task m_task;
	private Component m_output;
	private boolean m_showOutput = true;
	private JButton m_resetLocation;
	
	static boolean m_isWindows = OS.contains("Windows");
	static boolean m_isLinux = OS.contains("Linux");
	static boolean m_isMac = OS.contains("Mac");
	

	private static float m_progressSize = 0;
	private static String m_installpath = "";
	private static String path;

	class Task extends SwingWorker<Void, Void> {
		/*
		 * Main task. Executed in background thread.
		 */
		@Override
		public Void doInBackground() {
			// Initialize progress property.
			setProgress(0);
			DAVRepositoryFactory.setup();
			ISVNOptions options = SVNWCUtil.createDefaultOptions(true);

			/*
			 * Creates an instance of SVNClientManager providing authentication
			 * information (name, password) and an options driver
			 */
			SVNClientManager ourClientManager = SVNClientManager
					.newInstance(options);
			SVNUpdateClient updateClient = ourClientManager.getUpdateClient();

			/*
			 * Creates the event handler to display information
			 */
			ourClientManager.setEventHandler(new SVNEventAdapter() {
				public void handleEvent(SVNEvent event, double progress) {
					SVNEventAction action = event.getAction();
					File curFile = event.getFile();

					String path = curFile.getAbsolutePath();
					path = path.substring(path.indexOf(FOLDER_NAME));

					if (action == SVNEventAction.ADD
							|| action == SVNEventAction.UPDATE_ADD) {
						m_taskOutput.append("Downloading " + path + " \n");

						m_taskOutput.setCaretPosition(m_taskOutput
								.getDocument().getLength());
						System.out.println("Downloading " + curFile.getName());
					}
					if (action == SVNEventAction.STATUS_COMPLETED
							|| action == SVNEventAction.UPDATE_COMPLETED) {
						setProgress(100);
						m_taskOutput.append("Download completed. ");
						m_taskOutput.setCaretPosition(m_taskOutput
								.getDocument().getLength());
						System.out.println("Download completed. ");

					}
				}
			});

			/*
			 * sets externals not to be ignored during the checkout
			 */
			updateClient.setIgnoreExternals(false);

			/*
			 * A url of a repository to check out
			 */
			SVNURL url = null;
			try {
				url = SVNURL.parseURIDecoded("http://" + SVN_URL);
			} catch (SVNException e2) {
				e2.printStackTrace();
			}
			/*
			 * A revision to check out
			 */
			SVNRevision revision = SVNRevision.HEAD;

			/*
			 * A revision for which you're sure that the url you specify is
			 * exactly what you need
			 */
			SVNRevision pegRevision = SVNRevision.HEAD;

			/*
			 * A local path where a Working Copy will be ckecked out
			 */
			File destPath = new File(m_installpath);

			/*
			 * returns the number of the revision at which the working copy is
			 */
			try {
				System.out.println(m_installpath);
				m_taskOutput.append("Game folder: " + m_installpath + "\n");
				boolean exists = destPath.exists();
				if (!exists) {
					m_taskOutput
							.append("Installing...\nPlease be patient while PokeNet is downloaded...\n");
					System.out.println("Installing...");
					updateClient.doCheckout(url, destPath, pegRevision,
							revision, SVNDepth.INFINITY, true);

				} else {
					ourClientManager.getWCClient().doCleanup(destPath);
					m_taskOutput.append("Updating...\n");
					System.out.println("Updating...\n");
					updateClient.doUpdate(destPath, revision,
							SVNDepth.INFINITY, true, true);
				}
			} catch (SVNException e1) {
				// It's probably locked, lets cleanup and resume.
				e1.printStackTrace();
				try {
					ourClientManager.getWCClient().doCleanup(destPath);
					m_taskOutput.append("Resuming Download...\n");
					System.out.println("Resuming Download...\n");
					updateClient.doUpdate(destPath, revision,
							SVNDepth.INFINITY, true, true);
				} catch (SVNException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		/*
		 * Executed in event dispatching thread
		 */
		@Override
		public void done() {
			if (getProgress() < 100) {
				setProgress(100);
				m_taskOutput.append("Download complete!");
				m_taskOutput.setCaretPosition(m_taskOutput.getDocument()
						.getLength());
			}
			try {
				if (!m_installpath.equals("")) {
					m_taskOutput.append("Setting up the install directory...");
					m_taskOutput.setCaretPosition(m_taskOutput.getDocument()
							.getLength());
					File f = new File(path);
					if (m_isLinux || m_isMac || m_isWindows) {
						if (f.exists())
							f.delete();
						PrintWriter pw = new PrintWriter(f);
						pw.write(m_installpath);
						pw.flush();
						pw.close();
					} else {
						JOptionPane.showInternalMessageDialog(m_masterFrame,
								SAD_INSTALL_MESSAGE, TITLE,
								JOptionPane.WARNING_MESSAGE);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane
						.showInternalMessageDialog(m_masterFrame,
								SAD_INSTALL_MESSAGE, TITLE,
								JOptionPane.WARNING_MESSAGE);

			}

			setCursor(null); // turn off the wait cursor
			/*
			 * Launch Jar
			 */
			LaunchPokenet();
		}
	}

	public static void LaunchPokenet() {
		m_masterFrame.setVisible(false);
		String[] args = new String[4];
		try {
			//String s;
			String m_launchCommand = "";
			String sep = File.separator;
			
			if(m_isWindows) { // added " to path
				m_launchCommand = "\"" + m_installpath + "\\pokenet.bat\"";
				File dir = new File(m_installpath);
				Process p = Runtime.getRuntime().exec("cmd /c" + m_launchCommand, null, dir);
			} else {
				args[0] = "java ";
				args[1] = "-Dres.path=" + m_installpath + sep;
				args[2] = "-Djava.library.path=" + m_installpath + sep + "lib" + sep + "native";
				args[3] = "-jar " + m_installpath + sep + "Pokenet.jar";
				Process p = Runtime.getRuntime().exec(args);
			}

			
			for(String s: args) System.out.print(s + " ");
//			
//			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
//					p.getInputStream()));
//			BufferedReader stdError = new BufferedReader(new InputStreamReader(
//					p.getErrorStream()));
//			// Read the output from the command
//
//			String s;
//			while ((s = stdInput.readLine()) != null) {
//				System.out.println(s);
//			}
//			while ((s = stdError.readLine()) != null) {
//				System.out.println("Error: " + s);
//			}

		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane
					.showInternalMessageDialog(
							m_masterFrame,
							"Ouch! Something happened and we couldn't run the game. \nMaybe it didn't install properly?\nError: "
									+ e.getLocalizedMessage(), TITLE,
							JOptionPane.WARNING_MESSAGE);
		}
		System.exit(0);
	}

	public PokenetUpdater() {
		super(new BorderLayout());
		JPanel panel = new JPanel();
		JPanel container = new JPanel();

		panel.setSize(m_masterFrame.getWidth(), 40);
		container.setSize(m_masterFrame.getWidth(), OUTPUT_HEIGHT
				+ panel.getHeight());

		container.setLayout(new BorderLayout());
		panel.setLayout(new BorderLayout());

		updatePokenet();
//		
//		m_resetLocation = new JButton("Reset Game Location");
//		m_hideButton.setActionCommand("reset");
//		m_hideButton.addActionListener(this);
//		m_hideButton.setEnabled(true);
//		panel.add(m_resetLocation, BorderLayout.LINE_START);
		
		m_hideButton = new JButton("Hide Details...");
		m_hideButton.setActionCommand("hide");
		m_hideButton.addActionListener(this);
		m_hideButton.setEnabled(true);
		panel.add(m_hideButton, BorderLayout.LINE_END);

		m_progressBar = new JProgressBar();
		m_progressBar.setIndeterminate(true);
		m_progressBar.setStringPainted(false);
		m_progressBar.setToolTipText("Click on Show Details to see the progress");
		m_progressBar.setSize(WIDTH, m_hideButton.getHeight());

		m_taskOutput = new JTextArea();
		m_taskOutput.setSize(m_masterFrame.getWidth(), OUTPUT_HEIGHT);
		m_taskOutput.setEditable(false);
		m_taskOutput.setAutoscrolls(true);
		m_output = new JScrollPane(m_taskOutput);

		container.add(m_progressBar, BorderLayout.PAGE_END);
		container.add(m_output, BorderLayout.CENTER);

		ImageIcon m_logo;
		JLabel l = null;
		l = new JLabel();
		l.setSize(WIDTH, 190);

		try {
			m_logo = new ImageIcon(new URL(HEADER_IMAGE_URL));
			l = new JLabel(m_logo);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		add(l, BorderLayout.PAGE_START);
		add(container, BorderLayout.CENTER);
		add(panel, BorderLayout.PAGE_END);
	}

	/**
	 * Invoked when the user presses the start button.
	 */
	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals("hide")) {
			if (!m_showOutput) {
//				m_output.setVisible(true);
				m_hideButton.setText("Hide Details...");
				m_masterFrame.setSize(WIDTH, HEIGHT);
				m_showOutput = true;
			} else {
//				m_output.setVisible(false);
				m_hideButton.setText("Show Details...");
				m_masterFrame.setSize(WIDTH, HEIGHT - m_output.getHeight());
				m_showOutput = false;
			}
		} else if (evt.getActionCommand().equals("reset")) {
			File f = new File(path);
			f.delete();
			runApp();
		}
	}

	private void updatePokenet() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		// Instances of javax.swing.SwingWorker are not reusuable, so
		// we create new instances as needed.
		m_task = new Task();
		m_task.addPropertyChangeListener(this);
		m_task.execute();
	}

	/**
	 * Invoked when task's progress property changes.
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress" == evt.getPropertyName()) {
			int progress = (Integer) evt.getNewValue();
			m_progressBar.setValue(progress);
		}
	}

	/**
	 * Create the GUI and show it. As with all GUI code, this must run on the
	 * event-dispatching thread.
	 * 
	 * @param mProgressSize
	 */
	private static void createAndShowGUI() {
		// Create and set up the window.
		m_masterFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		m_masterFrame.setSize(new Dimension(WIDTH, HEIGHT));
		// Create and set up the content pane.
		JComponent newContentPane = new PokenetUpdater();
		newContentPane.setOpaque(true); // content panes must be opaque
		m_masterFrame.setContentPane(newContentPane);

		// Display the window.
		centerTheGUIFrame(m_masterFrame);
		m_masterFrame.setVisible(true);

	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		setPathForOS();
		runApp();
	}

	public static void runApp() {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
		
			public void run() {
				System.out.println("Path path : " + path);
				if (!path.equals("")) {
					BufferedReader br = null;
					try {
						br = new BufferedReader(new FileReader(path));
						m_installpath = br.readLine();
						createAndShowGUI();
					} catch (Exception e) {
						JFileChooser fc = new JFileChooser();
						fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

						int returnVal = fc.showDialog(m_masterFrame,
								"Choose Install Location...");
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							File file = fc.getSelectedFile();
							try {
								m_installpath = file.getCanonicalPath() + (m_isWindows ? "\\" : "/")
										+ FOLDER_NAME;
								createAndShowGUI();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} else {
							JOptionPane.showMessageDialog(m_masterFrame,
									"Thanks for choosing us!", TITLE,
									JOptionPane.INFORMATION_MESSAGE);
							System.exit(0);
						}
					}
				} else {
					/**
					 * Check Updates
					 */
					createAndShowGUI();
				}

			}
		});
	}

	protected static void setPathForOS() {
		if (m_isWindows) {
			path = System.getenv("APPDATA") + "\\.pokenet";
		} else if (m_isLinux) {
			path = System.getenv("HOME") + "/.pokenet";
		} else if (m_isMac) { // Probably?
			path = System.getProperty("user.home") + "/Library/Preferences/org.pokenet.updaterPrefs"; // Maybe.
		}
	}

	/**
	 * This method is used to center the GUI
	 * 
	 * @param frame
	 *            - Frame that needs to be centered.
	 */
	public static void centerTheGUIFrame(JFrame frame) {
		int widthWindow = frame.getWidth();
		int heightWindow = frame.getHeight();

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int X = (screen.width / 2) - (widthWindow / 2); // Center horizontally.
		int Y = (screen.height / 2) - (heightWindow / 2); // Center vertically.

		frame.setBounds(X, Y, widthWindow, heightWindow);

	}

	public static float getProgressSize() {
		return m_progressSize;
	}

	public static void setProgressSize(float f) {
		m_progressSize = f;
	}

	public String getInstallpath() {
		return m_installpath;
	}

	public void setInstallpath(String mInstallpath) {
		m_installpath = mInstallpath;
	}

}
