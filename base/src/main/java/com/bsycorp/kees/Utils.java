package com.bsycorp.kees;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static Map<String, String> environment = null;
    public static SdkHttpClient httpClient = null;

    public static void setupProxyProperties() {
        if (System.getProperty("https.proxyHost") == null) {
            String httpsProxy = getEnvironment().get("HTTPS_PROXY");
            if (httpsProxy!=null) {
                try {
                    URI uri = new URI(httpsProxy);
                    System.setProperty("https.proxyHost", uri.getHost());
                    System.setProperty("https.proxyPort", uri.getPort() + "");
                    LOG.info("Configured JVM proxy variables from HTTPS_PROXY: {}", httpsProxy);

                } catch (URISyntaxException e) {
                    LOG.warn("Invalid proxy value configured: " + httpsProxy, e);
                }
            }
        }

        if (System.getProperty("http.proxyHost") == null) {
            String httpProxy = getEnvironment().get("HTTP_PROXY");
            if (httpProxy!=null) {
                try {
                    URI uri = new URI(httpProxy);
                    System.setProperty("http.proxyHost", uri.getHost());
                    System.setProperty("http.proxyPort", uri.getPort() + "");
                    LOG.info("Configured JVM proxy variables from HTTP_PROXY: {}", httpProxy);

                } catch (URISyntaxException e) {
                    LOG.warn("Invalid proxy value configured: " + httpProxy, e);
                }
            }
        }
    }

    public static SdkHttpClient getSdkHttpClient() {
        if (httpClient!=null) {
            return httpClient;
        }
        httpClient = UrlConnectionHttpClient.builder()
                .build();
        return httpClient;
    }

    public static AwsCredentialsProvider getCredentialsProvider() {
        String stsEndpoint = getEnvironment().get("AWS_STS_ENDPOINT");
        String stsTokenFile = getEnvironment().get("AWS_WEB_IDENTITY_TOKEN_FILE");

        if (stsEndpoint != null && stsTokenFile != null) {
            try {
                AwsCredentialsProvider provider = WebIdentityTokenFileCredentialsProvider.create();
                Field credentialsProviderField =
                        provider.getClass().getDeclaredField("credentialsProvider");
                credentialsProviderField.setAccessible(true);
                Object factory = credentialsProviderField.get(provider);
                Field stsClientField = factory.getClass().getDeclaredField("stsClient");
                stsClientField.setAccessible(true);

                Field nestedCredentialsProviderField =
                        factory.getClass().getDeclaredField("credentialsProvider");
                nestedCredentialsProviderField.setAccessible(true);
                Object nestedCredentialProvider = nestedCredentialsProviderField.get(factory);
                Field nestedStsClientField =
                        nestedCredentialProvider
                                .getClass()
                                .getSuperclass()
                                .getDeclaredField("stsClient");
                nestedStsClientField.setAccessible(true);

                StsClient stsClient =
                        StsClient.builder()
                                .region(Region.US_EAST_1)
                                .credentialsProvider(AnonymousCredentialsProvider.create())
                                .endpointOverride(new URI(stsEndpoint))
                                .httpClient(getSdkHttpClient())
                                .build();

                stsClientField.set(factory, stsClient);
                nestedStsClientField.set(nestedCredentialProvider, stsClient);
                LOG.info("Using WebIdentityTokenFileCredentialsProvider with custom STS endpoint");
                return provider;

            } catch (Exception e) {
                LOG.warn("Failed to create provider with custom endpoint", e);
            }
        }

        return DefaultCredentialsProvider.create();
    }

    public static SsmClient getSSMClient() {
        String awsEndpoint = getEnvironment().get("AWS_ENDPOINT");
        URI overrideEndpoint = null;
        try {
            overrideEndpoint = new URI(awsEndpoint);
        } catch (URISyntaxException e) {
            LOG.warn("Ignoring invalid endpoint: {}", awsEndpoint);
        }

        SsmClientBuilder builder = SsmClient.builder();
        builder.credentialsProvider(getCredentialsProvider());
        if (awsEndpoint!=null) {
            builder.endpointOverride(overrideEndpoint);
        } else {
            builder.region(getCloudRegion());
        }
        return builder.build();
    }

    public static DynamoDbClient getDDBClient() {
        String awsEndpoint = getEnvironment().get("AWS_ENDPOINT");

        DynamoDbClientBuilder builder = DynamoDbClient.builder();
        builder.credentialsProvider(getCredentialsProvider());
        if (awsEndpoint != null) {
            try {
                builder.endpointOverride(new URI(awsEndpoint));
            } catch (URISyntaxException e) {
                LOG.warn("Ignoring invalid endpoint: {}", awsEndpoint);
                builder.region(getCloudRegion());
            }
        } else {
            builder.region(getCloudRegion());
        }
        return builder.build();
    }

    public static String getTableName(String clusterLabel) {
        return clusterLabel + "-kube-secret";
    }

    public static String getAnnotationDomain() {
        String value = getEnvironment().get("ANNOTATION_DOMAIN");
        if (value==null) {
            value = "bsycorp.com";
        }
        return value;
    }

    public static Region getCloudRegion() {
        String value = getEnvironment().get("AWS_REGION");
        if (value==null) {
            value = "ap-southeast-2";
        }
        return Region.of(value);
    }

    public static void setEnvironment(Map<String, String> overrideEnvironment) {
        environment = overrideEnvironment;
    }

    public static Map<String, String> getEnvironment() {
        if (environment==null) {
            environment = System.getenv();
        }
        return environment;
    }

}
