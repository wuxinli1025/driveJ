import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static java.nio.file.StandardCopyOption.*;

public class DriveJ {
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
    private static final String DOWNLOADS_PATH = Global.DOWNLOADS_PATH;

    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart. If modifying these
     * scopes, delete your previously saved credentials/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";

    /**
     * Creates an authorized Credential object.
     * 
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = DriveJ.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                clientSecrets, SCOPES)
                        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                        .setAccessType("offline").build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static Drive getDriveService(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME).build();
    }

    private static void downloadFiles(Drive drive, List<File> files) throws IOException {
        for (File file : files) {
            String fileName = file.getName();
            String fileId = file.getId();
            long size = file.getSize();

            OutputStream out = new FileOutputStream(fileName);
            Drive.Files.Get request = drive.files().get(fileId);
            System.out.println(ANSI_BOLD + ANSI_BLUE + "==> " + ANSI_BLACK + "Downloading " + fileName + ANSI_RESET);

            request.getMediaHttpDownloader().setProgressListener(new FileDownloadProgressListener(size));
            request.executeMediaAndDownloadTo(out);

            Files.move(Paths.get(fileName), Paths.get(DOWNLOADS_PATH + fileName), ATOMIC_MOVE);
        }
    }

    private static void uploadFile(Drive drive, String fileName) throws IOException {
        // File's content.
        java.io.File mediaFile = new java.io.File(fileName);

        // File's metadata.
        File fileMetadata = new File();
        fileMetadata.setName(fileName);

        InputStreamContent mediaContent = new InputStreamContent(null,
                new BufferedInputStream(new FileInputStream(mediaFile)));
        mediaContent.setLength(mediaFile.length());
        try {
            Drive.Files.Create request = drive.files().create(fileMetadata, mediaContent);
            request.getMediaHttpUploader().setProgressListener(new FileUploadProgressListener(fileName, mediaFile));
            request.execute();

            // Uncomment the following line to print the File ID.
            // System.out.println("File ID: " + file.getId());
        } catch (IOException e) {
            System.out.println("An error occured: " + e);
        }
    }

    private static void deleteFiles(Drive drive, List<File> files) throws IOException {
        // String fileId = file.getId();
        for (File file : files) {
            drive.files().delete(file.getId()).execute();
        }
    }

    private static List<File> listFiles(Drive drive) throws IOException {
        // Print the names and IDs for up to 10 files.
        FileList result = drive.files().list().setPageSize(10)
                .setFields("nextPageToken, files(id, name, size, md5Checksum, mimeType)").execute();
        List<File> files = result.getFiles();
        if (files == null || files.size() == 0) {
            System.out.println("No files found.");
        } else {
            System.out.println(ANSI_BOLD + ANSI_BLUE + "==> First 10 items:" + ANSI_RESET);
            for (File file : files) {
                // Hide folders
                if (!file.getMimeType().equals("application/vnd.google-apps.folder")) {
                    System.out.printf(String.format(ANSI_BOLD + "%s" + ANSI_RESET, file.getMd5Checksum()));
                    if (file.getSize() != null) {
                        System.out.printf(" %4sM ", file.getSize() / 1024 / 1024);
                    } else {
                        System.out.printf(" (null) ");
                    }
                    String fileName = file.getName();
                    if (fileName.length() < 45) {
                        System.out.printf(String.format("%-50s\n", fileName));
                    } else {
                        String subName1 = fileName.substring(0, 30);
                        String subName2 = fileName.substring(fileName.length() - 12, fileName.length());
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
            if (file.getMd5Checksum() != null && file.getMd5Checksum().equals(md5Checksum)) {
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
            if (file.getMd5Checksum() != null && file.getMd5Checksum().equals(md5Checksum)) {
                delFileList.add(file);
            }
        }
        return delFileList;
    }

    private static List<File> md5sToFileList(List<File> files, String md5From, String md5To) {
        List<File> fileList = new ArrayList<File>();
        Boolean flag = false, foundTo = false;
        for (File file : files) {
            if ((file.getMd5Checksum() != null && file.getMd5Checksum().equals(md5From)) || flag == true) {
                flag = true;
                fileList.add(file);
                if ((file.getMd5Checksum() != null && file.getMd5Checksum().equals(md5To))) {
                    flag = false;
                    foundTo = true;
                }
            }
        }
        if (!foundTo) {
            fileList = md5sToFileList(files, md5To, md5From);
        }
        return fileList;
    }

    private static List<File> md5Preprocessor(List<File> files, String input) {
        List<String> md5s1 = Arrays.asList(input.split(" ")); // stage 1, split it by space
        List<File> fileList = new ArrayList<File>();
        for (String md5s2 : md5s1) { // stage 1, make sure every item is s single md5 value
            if (!md5s2.contains("-")) { // not a range
                fileList.addAll(md5sToFileList(files, md5s2, md5s2));
            } else {
                String[] md5s = md5s2.split("-");
                fileList.addAll(md5sToFileList(files, md5s[0], md5s[1]));
            }
        }
        return fileList;
    }

    private static void printAbout(Drive drive) throws IOException, NullPointerException {
        About about = drive.about().get().setFields("storageQuota").execute();

        About.StorageQuota storageQuota = about.getStorageQuota();
        System.out.printf(ANSI_BOLD + ANSI_BLUE + "Quota: %.1f/%.1fGB" + ANSI_RESET + '\n',
                (double) storageQuota.getUsage() / 1024 / 1024 / 1024,
                (double) storageQuota.getLimit() / 1024 / 1024 / 1024);
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive drive = getDriveService(HTTP_TRANSPORT);

        printAbout(drive);

        if (args.length == 1 && !args[0].equals("remove")) {
            uploadFile(drive, args[0]);
        } else {
            List<File> files = listFiles(drive);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                if (args.length == 0) {
                    // Download
                    System.out.println(ANSI_BOLD + ANSI_BLUE
                            + "==> Enter MD5(s) to download ('md5 ... md5-md5' specifies a range): " + ANSI_RESET);

                    // if (scanner.hasNextLine()) {
                    String md5Checksum = scanner.nextLine();
                    List<File> dowFileList = md5Preprocessor(files, md5Checksum);
                    downloadFiles(drive, dowFileList);
                    // }
                } else {
                    // Remove
                    System.out.println(
                            ANSI_BOLD + ANSI_RED + "==> Enter MD5(s) to PURGE file(s) (Ctrl-C to exit): " + ANSI_RESET);
                    // if (scanner.hasNextLine()) {
                    String md5Checksum = scanner.nextLine();
                    // }

                }
            }
        }

    }
}