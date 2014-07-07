package net.eads.astrium.it3s.sourcechecksum;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Font;

/**
 * This class is the main window for source checksum application.
 * 
 * @author Bruce BUJON
 *
 */
public class MainWindow extends JFrame {
	/** Serialization id. */
	private static final long serialVersionUID = 2238711327400378548L;
	private JPanel contentPane;
	private JTextField repositoryTextField;
	private JTextField pathTextField;
	private JTextField userTextField;
	private JPasswordField passwdTextField;
	private JLabel lblComputeReleaseSource;
	private JLabel lblCheckReleaseSource;
	private JLabel lblReleaseDirectory;
	private JLabel lblChecksumFile;
	private JButton btnChooseDirectory;
	private JButton btnChooseFile;
	private JButton btnCheckChecksums;

	/**
	 * Create the frame.
	 */
	public MainWindow() {
		setTitle("SourceChecksum");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 360);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 0, 0, 0 };
		gbl_contentPane.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gbl_contentPane.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);
		
		lblComputeReleaseSource = new JLabel("Compute release source checksums");
		lblComputeReleaseSource.setFont(new Font("Tahoma", Font.BOLD, 14));
		GridBagConstraints gbc_lblComputeReleaseSource = new GridBagConstraints();
		gbc_lblComputeReleaseSource.anchor = GridBagConstraints.BASELINE;
		gbc_lblComputeReleaseSource.gridwidth = 2;
		gbc_lblComputeReleaseSource.insets = new Insets(0, 0, 5, 0);
		gbc_lblComputeReleaseSource.gridx = 0;
		gbc_lblComputeReleaseSource.gridy = 0;
		contentPane.add(lblComputeReleaseSource, gbc_lblComputeReleaseSource);

		JLabel lblRepositoryRoot = new JLabel("Repository root:");
		GridBagConstraints gbc_lblRepositoryRoot = new GridBagConstraints();
		gbc_lblRepositoryRoot.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_lblRepositoryRoot.insets = new Insets(0, 0, 5, 5);
		gbc_lblRepositoryRoot.gridx = 0;
		gbc_lblRepositoryRoot.gridy = 1;
		contentPane.add(lblRepositoryRoot, gbc_lblRepositoryRoot);

		repositoryTextField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.anchor = GridBagConstraints.BASELINE;
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 1;
		contentPane.add(repositoryTextField, gbc_textField);
		repositoryTextField.setColumns(10);

		JLabel lblReleasePath = new JLabel("Release path:");
		GridBagConstraints gbc_lblReleasePath = new GridBagConstraints();
		gbc_lblReleasePath.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_lblReleasePath.insets = new Insets(0, 0, 5, 5);
		gbc_lblReleasePath.gridx = 0;
		gbc_lblReleasePath.gridy = 2;
		contentPane.add(lblReleasePath, gbc_lblReleasePath);

		pathTextField = new JTextField();
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.anchor = GridBagConstraints.BASELINE;
		gbc_textField_1.insets = new Insets(0, 0, 5, 0);
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 1;
		gbc_textField_1.gridy = 2;
		contentPane.add(pathTextField, gbc_textField_1);
		pathTextField.setColumns(10);

		JLabel lblUsername = new JLabel("Username:");
		GridBagConstraints gbc_lblUsername = new GridBagConstraints();
		gbc_lblUsername.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_lblUsername.insets = new Insets(0, 0, 5, 5);
		gbc_lblUsername.gridx = 0;
		gbc_lblUsername.gridy = 3;
		contentPane.add(lblUsername, gbc_lblUsername);

		userTextField = new JTextField();
		GridBagConstraints gbc_textField_2 = new GridBagConstraints();
		gbc_textField_2.anchor = GridBagConstraints.BASELINE;
		gbc_textField_2.insets = new Insets(0, 0, 5, 0);
		gbc_textField_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_2.gridx = 1;
		gbc_textField_2.gridy = 3;
		contentPane.add(userTextField, gbc_textField_2);
		userTextField.setColumns(10);

		JLabel lblPassword = new JLabel("Password:");
		GridBagConstraints gbc_lblPassword = new GridBagConstraints();
		gbc_lblPassword.insets = new Insets(0, 0, 5, 5);
		gbc_lblPassword.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_lblPassword.gridx = 0;
		gbc_lblPassword.gridy = 4;
		contentPane.add(lblPassword, gbc_lblPassword);

		passwdTextField = new JPasswordField();
		GridBagConstraints gbc_passwordField = new GridBagConstraints();
		gbc_passwordField.anchor = GridBagConstraints.BASELINE;
		gbc_passwordField.insets = new Insets(0, 0, 5, 0);
		gbc_passwordField.fill = GridBagConstraints.HORIZONTAL;
		gbc_passwordField.gridx = 1;
		gbc_passwordField.gridy = 4;
		contentPane.add(passwdTextField, gbc_passwordField);

		JButton btnNewButton = new JButton("Compute checksums");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				if (fileChooser.showSaveDialog(MainWindow.this) != JFileChooser.APPROVE_OPTION)
					return;
				File file = fileChooser.getSelectedFile();
				
				String repository = MainWindow.this.repositoryTextField.getText();
				String path = MainWindow.this.pathTextField.getText();
				String user = MainWindow.this.userTextField.getText();
				String passwd = new String(MainWindow.this.passwdTextField.getPassword());
				new ChecksumGenerator(repository, path, user, passwd, file, null);
			}
		});
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.anchor = GridBagConstraints.BASELINE;
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton.gridwidth = 2;
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 5;
		contentPane.add(btnNewButton, gbc_btnNewButton);
		
		lblCheckReleaseSource = new JLabel("Check release source checksums");
		lblCheckReleaseSource.setFont(new Font("Tahoma", Font.BOLD, 14));
		GridBagConstraints gbc_lblCheckReleaseSource = new GridBagConstraints();
		gbc_lblCheckReleaseSource.anchor = GridBagConstraints.BASELINE;
		gbc_lblCheckReleaseSource.insets = new Insets(0, 0, 5, 0);
		gbc_lblCheckReleaseSource.gridwidth = 2;
		gbc_lblCheckReleaseSource.gridx = 0;
		gbc_lblCheckReleaseSource.gridy = 6;
		contentPane.add(lblCheckReleaseSource, gbc_lblCheckReleaseSource);
		
		lblReleaseDirectory = new JLabel("Release directory:");
		GridBagConstraints gbc_lblReleaseDirectory = new GridBagConstraints();
		gbc_lblReleaseDirectory.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc_lblReleaseDirectory.insets = new Insets(0, 0, 5, 5);
		gbc_lblReleaseDirectory.gridx = 0;
		gbc_lblReleaseDirectory.gridy = 7;
		contentPane.add(lblReleaseDirectory, gbc_lblReleaseDirectory);
		
		btnChooseDirectory = new JButton("Choose directory");
		GridBagConstraints gbc_btnChooseDirectory = new GridBagConstraints();
		gbc_btnChooseDirectory.anchor = GridBagConstraints.BASELINE;
		gbc_btnChooseDirectory.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnChooseDirectory.insets = new Insets(0, 0, 5, 0);
		gbc_btnChooseDirectory.gridx = 1;
		gbc_btnChooseDirectory.gridy = 7;
		contentPane.add(btnChooseDirectory, gbc_btnChooseDirectory);
		
		lblChecksumFile = new JLabel("Checksum file:");
		GridBagConstraints gbc_lblChecksumFile = new GridBagConstraints();
		gbc_lblChecksumFile.anchor = GridBagConstraints.BASELINE_TRAILING;
		gbc_lblChecksumFile.insets = new Insets(0, 0, 5, 5);
		gbc_lblChecksumFile.gridx = 0;
		gbc_lblChecksumFile.gridy = 8;
		contentPane.add(lblChecksumFile, gbc_lblChecksumFile);
		
		btnChooseFile = new JButton("Choose file");
		GridBagConstraints gbc_btnChooseFile = new GridBagConstraints();
		gbc_btnChooseFile.anchor = GridBagConstraints.BASELINE;
		gbc_btnChooseFile.insets = new Insets(0, 0, 5, 0);
		gbc_btnChooseFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnChooseFile.gridx = 1;
		gbc_btnChooseFile.gridy = 8;
		contentPane.add(btnChooseFile, gbc_btnChooseFile);
		
		btnCheckChecksums = new JButton("Check checksums");
		GridBagConstraints gbc_btnCheckChecksums = new GridBagConstraints();
		gbc_btnCheckChecksums.anchor = GridBagConstraints.BASELINE;
		gbc_btnCheckChecksums.gridwidth = 2;
		gbc_btnCheckChecksums.insets = new Insets(0, 0, 0, 5);
		gbc_btnCheckChecksums.gridx = 0;
		gbc_btnCheckChecksums.gridy = 9;
		contentPane.add(btnCheckChecksums, gbc_btnCheckChecksums);
	}

}
