package com.datavvy.vms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

/**
 * Guards "no credentials in source control": the shipped configuration must
 * take every secret from the environment, with no fallback value a forgotten
 * variable would silently fall back to.
 */
class ConfigHygieneTest {

    private static final List<String> SECRETS =
            List.of("VMS_DB_URL", "VMS_DB_USER", "VMS_DB_PASSWORD", "VMS_JWT_SECRET");

    @Test
    void baseConfigReadsEverySecretFromTheEnvironmentWithoutAFallback() throws IOException {
        String yml = read("application.yml");
        for (String name : SECRETS) {
            assertThat(yml).as("%s must be read from the environment", name).contains("${" + name + "}");
            assertThat(yml).as("%s must not have a fallback value", name).doesNotContain("${" + name + ":");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"application.yml", "application-local.yml", "application-demo.yml", "application-h2.yml"})
    void noProfileHardCodesAPasswordOrSigningKey(String file) throws IOException {
        // `password:` may be empty (embedded H2) or a ${placeholder}; never a literal.
        var literalPassword = Pattern.compile("(?m)^\\s*password:[ \\t]*(?!\\$\\{)\\S+");
        var literalJwtSecret = Pattern.compile("(?m)^\\s*jwt-secret:[ \\t]*(?!\\$\\{)\\S+");
        String yml = read(file);
        assertThat(literalPassword.matcher(yml).find()).as("%s has a literal password", file).isFalse();
        assertThat(literalJwtSecret.matcher(yml).find()).as("%s has a literal JWT secret", file).isFalse();
    }

    private static String read(String file) throws IOException {
        return new ClassPathResource(file).getContentAsString(StandardCharsets.UTF_8);
    }
}
