package org.jboss.xavier.sanitizer;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class XavierPayloadSanitizerTest {

    @Test
    public void testVMsHost() throws URISyntaxException, IOException {
        // This works
//        final List<String> inputFileNames = Arrays.asList("vm_with_null_host.json");

        // This does not work
        final List<String> inputFileNames = Arrays.asList("vm_with_null_host.json", "vm_without_host.json", "vm_with_empty_host.json");


        final String issuesFileName = "issues_conditions.json";
        URL issuesURL = Thread.currentThread().getContextClassLoader().getResource(issuesFileName);
        Assert.assertNotNull(issuesFileName + " not found", issuesURL);
        String issues = new File(issuesURL.toURI()).getAbsolutePath();

        for (String inputFileName : inputFileNames) {
            URL inputURL = Thread.currentThread().getContextClassLoader().getResource(inputFileName);
            Assert.assertNotNull(inputFileName + " not found", inputURL);
            String input = new File(inputURL.toURI()).getAbsolutePath();


            String[] args = {"--input", input, "--issues", issues};
            XavierPayloadSanitizer.main(args);


            Path sanitizedFilePath = Paths.get(input).resolveSibling(inputFileName.replace(".json", "_sanitized.json"));
            Assert.assertTrue("Sanitized file not found for input=" + inputFileName, Files.exists(sanitizedFilePath));

            String sanitizedFileContent = new String(Files.readAllBytes(sanitizedFilePath), StandardCharsets.UTF_8);
            Assert.assertTrue("input=" + inputFileName + " should contain 'retired'", sanitizedFileContent.contains("retired"));
        }
    }
}
