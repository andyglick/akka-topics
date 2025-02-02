package betting.house.projection.proto

import akka.actor.typed.{ ActorSystem, DispatcherSelector }
import scala.concurrent.{ ExecutionContext, Future }

import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession,
  StakePerResult
}

class BetProjectionServiceImpl(
    system: ActorSystem[_],
    betRepository: BetRepository)
    extends BetProjectionService {

  implicit private val jdbcExecutor: ExecutionContext =
    system.dispatchers.lookup(
      DispatcherSelector.fromConfig(
        "akka.projection.jdbc.blocking-jdbc-dispatcher"))

  def getBetByMarket(
      in: MarketIdsBet): scala.concurrent.Future[SumStakes] = {
    Future {
      ScalikeJdbcSession.withSession { session =>
        val sumStakes = betRepository
          .getBetPerMarketTotalStake(in.marketId, session)
          .map { each =>
            println(s"${each.sum} : ${each.result}")
            SumStake(each.sum, each.result)
          }
        println(SumStakes(sumStakes))
        SumStakes(sumStakes)
      }
    }
  }
}
