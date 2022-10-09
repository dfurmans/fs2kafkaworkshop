import cats.effect.{IO, IOApp}
import fs2.kafka._
import scala.concurrent.duration._
import cats.effect.unsafe.implicits.global

object Configuration {
  case class ServiceConfiguration(
                                   withBootstrapServers: String = "localhost:9092",
                                   withGroupId: String = "group",
                                   subscribeToTopicName: String = "aTopic3",
                                   produceToTopicName: String = "aTopic3"
                                 )
}

object Main extends IOApp.Simple {

  val run = {
    import Configuration._
    val appConfig = ServiceConfiguration()
    def processRecord(record: ConsumerRecord[String, String]): IO[(String, String)] =
      IO.pure(record.key -> record.value)

    val consumerSettings =
      ConsumerSettings[IO, String, String](
        keyDeserializer = Deserializer[IO, String],
        valueDeserializer = Deserializer[IO, String]
      )
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers(bootstrapServers = appConfig.withBootstrapServers)
        .withGroupId(groupId = appConfig.withGroupId )

    val producerSettings =
      ProducerSettings[IO, String, String]
        .withBootstrapServers(bootstrapServers = appConfig.withBootstrapServers)

    val stream =
      KafkaConsumer.stream(consumerSettings)
        .subscribeTo(appConfig.subscribeToTopicName)
        .records
        .mapAsync(25) { committable =>
          processRecord(committable.record)
            .map { case (key, value) =>
              val record = ProducerRecord(appConfig.produceToTopicName, key, value)
              println(s""" $key -> $value """)
              ProducerRecords.one(record, committable.offset)
            }
        }
        .through(KafkaProducer.pipe(producerSettings))
        .map(_.passthrough)
        .through(
          commitBatchWithin(500, 15.seconds)
        )

    stream.compile.drain
  }
  run.unsafeRunSync()
}
