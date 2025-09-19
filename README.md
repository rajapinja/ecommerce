# ecommerce
🛒 E-Commerce with MongoDB Sharding

This project demonstrates an e-commerce platform backed by MongoDB Sharding for scalable, high-performance data management.
Domain: Orders stored in MongoDB with schema defined via Spring Boot.

Sharding Strategy:
Collection: ecommerce.orders
Shard Key: customerId (hashed) → ensures even distribution of customer data across shards.

Cluster Setup:
3 Shards (shard1, shard2, shard3)
Config Server Replica Set (configReplSet)
Mongos Router for query routing
Data Distribution: Orders are automatically split into chunks and mapped to shards. Balancer ensures even spread across the cluster.

Monitoring:
Scripts available (Node.js & Python) to query config.chunks and show live chunk → shard distribution.
Mongo Express configured via Docker for admin/monitoring UI.

If you want true visibility into shards, chunks, and balancer activity:
Mongosh → Run commands like sh.status(), db.printShardingStatus(), db.getSiblingDB("config").chunks.find().pretty().
MongoDB Atlas UI (if using Atlas) → has visual shard/chunk monitoring.
Ops Manager / Cloud Manager → full monitoring suite.
Custom Scripts → your Node.js/Python chunk map tools (from earlier).

Mongo Express is fine for document-level inspection and CRUD.
But for cluster-level monitoring (config servers, shard distribution, chunks) you’ll need mongosh commands or custom monitoring scripts.

✅ Why Sharding?
As customer and order volume grows, sharding horizontally scales the database, avoids single-node bottlenecks, and ensures queries remain performant.

