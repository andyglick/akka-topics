package example.betting

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit
}

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import akka.actor.testkit.typed.scaladsl.ActorTestKit

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
import akka.cluster.typed.{ Cluster, Join }

import java.time.OffsetDateTime

import scala.concurrent.duration._

object IntegrationSpec {
  val config = ConfigFactory.parseString(
    """
      akka.actor.provider = cluster
      akka.actor.serialization-bindings {
        "example.betting.CborSerializable" = jackson-cbor
      } 
      akka.remote.classic.netty.tcp.port = 0
      akka.remote.artery.canonical.port = 12345
      akka.remote.artery.canonical.hostname = 127.0.0.1
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.journal.inmem.test-serialization = on
      """)

}

class IntegrationSpec
    extends ScalaTestWithActorTestKit(IntegrationSpec.config)
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  private val sharding = ClusterSharding(system)

  override def beforeAll() {
    super.beforeAll()
    Cluster(system).manager ! Join(Cluster(system).selfMember.address)

    sharding.init(
      Entity(Wallet.TypeKey)(createBehavior = entityContext =>
        Wallet(entityContext.entityId)))

    sharding.init(
      Entity(Market.TypeKey)(createBehavior = entityContext =>
        Market(entityContext.entityId)))

    sharding.init(
      Entity(Bet.TypeKey)(createBehavior = entityContext =>
        Bet(entityContext.entityId)))
  }

  "a bet" should {
    "be able to relate to a market and a wallet" in {

      val walletProbe = createTestProbe[Wallet.Response]

      val wallet = sharding.entityRefFor(Wallet.TypeKey, "walletId1")

      wallet ! Wallet.AddFunds(100, walletProbe.ref)

      walletProbe.expectMessage(10.seconds, Wallet.Accepted)

      val marketProbe = createTestProbe[Market.Response]

      val market = sharding.entityRefFor(Market.TypeKey, "marketId1")

      market ! Market.Initialize(
        Market.Fixture("fixtureId1", "RM", "MU"),
        Market.Odds(1.25, 1.75, 1.05),
        OffsetDateTime.now,
        marketProbe.ref)

      marketProbe.expectMessage(10.seconds, Market.Accepted)

      val bet = sharding.entityRefFor(Bet.TypeKey, "betId")

      val betProbe = createTestProbe[Bet.Response]

      bet ! Bet.Open(
        "walletId1",
        "marketId1",
        1.26,
        100,
        0,
        betProbe.ref)

      betProbe.expectMessage(Bet.Accepted)

    }

  }

}