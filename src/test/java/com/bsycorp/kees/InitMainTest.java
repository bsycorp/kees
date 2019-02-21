package com.bsycorp.kees;

import java.io.ByteArrayInputStream;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class InitMainTest {

    @ClassRule
    public static final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private static File tempAnnotationsFile;
    private static File tempSecretsFile;
    private static File tempResourcesFile;
    private static String tempResourcesFilePath;

    private void init(String originalAnnotationFileName) throws Exception {
        String originalAnnotationFilePath = InitMainTest.class.getClassLoader().getResource(originalAnnotationFileName).getFile();
        tempAnnotationsFile = File.createTempFile("annotations", ".txt");
        tempAnnotationsFile.deleteOnExit();

        tempSecretsFile = File.createTempFile("secrets", ".properties");
        tempSecretsFile.deleteOnExit();

        tempResourcesFile = File.createTempFile("resources", ".properties");
        tempResourcesFile.deleteOnExit();

        tempResourcesFilePath = tempResourcesFile.getParentFile().getPath();

        environmentVariables.set("ANNOTATIONS_FILE", tempAnnotationsFile.getAbsolutePath());
        FileUtils.copyFile(new File(originalAnnotationFilePath), tempAnnotationsFile);

        environmentVariables.set("SECRETS_FILE", tempSecretsFile.getAbsolutePath());
        environmentVariables.set("RESOURCES_FILE", tempResourcesFile.getAbsolutePath());
        environmentVariables.set("RESOURCES_FILE_PATH", tempResourcesFilePath);
        environmentVariables.set("ENV_LABEL", "sml-place");

        //overwrite to be sure it hasn't been changed
        FileUtils.copyFile(new File(originalAnnotationFilePath), tempAnnotationsFile);
        //delete for consistency
        tempSecretsFile.delete();
        tempResourcesFile.delete();
    }

    @Test
    public void shouldBeSuccessful() throws Exception {
        init("annotations.txt");

        InitMain.main();

        String expectedSecrets = FileUtils.readFileToString(new File(this.getClass().getClassLoader().getResource("expected-secrets.properties").getFile()), "UTF-8");
        assertEquals(expectedSecrets, FileUtils.readFileToString(tempSecretsFile, "UTF-8"));

        String expectedResources = FileUtils.readFileToString(new File(this.getClass().getClassLoader().getResource("expected-resources.properties").getFile()), "UTF-8");
        assertEquals(expectedResources, FileUtils.readFileToString(tempResourcesFile, "UTF-8"));

        String expectedDBResource = FileUtils.readFileToString(new File(this.getClass().getClassLoader().getResource("expected-app.db.main.url").getFile()), "UTF-8");
        assertEquals(expectedDBResource, FileUtils.readFileToString(new File(tempResourcesFilePath + "/app.db.main.url"), "UTF-8"));

    }

    /*
     * Kind of hard to test as for deterministic GPG keys, we get different armored outputs (The underlying RSA keys are the same).
     * Test not null for private and public key. Password output is deterministic.
     */
    @Test
    public void shouldBeSuccessfulForGPGAnnotations() throws Exception {
        init("annotations-gpg.txt");

        InitMain.main();

        final String result = FileUtils.readFileToString(tempSecretsFile, "UTF-8");
        final Properties props = new Properties();
        props.load(new ByteArrayInputStream(result.getBytes()));

        Assert.assertNotNull(props.getProperty("common.gpg.v1_private"));
        Assert.assertNotNull(props.getProperty("common.gpg.v1_public"));
        Assert.assertEquals("SmRGbU5tSUh0MjhZc3RXcQ==", props.getProperty("common.gpg.v1_password"));
    }

    @Test(expected = Exception.class)
    public void shouldBeUnsuccessful() throws Exception {
        FileUtils.copyFile(new File(InitMainTest.class.getClassLoader().getResource("annotations-malformed.txt").getFile()), tempAnnotationsFile);
        InitMain.main();
    }

    @Test
    public void testConfigGetters() throws Exception {
        InitMain initMain = new InitMain();

        String annotations = "init.bsycorp.com/local-mode: \"true\"";
        annotations += "\n";
        annotations += "init.bsycorp.com/storage-prefix: \"/bsycorp/staging\"";

        File annotationsFile = File.createTempFile("annotations", ".properties");
        annotationsFile.deleteOnExit();
        FileUtils.writeStringToFile(annotationsFile, annotations, Charset.defaultCharset());

        environmentVariables.set("ANNOTATIONS_FILE", annotationsFile.getAbsolutePath());

        assertEquals(annotationsFile.getAbsolutePath(), initMain.getAnnotationsFile().getAbsolutePath());
        assertEquals(true, initMain.isLocalMode());
        assertEquals("/bsycorp/staging", new InitMain().getStoragePrefix());
    }
}