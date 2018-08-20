import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;

public class FileDownloadProgressListener implements MediaHttpDownloaderProgressListener {
    long size;

    FileDownloadProgressListener(long size) {
        this.size = size;
    }

    public void progressChanged(MediaHttpDownloader downloader) {
        switch (downloader.getDownloadState()) {
        case MEDIA_IN_PROGRESS:
            Global.updateProgress(downloader.getProgress(), size);
            break;
        case MEDIA_COMPLETE:
            Global.updateProgress(downloader.getProgress(), size);
            System.out.print('\n');
        }
    }
}
