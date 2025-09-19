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
