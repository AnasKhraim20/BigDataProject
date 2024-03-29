import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.mongodb.scala._
import org.mongodb.scala.bson.collection.mutable.Document

import java.io.{BufferedReader, FileReader}
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.{Date, Properties, UUID}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object Main {
  def main(args: Array[String]): Unit = {
    println("Program started")

    // Kafka producer configuration
    val producerProps = new Properties()
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)

    println("Setting up Kafka producer")
    val producer = new KafkaProducer[String, String](producerProps)
    val topic = "A"
    val filePath = "/Users/mbpro/Downloads/KafkaProj/src/main/scala/tweets.csv" // Replace with your CSV file path

    // Read data from file and produce to Kafka topic
    try {
      val fileReader = new FileReader(filePath)
      val reader = new BufferedReader(fileReader)
      var line: String = reader.readLine()

      while (line != null) {
        val message = new ProducerRecord[String, String](topic, line)
        producer.send(message)
        println(s"Sending message: $line")
        line = reader.readLine()
      }

      reader.close()
      fileReader.close()
    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      println("Closing Kafka producer")
      producer.close()
    }

    // Kafka consumer configuration
    val consumerProps = new Properties()
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString)
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")


    println("Setting up Kafka consumer")
    val consumer = new KafkaConsumer[String, String](consumerProps)
    consumer.subscribe(List(topic).asJava)

    // MongoDB connection setup
    val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
    val database: MongoDatabase = mongoClient.getDatabase("BigData")
    val collection: MongoCollection[Document] = database.getCollection("tweet")

    val dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")

    try {
      while (true) {
        println("Polling for messages...")
        val records = consumer.poll(java.time.Duration.ofMillis(5000)).asScala

        if (records.isEmpty) {
          println("No messages received in this poll.")
        } else {
          println(s"Received ${records.size} messages")
        }

        for (record <- records) {
          println(s"Processing message: ${record.value()}")
          Try {
            val fields = record.value().split(",", -1) // Splitting with limit to handle empty fields
            if (fields.length >= 5) {
              val id = fields(1).filter(_.isDigit).toLong // Extracting only digits for the ID
              val dateStr = fields(2)
              val user = fields(4)
              val text = fields(5)
              val dateNow = Instant.now()

              val dateNowAsDate = Date.from(dateNow)
              val date = Try(dateFormat.parse(dateStr)).getOrElse(dateNowAsDate)

              val doc = Document(
                "id" -> id,
                "date" -> date,
                "user" -> user,
                "text" -> text,
                "retweets" -> 0,
                "dateNow" -> dateNowAsDate
              )

              collection.insertOne(doc).toFuture().onComplete {
                case Success(_) => println("Successfully inserted document: " + doc.toJson())
                case Failure(e) => println("Failed to insert document: " + e.getMessage)
              }
            } else {
              println("Invalid message format, skipping message.")
            }
          }.recover {
            case e: Exception => println(s"Error processing message: $e")
          }
        }
      }
    } catch {
      case e: Exception => println(s"Error in main loop: $e")
    } finally {
      println("Closing Kafka consumer and MongoDB client")
      consumer.close()
      mongoClient.close()
    }
  }
}
