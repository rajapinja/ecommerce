from pymongo import MongoClient
import json

def print_chunk_map():
    client = MongoClient("mongodb://localhost:27017")  # adjust if mongos exposed differently
    config_db = client["config"]

    chunks = config_db["chunks"].find(
        {"ns": "ecommerce.orders"}
    ).sort("min.customerId", 1)

    print("Chunk distribution for ecommerce.orders")
    print("=" * 80)
    print("| Range (min → max)                           | Shard   |")
    print("-" * 80)

    for c in chunks:
        min_val = json.dumps(c["min"]["customerId"])
        max_val = json.dumps(c["max"]["customerId"])
        print(f"| {min_val} → {max_val} | {c['shard']}")

    print("=" * 80)

if __name__ == "__main__":
    print_chunk_map()


# python chunk_map.py
##3️⃣ Example Output (both versions)
#   Chunk distribution for ecommerce.orders
#   ================================================================================
#   | Range (min → max)                           | Shard   |
#   --------------------------------------------------------------------------------
#   | "MinKey" → "-5000000000000000000"           | shard1  |
#   | "-5000000000000000000" → "0"                | shard2  |
#   | "0" → "MaxKey"                              | shard3  |
#   ================================================================================