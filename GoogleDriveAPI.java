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
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.*;
import java.util.ArrayList;

public class GoogleDriveAPI {
	/** ANSI code. */
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

	/** DOWNLOADS_PATH, where you want to save the downloaded files in. */
	private static final String DOWNLOADS_PATH = "/Users/Ryan7WU/Downloads/";

	/** Application name. */
	private static final String APPLICATION_NAME =
		"Google Drive API";

	/** Directory to store user credentials for this application. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(
		System.getProperty("user.home"), ".credentials/google-drive-api.json");

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
			GoogleDriveAPI.class.getResourceAsStream("/client_secret.json");
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

	static void updateProgress(double progress, long size) {
		System.out.print(ANSI_BOLD);
		int percentage = (int)Math.round(progress * 100);
		final int width = 50; // progress bar width in chars
		StringBuilder progressBar = new StringBuilder("╢");
		for(int i = 0; i < width; i++){
			if( i <= (percentage/2)){
				progressBar.append('█');
			} else if( i == (percentage/2)){
				progressBar.append('▓');
			} else {
				progressBar.append('░');
			}
		}
		progressBar.append('╟');
		System.out.printf("\r%s  %.2f%%", progressBar.toString(), progress * 100);
		System.out.printf(ANSI_GREEN + "  %.2f/%.2fMB " + ANSI_RESET, (double)progress*size/1024/1024, (double)size/1024/1024);
		
		
		/*for (int i = 0; true ; i++ ) {
			if (i%4 == 0) System.out.print('▄');
			if (i%4 == 1) System.out.print('▌');
			if (i%4 == 2) System.out.print('▀');
			if (i%4 == 3) System.out.print('▐');
			System.out.print('\b');
			System.out.flush();
			try {
				Thread.sleep(100);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}*/
	}

	private static void downloadFiles(Drive drive, List<File> files) throws IOException {
		for (File file : files) {
			String fileName = file.getName();
			String fileId = file.getId();
			long size = file.getSize();

			class CustomProgressListener implements MediaHttpDownloaderProgressListener {
				public void progressChanged(MediaHttpDownloader downloader) {
					switch (downloader.getDownloadState()) {
						case MEDIA_IN_PROGRESS:
							updateProgress(downloader.getProgress(), size);
							break;
						case MEDIA_COMPLETE:
							updateProgress(downloader.getProgress(), size);
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

			Files.move(Paths.get(fileName), Paths.get(DOWNLOADS_PATH + fileName), ATOMIC_MOVE);
		}
	}

	private static void uploadFile(Drive drive, String fileName) throws IOException {
		// File's content.
		java.io.File mediaFile = new java.io.File(fileName);
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
						updateProgress(uploader.getProgress(), mediaFile.length());
						break;
					case MEDIA_COMPLETE:
						updateProgress(uploader.getProgress(), mediaFile.length());
						System.out.println(ANSI_BOLD + ANSI_BLUE + "\n==>" + ANSI_GREEN + " Upload is complete!" + ANSI_RESET);
						//System.out.println("Upload is complete!");
				}
			}
		}

		// File's metadata.
		File fileMetadata = new File();
		fileMetadata.setName(fileName);

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

	private static void deleteFiles(Drive drive, List<File> files) throws IOException {
		//String fileId = file.getId();
		for (File file : files) {
			drive.files().delete(file.getId()).execute();
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
			System.out.println(ANSI_BOLD + ANSI_BLUE + "==> First 10 items:" + ANSI_RESET);
			for (File file : files) {
				//Hide folders
				if(!file.getMimeType().equals("application/vnd.google-apps.folder")) {
					System.out.printf(String.format(ANSI_BOLD + "%s" + ANSI_RESET, file.getMd5Checksum()));
					if (file.getSize() != null) {
						System.out.printf(" %4sM ", file.getSize()/1024/1024);
					} else {
						System.out.printf(" (null) ");
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

	private static File md5ToFile(List<File> files, String md5Checksum) {
		String fileId = new String();
		String fileName = new String();
		for (File file : files) {
			if(file.getMd5Checksum() != null && file.getMd5Checksum().equals(md5Checksum)) {
				return file;
			}
		}
		return null;
	}

	private static List<File> md5ToFileList(List<File> files, String md5Checksum) {
		String fileId = new String();
		String fileName = new String();
		List<File> delFileList = new ArrayList<File>();
		for (File file : files) {
			if(file.getMd5Checksum() != null && file.getMd5Checksum().equals(md5Checksum)) {
				delFileList.add(file);
			}
		}
		return delFileList;
	}

	private static List<File> md5sToFileList(List<File> files, String md5From, String md5To) {
		List<File> fileList = new ArrayList<File>();
		Boolean flag = false, foundTo = false;
		for (File file : files) {
			if((file.getMd5Checksum() != null && file.getMd5Checksum().equals(md5From)) || flag == true) {
				flag = true;
				fileList.add(file);
				if((file.getMd5Checksum() != null && file.getMd5Checksum().equals(md5To))) {
					flag = false;
					foundTo = true;
				}
			}
		}
		if(!foundTo) {
			fileList = md5sToFileList(files, md5To, md5From);
		}
		return fileList;
	}

	private static List<File> md5Preprocessor(List<File> files, String input) {
		List<String> md5s1 = Arrays.asList(input.split(" "));	//stage 1, split it by space
		List<File> fileList = new ArrayList<File>();
		for (String md5s2 : md5s1) {	//stage 1, make sure every item is s single md5 value
			if (!md5s2.contains("-")) {	//not a range
				fileList.addAll(md5sToFileList(files, md5s2, md5s2));
			} else {
				String[] md5s = md5s2.split("-");
				fileList.addAll(md5sToFileList(files, md5s[0], md5s[1]));
			}
		}
		return fileList;
	}

	private static void printAbout(Drive drive) throws IOException, NullPointerException {
		System.out.println(ANSI_BOLD + ANSI_BLUE);
		About about = drive.about().get()
			.setFields("storageQuota")
			.execute();

		About.StorageQuota aStorageQuota = about.getStorageQuota();
		System.out.printf("Quota: %.1f/%.1fGB" + ANSI_RESET + '\n', (double)aStorageQuota.getUsage()/1024/1024/1024, (double)aStorageQuota.getLimit()/1024/1024/1024);
	}

	public static void main(String[] args) throws IOException {
		// Build a new authorized API client service.
		Drive drive = getDriveService();
		printAbout(drive);
		//Upload
		if (args.length == 1 && !args[0].equals("remove")) {
			uploadFile(drive, args[0]);
		} else {
			List<File> files = listFiles(drive);
			Scanner scanner = new Scanner(System.in);
			while(true) {
				if (args.length == 0) {
					//Download
					System.out.println(ANSI_BOLD + ANSI_BLUE + "==> Enter MD5(s) to download ('md5 ... md5-md5' specifies a range): " + ANSI_RESET);
					String md5Checksum = scanner.nextLine();
					List<File> dowFileList = md5Preprocessor(files, md5Checksum);
					downloadFiles(drive, dowFileList);
				} else {
					//Remove
					System.out.println(ANSI_BOLD + ANSI_BLUE + "==> Enter MD5 to " + ANSI_RED + "REMOVE " + ANSI_BLUE + "file (Ctrl-C to exit): " + ANSI_RESET);
					String md5Checksum = scanner.next();
					List<File> delFileList = md5Preprocessor(files, md5Checksum);
					//List<File> delFileList = md5ToFileList(files, md5Checksum);
					deleteFiles(drive, delFileList);
					//another function
				}
			}
		}
	}
}
