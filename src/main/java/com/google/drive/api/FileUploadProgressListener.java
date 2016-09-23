/**
 * Created by Ryan7WU on 9/23/16.
 */
package com.google.drive.api;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;

import java.io.IOException;

public class FileUploadProgressListener implements MediaHttpUploaderProgressListener {
    String fileName;
    java.io.File mediaFile;
    FileUploadProgressListener(String fileName, java.io.File mediaFile) {
        this.fileName = fileName;
        this.mediaFile = mediaFile;
    }

    public void progressChanged(MediaHttpUploader uploader) throws IOException {
        switch (uploader.getUploadState()) {
            case INITIATION_STARTED:
                //System.out.println("Initiation has started!");
                break;
            case INITIATION_COMPLETE:
                System.out.println(Global.ANSI_BOLD + Global.ANSI_BLUE + "==> " + Global.ANSI_BLACK + "Uploading " + fileName + Global.ANSI_RESET);
                //System.out.println("Initiation is complete!");
                break;
            case MEDIA_IN_PROGRESS:
                Global.updateProgress(uploader.getProgress(), mediaFile.length());
                break;
            case MEDIA_COMPLETE:
                Global.updateProgress(uploader.getProgress(), mediaFile.length());
                //System.out.println(Global.ANSI_BOLD + Global.ANSI_BLUE + "\n==>" + Global.ANSI_GREEN + " Upload is complete!" + Global.ANSI_RESET);
        }
    }
}
