// MongoDB initialization script
// Creates the two logical databases with initial collections

// Switch to logs_db
db = db.getSiblingDB("logs_db");

// Create collections for logs database
db.createCollection("api_logs");
db.createCollection("rate_limit_events");

// Create indexes for better query performance
db.api_logs.createIndex({ timestamp: -1 });
db.api_logs.createIndex({ serviceName: 1, timestamp: -1 });
db.api_logs.createIndex({ endpoint: 1, timestamp: -1 });
db.api_logs.createIndex({ statusCode: 1 });
db.api_logs.createIndex({ latency: -1 });

db.rate_limit_events.createIndex({ timestamp: -1 });
db.rate_limit_events.createIndex({ serviceName: 1, timestamp: -1 });

print("logs_db initialized successfully");

// Switch to meta_db
db = db.getSiblingDB("meta_db");

// Create collections for metadata database
db.createCollection("users");
db.createCollection("incidents");
db.createCollection("alerts");
db.createCollection("rate_limiter_configs");

// Create indexes
db.users.createIndex({ username: 1 }, { unique: true });
db.users.createIndex({ email: 1 }, { unique: true });

db.incidents.createIndex({ endpoint: 1, serviceName: 1 });
db.incidents.createIndex({ status: 1 });
db.incidents.createIndex({ createdAt: -1 });

db.alerts.createIndex({ timestamp: -1 });
db.alerts.createIndex({ type: 1, timestamp: -1 });
db.alerts.createIndex({ acknowledged: 1 });

db.rate_limiter_configs.createIndex({ serviceName: 1 }, { unique: true });

// Insert default demo user (password: demo123)
// BCrypt hash for 'demo123'
db.users.insertOne({
  username: "demo",
  email: "demo@leapfinance.com",
  password: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3mV8xYqYvKBvNqOUJE2y",
  role: "ADMIN",
  createdAt: new Date(),
  updatedAt: new Date(),
});

print("meta_db initialized successfully");
print("Demo user created - username: demo, password: demo123");
