package com.datavvy.vms.document.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FileKindTest {

    @Test
    void recognisesAcceptedFormatsByTheirFirstBytes() {
        assertThat(FileKind.detect("%PDF-1.7".getBytes(StandardCharsets.US_ASCII))).contains(FileKind.PDF);
        assertThat(FileKind.detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0})).contains(FileKind.JPEG);
        assertThat(FileKind.detect(new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10})).contains(FileKind.PNG);
        assertThat(FileKind.detect("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII))).contains(FileKind.WEBP);
    }

    @Test
    void rejectsEverythingElse() {
        assertThat(FileKind.detect("MZ\u0090\0executable".getBytes(StandardCharsets.ISO_8859_1))).isEmpty();
        assertThat(FileKind.detect("<html>".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(FileKind.detect(new byte[] {1, 2})).isEmpty();
        assertThat(FileKind.detect(null)).isEmpty();
    }
}
