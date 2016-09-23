/**
 * Created by Ryan7WU on 9/23/16.
 */
package com.google.drive.api;

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
                //System.out.println(Global.ANSI_BOLD + Global.ANSI_BLUE + "\n==>" + Global.ANSI_GREEN + " Download is complete!" + Global.ANSI_RESET);
        }
    }
}
