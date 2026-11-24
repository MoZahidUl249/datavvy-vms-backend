package com.datavvy.vms.document.application;

/** A stored file ready to stream back to the browser. */
public record DownloadableFile(String fileName, String contentType, byte[] content) {
}
