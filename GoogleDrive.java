import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.api.services.drive.Drive;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import java.util.Scanner;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.NoSuchElementException;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import javafx.util.Pair;

public class GoogleDrive {
	public static final String ANSI_BOLD = "\u001B[1m";
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	/** Application name. */
	private static final String APPLICATION_NAME =
		"Drive API Java Quickstart";

	/** Directory to store user credentials for this application. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(
		System.getProperty("user.home"), ".credentials/drive-java-quickstart.json");

	/** Global instance of the {@link FileDataStoreFactory}. */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY =
		JacksonFactory.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	/** Global instance of the scopes required by this quickstart.
	 *
	 * If modifying these scopes, delete your previously saved credentials
	 * at ~/.credentials/drive-java-quickstart.json
	 */
	private static final List<String> SCOPES =
		Arrays.asList(DriveScopes.DRIVE);

	static {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Creates an authorized Credential object.
	 * @return an authorized Credential object.
	 * @throws IOException
	 */
	public static Credential authorize() throws IOException {
		// Load client secrets.
		InputStream in =
			GoogleDrive.class.getResourceAsStream("/client_secret.json");
		GoogleClientSecrets clientSecrets =
			GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow =
				new GoogleAuthorizationCodeFlow.Builder(
						HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
				.setDataStoreFactory(DATA_STORE_FACTORY)
				.setAccessType("offline")
				.build();
		Credential credential = new AuthorizationCodeInstalledApp(
			flow, new LocalServerReceiver()).authorize("user");
		System.out.println(
				"Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
		return credential;
	}

	/**
	 * Build and return an authorized Drive client service.
	 * @return an authorized Drive client service
	 * @throws IOException
	 */
	public static Drive getDriveService() throws IOException {
		Credential credential = authorize();
		return new Drive.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME)
				.build();
	}

	static void updateProgress(double progress) {
		int percentage = (int)Math.round(progress * 100);
		final int width = 50; // progress bar width in chars
		StringBuilder bar = new StringBuilder(ANSI_BOLD + ANSI_BLUE + "\r[");
		for(int i = 0; i < width; i++){
			if( i < (percentage/2)){
				bar.append('=');
			} else if( i == (percentage/2)){
				bar.append('>');
			} else {
				bar.append('-');
			}
		}
		bar.append(']');
		System.out.printf("%s  %.2f%%", bar.toString(), progress * 100, ANSI_RESET);
	}

	private static void downloadFile(Drive drive, String fileId, String fileName) throws IOException {
		class CustomProgressListener implements MediaHttpDownloaderProgressListener {
			public void progressChanged(MediaHttpDownloader downloader) {
				switch (downloader.getDownloadState()) {
					case MEDIA_IN_PROGRESS:
						updateProgress(downloader.getProgress());
						break;
					case MEDIA_COMPLETE:
						updateProgress(downloader.getProgress());
						System.out.println(ANSI_BOLD + ANSI_BLUE + "\n==>" + ANSI_GREEN + " Download is complete!" + ANSI_RESET);
						//System.out.println("Download is complete!");
				}
			}
		}

		OutputStream out = new FileOutputStream(fileName);
		Drive.Files.Get request = drive.files().get(fileId);
		System.out.println(ANSI_BOLD + ANSI_BLUE + "==> " + ANSI_BLACK + "Downloading " + fileName + ANSI_RESET);

		request.getMediaHttpDownloader().setProgressListener(new CustomProgressListener());
		request.executeMediaAndDownloadTo(out);
	}

	private static void uploadFile(Drive drive, String fileName) throws IOException {
		class CustomProgressListener implements MediaHttpUploaderProgressListener {
			public void progressChanged(MediaHttpUploader uploader) throws IOException {
				switch (uploader.getUploadState()) {
					case INITIATION_STARTED:
						//System.out.println("Initiation has started!");
						break;
					case INITIATION_COMPLETE:
						System.out.println(ANSI_BOLD + ANSI_BLUE + "==> " + ANSI_BLACK + "Uploading " + fileName + ANSI_RESET);
						//System.out.println("Initiation is complete!");
						break;
					case MEDIA_IN_PROGRESS:
						updateProgress(uploader.getProgress());
						break;
					case MEDIA_COMPLETE:
						updateProgress(uploader.getProgress());
						System.out.println(ANSI_BOLD + ANSI_BLUE + "\n==>" + ANSI_GREEN + " Upload is complete!" + ANSI_RESET);
						//System.out.println("Upload is complete!");
				}
			}
		}

		// File's metadata.
		File fileMetadata = new File();
		fileMetadata.setName(fileName);
		//fileMetadata.setDescription(description);
		//fileMetadata.setMimeType(mimeType);

		// File's content.
		java.io.File mediaFile = new java.io.File(fileName);
		InputStreamContent mediaContent =
			new InputStreamContent(null,
				new BufferedInputStream(new FileInputStream(mediaFile)));
		mediaContent.setLength(mediaFile.length());
		try {
			Drive.Files.Create request = drive.files().create(fileMetadata, mediaContent);
			request.getMediaHttpUploader().setProgressListener(new CustomProgressListener());
			request.execute();

		// Uncomment the following line to print the File ID.
		// System.out.println("File ID: " + file.getId());
		} catch (IOException e) {
			System.out.println("An error occured: " + e);
		}
	}

	private static void deleteFile(Drive drive, String fileId) {
		try {
			drive.files().delete(fileId).execute();
		} catch (IOException e) {
			System.out.println("An error occurred: " + e);
		}
	}

	private static List<File> listFiles(Drive drive) throws IOException {
		// Print the names and IDs for up to 10 files.
		FileList result = drive.files().list()
			.setPageSize(10)
			.setFields("nextPageToken, files(id, name, size, md5Checksum, mimeType)")
			.execute();
		List<File> files = result.getFiles();
		if (files == null || files.size() == 0) {
			System.out.println("No files found.");
		} else {
			System.out.println("Files:");
			for (File file : files) {
				//Hide folders
				if(!file.getMimeType().equals("application/vnd.google-apps.folder")) {
					System.out.printf(String.format("%-32s", file.getMd5Checksum()));
					System.out.print(' ');
					if (file.getSize() != null) {
						System.out.printf("%4sM\t", file.getSize()/1024/1024);
					} else {
						System.out.printf("(null)\t");
					}
					String fileName = file.getName();
					if (fileName.length() < 45) {
						System.out.printf(String.format("%-50s\n", fileName));
					} else {
						String subName1 = fileName.substring(0, 30);
						String subName2 = fileName.substring(fileName.length()-12, fileName.length());
						System.out.printf(String.format("%30s...%s\n", subName1, subName2));
					}
				}
			}
		}
		return files;
	}

	private static Pair<String,String> md5ToFileIdAndFileName(List<File> files, String md5Checksum) {
		String fileId = new String();
		String fileName = new String();
		for (File file : files) {
			if(file.getMd5Checksum() != null && file.getMd5Checksum().equals(md5Checksum)) {
				fileId = file.getId();
				fileName = file.getName();
			}
		}
		return new Pair<>(fileId, fileName);
	}

	public static void main(String[] args) throws IOException {
		// Build a new authorized API client service.
		Drive drive = getDriveService();
		//Upload
		if (args.length == 1 && !args[0].equals("-r")) {
			uploadFile(drive, args[0]);
		} else {
			List<File> files = listFiles(drive);
			Scanner scanner = new Scanner(System.in);
			if (args.length == 0) {
				//Download
				System.out.println("Enter MD5 to download: ");
				String md5Checksum = scanner.next();
				Pair<String,String> fileNameAndFileId = md5ToFileIdAndFileName(files, md5Checksum);
				downloadFile(drive, fileNameAndFileId.getKey(), fileNameAndFileId.getValue());
			} else {
				while(true) {
					//Remove
					System.out.println("Enter MD5 to remove file: ");
					String md5Checksum = scanner.next();
					Pair<String,String> fileNameAndFileId = md5ToFileIdAndFileName(files, md5Checksum);
					deleteFile(drive, fileNameAndFileId.getKey());
				}
			}
		}
	}
}
