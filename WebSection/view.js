const express = require('express');
const cors = require('cors');
const { MongoClient } = require('mongodb');

const app = express();
// Use CORS middleware
app.use(cors());
const url = 'mongodb://localhost:27017';
const dbName = 'local';
const collectionName = 'tweets';

async function getTopUsers() {
  let client;
  try {
    console.log('Attempting to connect to MongoDB...');
    client = await MongoClient.connect(url, { useNewUrlParser: true, useUnifiedTopology: true });
    console.log('Connected to MongoDB successfully.');

    const db = client.db(dbName);
    const collection = db.collection(collectionName);

    console.log('Retrieving top users...');
    const result = await collection.aggregate([
      { $group: { _id: '$user', count: { $sum: 1 } } },
      { $sort: { count: -1 } },
      { $limit: 20 }
    ]).toArray();

    return result;
  } catch (err) {
    console.error('Error:', err);
    throw err;
  } finally {
    if (client) {
      client.close();
      console.log('MongoDB connection closed.');
    }
  }
}

app.get('/getTopUsers', async (req, res) => {
  try {
    const topUsers = await getTopUsers();
    res.json(topUsers);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});

