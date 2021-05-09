package example.persistence

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit
}

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit

import com.typesafe.config.ConfigFactory

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior
}
import akka.persistence.typed.PersistenceId

import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityRef,
  EntityTypeKey
}
import akka.cluster.sharding.typed.ShardingEnvelope

class SContainerSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(
        ConfigFactory.load("application-test")))
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  "a persistent entity with sharding" should {

    "be able to add container" in {
      val sharding = ClusterSharding(system)

      val shardRegion
          : ActorRef[ShardingEnvelope[SContainer.Command]] =
        sharding.init(
          Entity(SContainer.TypeKey)(createBehavior = entityContext =>
            SContainer(entityContext.entityId)))

      val containerId = "id-1"
      val cargo = SContainer.Cargo("id-c", "sack", 3)

      shardRegion ! ShardingEnvelope(
        containerId,
        SContainer.AddCargo(cargo))

      val probe =
        createTestProbe[List[SContainer.Cargo]]()
      val container: EntityRef[SContainer.Command] =
        sharding.entityRefFor(SContainer.TypeKey, containerId)
      container ! SContainer.GetCargos(probe.ref)

      probe.expectMessage(List(cargo))

    }

  }
  // "a persistent entity with sharing and state" should {
  //   "be able to add parcels even when cleaning by stashing them" in {
  //     val sharding = ClusterSharding(system)

  //     val shardRegion: ActorRef[
  //       ShardingEnvelope[SContainerFSM.Command]] =
  //       sharding.init(
  //         Entity(SContainerFSM.TypeKey)(createBehavior =
  //           entityContext =>
  //             SContainerFSM(entityContext.entityId)))

  //     val containerId = "id-2"
  //     val parcelCode = "ABC"

  //     shardRegion ! ShardingEnvelope(
  //       containerId,
  //       SContainerFSM.Clean)

  //     shardRegion ! ShardingEnvelope(
  //       containerId,
  //       SContainerFSM.AddParcel(parcelCode))

  //     shardRegion ! ShardingEnvelope(
  //       containerId,
  //       SContainerFSM.AddParcel(parcelCode))

  //     val probe = createTestProbe[List[String]]()
  //     val truck: EntityRef[SContainerFSM.Command] =
  //       sharding.entityRefFor(
  //         SContainerFSM.TypeKey,
  //         containerId)
  //     truck ! SContainerFSM.GetParcels(probe.ref)

  //     probe.expectMessage(List("no parcels, we're cleaning"))

  //   }
  // }
}
// object SContainerFSM {

//   val TypeKey =
//     EntityTypeKey[SContainerFSM.Command](
//       "vehicle-type-key")

//   sealed trait Command
//   case object Clean extends Command
//   case class AddParcel(id: String) extends Command
//   case class GetParcels(replyTo: ActorRef[List[String]])
//       extends Command

//   sealed trait Event
//   case class ParcelAdded(id: String) extends Event
//   case object CleanedRequested extends Event

//   sealed trait State {
//     def parcels: List[String]
//   }

//   final case class Ready(parcels: List[String]) extends State
//   final case class Cleaning(parcels: List[String] = Nil) extends State

//   def apply(containerId: String): Behavior[Command] =
//     EventSourcedBehavior[Command, Event, State](
//       PersistenceId(TypeKey.name, containerId),
//       Ready(List()),
//       commandHandler,
//       eventHandler)

//   def commandHandler(
//       state: State,
//       command: Command): Effect[Event, State] =
//     state match {
//       case c: Cleaning =>
//         command match {
//           case AddParcel(id) => Effect.stash
//           case GetParcels(replyTo) =>
//             Effect.none.thenRun(state =>
//               replyTo ! List("no parcels, we're cleaning"))
//           case Clean => Effect.none
//         }
//       case r: Ready =>
//         command match {
//           case AddParcel(id) =>
//             //call the drone to add the parcel
//             //is this blocking. Again there is many situation we could model
//             // like the vehicle being full.
//             Effect.persist(ParcelAdded(id))
//           case GetParcels(replyTo) =>
//             Effect.none.thenRun(state => replyTo ! state.parcels)
//           case Clean => Effect.persist(Cleaned)
//         }
//     }

//   def eventHandler(state: State, event: Event): State =
//     state match {
//       case c: Cleaning => state
//       case r: Ready =>
//         event match {
//           case ParcelAdded(id) =>
//             r.copy(parcels = id +: state.parcels)
//           case Cleaned => Ready(List())
//         }
//     }

//   def findAndSet(durationMillis: Int): Unit = {
//     val until = System.currentTimeMillis() + durationMillis
//     while (System.currentTimeMillis() < until) //cleaning
//       println("found")
//   }
// }