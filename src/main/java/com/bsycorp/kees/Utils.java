package com.bsycorp.kees;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static final String HTTPS_PROXY = "https_proxy";

    public static AWSSimpleSystemsManagement getSSMClient() {
        String proxyConfig = System.getProperty(HTTPS_PROXY);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        if (proxyConfig == null) {
            proxyConfig = System.getenv(HTTPS_PROXY);
        }

        if (proxyConfig != null) {
            LOG.info("Configuring SSM client with proxy {}", proxyConfig);
            String proxyHost = proxyConfig.split(":")[0];
            String proxyPort = proxyConfig.split(":")[1];
            clientConfiguration.withProxyHost(proxyHost).withProxyPort(Integer.parseInt(proxyPort));
        }

        return AWSSimpleSystemsManagementClientBuilder.standard().withClientConfiguration(clientConfiguration).withRegion(getCloudRegion()).build();
    }

    public static AmazonDynamoDB getDDBClient(){
        String proxyConfig = System.getProperty(HTTPS_PROXY);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        if (proxyConfig == null) {
            proxyConfig = System.getenv(HTTPS_PROXY);
        }

        if (proxyConfig != null) {
            LOG.info("Configuring DynamoDB client with proxy {}", proxyConfig);
            String proxyHost = proxyConfig.split(":")[0];
            String proxyPort = proxyConfig.split(":")[1];
            clientConfiguration.withProxyHost(proxyHost).withProxyPort(Integer.parseInt(proxyPort));
        }

        return AmazonDynamoDBClientBuilder.standard().withClientConfiguration(clientConfiguration).withRegion(getCloudRegion()).build();
    }

    public static String getTableName(String clusterLabel){
        return clusterLabel + "-kube-secret";
    }

    public static String getAnnotationDomain() {
        String value = System.getenv("ANNOTATION_DOMAIN");
        if (value == null) {
            value = "bsycorp.com";
        }
        return value;
    }

    public static String getCloudRegion(){
        String value = System.getenv("AWS_REGION");
        if (value == null) {
            value = "ap-southeast-2";
        }
        return value;
    }

}
