package com.bsycorp.kees.models;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SecretParameterTest {

    //SSM Path
    @Test
    public void shouldGiveCorrectSSMPath() throws IOException {
        SecretParameter parameter = (SecretParameter) Parameter.construct("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=RANDOM,size=128");
        assertEquals("/storage/prefix/service-a.api-key", parameter.getStorageFullPath("/storage/prefix/"));
    }

    @Test
    public void shouldGiveCorrectSSMPathWithStorageKey() throws IOException {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=RANDOM,size=128,storageKey=app.key");
        assertEquals("/storage/smth/app.key", parameter.getStorageFullPath("/storage/smth/"));
    }

    //DYNAMIC
    @Test
    public void shouldParseDynamicRandom128Annotation() throws Exception {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=RANDOM,size=128");

        assertEquals("DYNAMIC", parameter.getKind().name());
        assertEquals("RANDOM", parameter.getType().name());
        assertEquals(128, parameter.getSize());
    }

    @Test
    public void shouldParseDynamicRandom256Annotation() throws Exception {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=RANDOM,size=256");

        assertEquals("DYNAMIC", parameter.getKind().name());
        assertEquals("RANDOM", parameter.getType().name());
        assertEquals(256, parameter.getSize());
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotParseDynamicRandom128Annotation() throws Exception {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=RANDOM");
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotParseDynamicRandomLargeAnnotation() throws Exception {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=RANDOM,size=1230918");
    }

    //RSA
    @Test
    public void shouldParseDynamicRSA2048Annotation() throws Exception {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=RSA,size=2048");

        assertEquals("DYNAMIC", parameter.getKind().name());
        assertEquals("RSA", parameter.getType().name());
        assertEquals(2048, parameter.getSize());
    }

    @Test
    public void shouldParseDynamicRSA4096Annotation() throws Exception {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=RSA,size=4096");

        assertEquals("DYNAMIC", parameter.getKind().name());
        assertEquals("RSA", parameter.getType().name());
        assertEquals(4096, parameter.getSize());
    }

    @Test
    public void shouldParseDynamicGPG4096Annotation() throws Exception {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=GPG,size=4096,userId=aaaa<aaaa@email.com>");

        assertEquals("DYNAMIC", parameter.getKind().name());
        assertEquals("GPG", parameter.getType().name());
        assertEquals(4096, parameter.getSize());
        assertEquals("aaaa<aaaa@email.com>", parameter.getUserId());
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailIfGPGAnnotationMissingUserId() throws IOException {
        new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=GPG,size=4096");
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailIfGPGAnnotationUserIdInvalidFormat() throws IOException {
        new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=GPG,size=4096,userId=aaaa@email.com");
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotParseDynamicRSA12048Annotation() throws Exception {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/service-a.api-key", "kind=DYNAMIC,type=RSA,size=12048");
    }

    //REFERENCE
    @Test
    public void shouldParseReferencePasswordAnnotation() throws Exception {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/app.db", "kind=REFERENCE,type=PASSWORD");

        assertEquals("REFERENCE", parameter.getKind().name());
        assertEquals("PASSWORD", parameter.getType().name());
        assertEquals(null, parameter.getLocalValue());
    }

    @Test
    public void shouldParseReferencePasswordWithLocalModeValueAnnotation() throws Exception {
        SecretParameter parameter = new SecretParameter("secret.bsycorp.com/app.db", "kind=REFERENCE,type=PASSWORD,localModeValue=cGFzc3dvcmQ=");

        assertEquals("REFERENCE", parameter.getKind().name());
        assertEquals("PASSWORD", parameter.getType().name());
        assertEquals("cGFzc3dvcmQ=", parameter.getLocalValue());
    }

}