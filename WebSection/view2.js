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

app.get('/getUserTweetsByDate', async (req, res) => {
  const { username } = req.query;

  try {
    console.log(`Fetching tweets for user: ${username}...`);
    const client = await MongoClient.connect(url, { useNewUrlParser: true, useUnifiedTopology: true });
    const db = client.db(dbName);
    const collection = db.collection(collectionName);

    const userTweetsByDate = await collection.aggregate([
      {
        $match: { user: username }
      },
      {
        $group: {
          _id: {
            year: { $year: { $toDate: "$date" } },
            month: { $month: { $toDate: "$date" } },
            day: { $dayOfMonth: { $toDate: "$date" } }
          },
          count: { $sum: 1 }
        }
      },
      {
        $project: {
          _id: 0,
          year: "$_id.year",
          month: "$_id.month",
          day: "$_id.day",
          count: 1
        }
      }
    ]).toArray();

    client.close();
    console.log(`Fetched ${userTweetsByDate.length} tweets for user: ${username}`);

    res.json(userTweetsByDate);
  } catch (err) {
    console.error('Error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

const PORT = 3001;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
