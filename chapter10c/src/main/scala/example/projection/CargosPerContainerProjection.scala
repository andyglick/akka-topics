package example.projection

import org.slf4j.LoggerFactory

import akka.actor.typed.ActorSystem

import akka.projection.ProjectionId
import akka.projection.scaladsl.ExactlyOnceProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider

import akka.projection.jdbc.scaladsl.{ JdbcHandler, JdbcProjection }

import akka.persistence.query.Offset
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal

import example.repository.scalike.{
  CargosPerContainerRepository,
  ScalikeJdbcSession
}

import example.persistence.SPContainer

object CargosPerContainerProjection {

  val logger =
    LoggerFactory.getLogger(CargosPerContainerProjection + "")

  def createProjectionFor(
      system: ActorSystem[_],
      repository: CargosPerContainerRepository,
      indexTag: Int): ExactlyOnceProjection[
    Offset,
    EventEnvelope[SPContainer.Event]] = {

    val tag = "container-tag-" + indexTag

    val sourceProvider =
      EventSourcedProvider.eventsByTag[SPContainer.Event](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag)

    JdbcProjection.exactlyOnce(
      projectionId =
        ProjectionId("CargosPerContainerProjection", tag),
      sourceProvider = sourceProvider,
      handler = () => new CPCProjectionHandler(repository),
      sessionFactory = () => new ScalikeJdbcSession())(system)
  }
}

class CPCProjectionHandler(repository: CargosPerContainerRepository)
    extends JdbcHandler[
      EventEnvelope[SPContainer.Event],
      ScalikeJdbcSession] {

  val logger = LoggerFactory.getLogger(classOf[CPCProjectionHandler])

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[SPContainer.Event]): Unit = {
    envelope.event match {
      case SPContainer.CargoAdded(containerId, cargo) =>
        repository.addCargo(containerId, session)
      case x =>
        logger.debug("ignoring event {} in projection", x)

    }
  }
}
