The cluster isn’t fully functional until you initialize the replica sets and add the shards.

Right now, mongos is running, but it won’t know about your shards until you do:

// init-mongo.js

// Wait a bit for mongod/mongos to be up
sleep(10000);

// ----------------------
// Config Server Replica Set
// ----------------------
print(">>> Initiating Config Server Replica Set");
rs.initiate({
_id: "configReplSet",
configsvr: true,
members: [
{ _id: 0, host: "configsvr1:27019" },
{ _id: 1, host: "configsvr2:27020" },
{ _id: 2, host: "configsvr3:27021" }
]
});

// ----------------------
// Shard1
// ----------------------
print(">>> Initiating Shard1 Replica Set");
conn = new Mongo("shard1:27030");
db = conn.getDB("admin");
db.runCommand({
replSetInitiate: {
_id: "shard1",
members: [ { _id: 0, host: "shard1:27030" } ]
}
});

// ----------------------
// Shard2
// ----------------------
print(">>> Initiating Shard2 Replica Set");
conn = new Mongo("shard2:27031");
db = conn.getDB("admin");
db.runCommand({
replSetInitiate: {
_id: "shard2",
members: [ { _id: 0, host: "shard2:27031" } ]
}
});

// ----------------------
// Shard3
// ----------------------
print(">>> Initiating Shard3 Replica Set");
conn = new Mongo("shard3:27032");
db = conn.getDB("admin");
db.runCommand({
replSetInitiate: {
_id: "shard3",
members: [ { _id: 0, host: "shard3:27032" } ]
}
});

// ----------------------
// Mongos Setup
// ----------------------
print(">>> Connecting to Mongos and adding shards");
conn = new Mongo("mongos:27017");
db = conn.getDB("admin");
sh.addShard("shard1/shard1:27030");
sh.addShard("shard2/shard2:27031");
sh.addShard("shard3/shard3:27032");

// Enable sharding for ecommerce DB
print(">>> Enabling sharding for ecommerce DB");
sh.enableSharding("ecommerce");

// Shard the orders collection
print(">>> Sharding ecommerce.orders on orderId (hashed)");
sh.shardCollection("ecommerce.orders", { orderId: "hashed" });

print(">>> Sharding Setup Complete ✅");

## [direct: mongos] test> sh.status()
shardingVersion
{ _id: 1, clusterId: ObjectId('68cc554a26f01bda17749c6f') }
---
shards
[
{
_id: 'shard1',
host: 'shard1/shard1:27030',
state: 1,
topologyTime: Timestamp({ t: 1758221959, i: 1 })
},
{
_id: 'shard2',
host: 'shard2/shard2:27031',
state: 1,
topologyTime: Timestamp({ t: 1758221959, i: 6 })
},
{
_id: 'shard3',
host: 'shard3/shard3:27032',
state: 1,
topologyTime: Timestamp({ t: 1758221959, i: 19 })
}
]
---
active mongoses
[ { '7.0.24': 1 } ]
---
autosplit
{ 'Currently enabled': 'yes' }
---
balancer
{
'Currently enabled': 'yes',
'Currently running': 'no',
'Failed balancer rounds in last 5 attempts': 0,
'Migration Results for the last 24 hours': 'No recent migrations'
}
---
shardedDataDistribution
[
{
ns: 'ecommerce.orders',
shards: [
{
shardName: 'shard3',
numOrphanedDocs: 0,
numOwnedDocuments: 5,
ownedSizeBytes: 635,
orphanedSizeBytes: 0
}
]
},
{
ns: 'config.system.sessions',
shards: [
{
shardName: 'shard1',
numOrphanedDocs: 0,
numOwnedDocuments: 4,
ownedSizeBytes: 396,
orphanedSizeBytes: 0
}
]
}
]
---
databases
[
{
database: { _id: 'config', primary: 'config', partitioned: true },
collections: {
'config.system.sessions': {
shardKey: { _id: 1 },
unique: false,
balancing: true,
chunkMetadata: [ { shard: 'shard1', nChunks: 1 } ],
chunks: [
{ min: { _id: MinKey() }, max: { _id: MaxKey() }, 'on shard': 'shard1', 'last modified': Timestamp({ t: 1, i: 0 }) }
],
tags: []
}
}
},
{
database: {
_id: 'ecommerce',
primary: 'shard3',
partitioned: false,
version: {
uuid: UUID('89b7f743-5e36-4c42-bd68-c75dc8dc0598'),
timestamp: Timestamp({ t: 1758222481, i: 1 }),
lastMod: 1
}
},
collections: {
'ecommerce.orders': {
shardKey: { customerId: 1 },
unique: false,
balancing: true,
chunkMetadata: [ { shard: 'shard3', nChunks: 1 } ],
chunks: [
{ min: { customerId: MinKey() }, max: { customerId: MaxKey() }, 'on shard': 'shard3', 'last modified': Timestamp({ t: 1, i: 0 }) }
],
tags: []
}
}
}
]


## 🔹 Cluster Metadata
shardingVersion
{ _id: 1, clusterId: ObjectId('68cc554a26f01bda17749c6f') }


