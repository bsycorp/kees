package com.bsycorp.kees;

import static org.junit.Assert.assertEquals;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import java.io.File;

public class ExportMainTest {

    @Test(expected = Exception.class)
    public void shouldntStartWithNothing() throws Exception {
        ExportMain.main();
    }

    @Test(expected = Exception.class)
    public void shouldntStartWithInputAndNoName() throws Exception {
        String manifestPath = this.getClass().getClassLoader().getResource("manifest.yml").getFile();
        ExportMain.main("-i", manifestPath);
    }

    @Test
    public void shouldStartWithInputAndName() throws Exception {
        String manifestPath = this.getClass().getClassLoader().getResource("manifest.yml").getFile();
        String templatePath = this.getClass().getClassLoader().getResource("terraform-secrets.hbs").getFile();
        ExportMain.main("-i", manifestPath, "-m", "service-c", "-t", templatePath);
    }

    @Test
    public void shouldStartWithInputAndNameAndOutput() throws Exception {
        String templatePath = this.getClass().getClassLoader().getResource("terraform-secrets.hbs").getFile();
        String manifestPath = this.getClass().getClassLoader().getResource("manifest.yml").getFile();
        File outputFile = File.createTempFile("output", ".tf");
        outputFile.deleteOnExit();
        String outputPath = outputFile.getAbsolutePath();
        ExportMain.main("-i", manifestPath, "-m", "service-c", "-o", outputPath, "-t", templatePath);

        String outputContent = FileUtils.readFileToString(new File(outputPath), "UTF-8");

        String expectedResultPath = this.getClass().getClassLoader().getResource("expected-terraform.tf").getFile();
        String expectedResultContent = FileUtils.readFileToString(new File(expectedResultPath), "UTF-8");
        assertEquals(expectedResultContent, outputContent);
    }
}