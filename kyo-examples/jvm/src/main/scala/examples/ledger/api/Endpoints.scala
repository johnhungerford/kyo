package examples.ledger.api

import examples.ledger.*
import java.time.Instant
import kyo.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import zio.json.JsonEncoder

object Endpoints:

    val init: Unit < (Env[Handler] & Routes) = defer {

        val handler = await(Env.get[Handler])

        await {
            Routes.add(
                _.post
                    .in("clientes" / path[Int]("id") / "transacoes")
                    .errorOut(statusCode)
                    .in(jsonBody[Transaction])
                    .out(jsonBody[Processed])
            )(handler.transaction)
        }

        await {
            Routes.add(
                _.get
                    .in("clientes" / path[Int]("id") / "extrato")
                    .errorOut(statusCode)
                    .out(jsonBody[Statement])
            )(handler.statement)
        }

        await {
            Routes.add(
                _.get
                    .in("health")
                    .out(stringBody)
            )(_ => "ok")
        }
    }

end Endpoints
