package com.laraid.ecommerce.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoShardingInitializer {

    private static final String DATABASE_NAME = "ecommerce";
    private static final String COLLECTION_NAME = "orders";
    private static final String SHARD_KEY = "customerId";

    @Bean
    CommandLineRunner initSharding(MongoClient mongoClient) {
        return args -> {
            MongoDatabase adminDb = mongoClient.getDatabase("admin");

            // 1. Enable sharding on the database
            Document enableShardingCmd = new Document("enableSharding", DATABASE_NAME);
            try {
                adminDb.runCommand(enableShardingCmd);
                System.out.println("✅ Sharding enabled for database: " + DATABASE_NAME);
            } catch (Exception e) {
                System.out.println("⚠️ Sharding already enabled for DB or error: " + e.getMessage());
            }

            // 2. Shard the collection on the shard key
            Document shardCollectionCmd = new Document("shardCollection", DATABASE_NAME + "." + COLLECTION_NAME)
                    .append("key", new Document(SHARD_KEY, 1)); // 1 = ascending
            try {
                adminDb.runCommand(shardCollectionCmd);
                System.out.println("✅ Collection sharded: " + COLLECTION_NAME + " on key: " + SHARD_KEY);
            } catch (Exception e) {
                System.out.println("⚠️ ShardCollection already exists or error: " + e.getMessage());
            }
        };
    }
}