This is the metadata version of your sharded cluster, stored in the config servers.

clusterId is the unique identifier of this cluster.

🔹 Shards
shards
[
{ _id: 'shard1', host: 'shard1/shard1:27030', state: 1, topologyTime: Timestamp(...) },
{ _id: 'shard2', host: 'shard2/shard2:27031', state: 1, topologyTime: Timestamp(...) },
{ _id: 'shard3', host: 'shard3/shard3:27032', state: 1, topologyTime: Timestamp(...) }
]


You have 3 shards: shard1, shard2, shard3.

state: 1 means they are healthy and part of the cluster.

topologyTime is internal — helps Mongo track changes to the shard’s replica set topology.

🔹 Active Mongos Routers
active mongoses
[ { '7.0.24': 1 } ]


There is 1 mongos router running, version 7.0.24.

Applications connect to this mongos to query the cluster.

🔹 Autosplit
autosplit
{ 'Currently enabled': 'yes' }


Autosplitting is ON, meaning if a chunk grows beyond the configured size (usually 128MB), Mongo can split it automatically.

Important for scaling out.

🔹 Balancer
balancer
{
'Currently enabled': 'yes',
'Currently running': 'no',
'Failed balancer rounds in last 5 attempts': 0,
'Migration Results for the last 24 hours': 'No recent migrations'
}


Balancer enabled = MongoDB can move chunks between shards.

Currently running: no = not migrating right now.

Failed rounds: 0 = no recent errors.

No migrations in last 24h = chunks didn’t need rebalancing.
👉 So the cluster is balanced and healthy.

🔹 Sharded Data Distribution
shardedDataDistribution
[
{ ns: 'ecommerce.orders', shards: [ { shardName: 'shard3', numOwnedDocuments: 5, ... } ] },
{ ns: 'config.system.sessions', shards: [ { shardName: 'shard1', numOwnedDocuments: 4, ... } ] }
]


ns = namespace (database.collection).

ecommerce.orders → all 5 docs are on shard3.

config.system.sessions → 4 docs on shard1 (used internally for sessions).

numOrphanedDocs: 0 → no orphaned documents (good).

This section shows how many documents each shard owns.

🔹 Databases & Collections
databases
[
{
database: { _id: 'config', primary: 'config', partitioned: true },
collections: {
'config.system.sessions': { shardKey: { _id: 1 }, ... }
}
},
{
database: {
_id: 'ecommerce',
primary: 'shard3',
partitioned: false,
version: { uuid: ..., timestamp: ..., lastMod: 1 }
},
collections: {
'ecommerce.orders': {
shardKey: { customerId: 1 },
chunkMetadata: [ { shard: 'shard3', nChunks: 1 } ],
chunks: [
{ min: { customerId: MinKey() }, max: { customerId: MaxKey() }, 'on shard': 'shard3' }
]
}
}
}
]


Two databases in the cluster:

config (internal metadata)

ecommerce (your app DB)

config.system.sessions: sharded by _id. Used internally by drivers for sessions.

ecommerce.orders:

Shard key = { customerId: 1 } (range-based sharding on customerId).

Currently 1 chunk, covering the whole range (MinKey → MaxKey).

That chunk is on shard3.

Since there are only 5 docs, Mongo hasn’t needed to split or balance yet.

🔹 TL;DR — What this Means for You

Cluster has 3 shards, all healthy.

Mongos router is running and connected.

Balancer is enabled but idle (no need to move chunks).

ecommerce.orders collection is sharded on customerId, but so far all data sits on shard3 (because small volume, only 1 chunk).

As data grows, autosplit will create more chunks, and the balancer will move them across shards.

👉 Right now, your cluster is healthy but under-utilized — the sharding mechanism is set up, but until your orders collection grows larger, you won’t see chunks spread across shard1 and shard2.

Do you want me to show you how to force chunk splits (e.g. with sh.splitAt or sh.splitFind) so you can actually see data moving across shards?


# All your CRUD operations in mongo-express go through mongos.

You can open the config database inside mongo-express and inspect:

config.shards → list of shards

config.databases → db metadata

config.collections → shard keys, balancing info

config.chunks → chunk ranges per shard

That’s basically the raw JSON version of what sh.status() prints.

🔹 Example Queries you can run in mongo-express (from the query tab)

See shards registered:

db.getSiblingDB("config").shards.find().pretty()


See which collections are sharded:

db.getSiblingDB("config").collections.find().pretty()


Check chunk distribution for your orders collection:

db.getSiblingDB("config").chunks.find({ ns: "ecommerce.orders" }).pretty()

🔹 What you won’t get in mongo-express

You can’t directly run admin helpers like sh.status(), sh.splitAt(), or sh.moveChunk() because those are mongosh helpers, not raw MongoDB driver commands.

mongo-express doesn’t visualize balancing, orphaned docs, or chunk migrations. You’d have to read those from config collections manually.

✅ So the answer: yes, with ME_CONFIG_MONGODB_SERVER: mongos you can monitor shards, config servers, and balancer state from mongo-express — but you’ll see the raw metadata, not a nice dashboard.