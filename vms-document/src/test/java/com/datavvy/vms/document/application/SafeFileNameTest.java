package com.datavvy.vms.document.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.datavvy.vms.document.domain.FileKind;
import org.junit.jupiter.api.Test;

class SafeFileNameTest {

    @Test
    void dropsPathsAndOddCharactersAndUsesTheDetectedExtension() {
        assertThat(DocumentService.safeFileName("C:\\scans\\tax token.PDF", FileKind.PDF)).isEqualTo("tax token.pdf");
        assertThat(DocumentService.safeFileName("../../etc/passwd", FileKind.PDF)).isEqualTo("passwd.pdf");
        assertThat(DocumentService.safeFileName("photo.pdf", FileKind.JPEG)).isEqualTo("photo.jpg");
        assertThat(DocumentService.safeFileName("<script>.png", FileKind.PNG)).isEqualTo("_script_.png");
        assertThat(DocumentService.safeFileName(null, FileKind.WEBP)).isEqualTo("document.webp");
    }
}
