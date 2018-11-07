package com.bsycorp.kees.storage;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.bsycorp.kees.Utils;
import com.bsycorp.kees.models.Parameter;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoDBStorageProvider implements StorageProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DynamoDBStorageProvider.class);

    public final String  tableName;
    private final AmazonDynamoDB client = Utils.getDDBClient();

    public DynamoDBStorageProvider(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void put(String storagePrefix, Parameter key, String value) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Setting DDB item for key: {}", ssmPath);

        try {
            Map<String, AttributeValue> values = new HashMap<>();
            values.put("secretName", new AttributeValue(ssmPath));
            values.put("secretValue", new AttributeValue(value));
            client.putItem(
                    new PutItemRequest()
                            .withTableName(tableName)
                            .withItem(values)
                            //do this to mimic default SSM behaviour where it won't overwrite
                            .withConditionExpression("attribute_not_exists(secretName)")
            );
            //success!
            LOG.info("Set DDB parameter and value for key: {}", ssmPath);

        } catch (ConditionalCheckFailedException ce){
            //item already exists
            LOG.warn("Item already exists for key, ignoring new value: {}", ssmPath);

        } catch (AmazonDynamoDBException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when setting item for key: " + ssmPath, e);
        }

    }

    @Override
    public String get(String storagePrefix, Parameter key) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Looking up DDB value for key: {}", ssmPath);

        try {
            Map<String, AttributeValue> itemKey = new HashMap<>();
            itemKey.put("secretName", new AttributeValue(ssmPath));
            GetItemResult result = client.getItem(new GetItemRequest().withTableName(tableName).withKey(itemKey));
            if (result.getItem() == null){
                LOG.warn("Couldn't find item for key: {}", ssmPath);
                return null;
            }
            return result.getItem().get("secretValue").getS();

        } catch (ResourceNotFoundException e) {
            LOG.warn("Couldn't find item for key: {}", ssmPath);
            return null;

        } catch (AmazonDynamoDBException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when looking up parameter with key: " + ssmPath, e);
            return null;
        }
    }

    @Override
    public boolean exists(String storagePrefix, Parameter key) {
        String ssmPath = key.getStorageFullPath(storagePrefix);
        LOG.info("Checking DDB value exists for key: {}", ssmPath);

        try {
            Map<String, AttributeValue> eav = new HashMap<>();
            eav.put(":name", new AttributeValue().withS(ssmPath));

            QueryResult result = client.query(
                new QueryRequest().withTableName(tableName)
                    .withKeyConditionExpression("secretName =:name")
                    .withExpressionAttributeValues(eav)
                    .withProjectionExpression("secretName")
            );
            return result.getCount().intValue() > 0;

        } catch (AmazonDynamoDBException e) {
            //had error finding value, could be missing or invalid or error
            LOG.error("Error when looking up parameter with key: " + ssmPath, e);
            return false;
        }
    }
}
