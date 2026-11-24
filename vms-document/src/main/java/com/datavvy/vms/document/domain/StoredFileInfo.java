package com.datavvy.vms.document.domain;

/** File metadata kept on the document row. {@code contentType} is detected from the bytes, not trusted from the client. */
public record StoredFileInfo(String fileName, String contentType, long sizeBytes) {
}
