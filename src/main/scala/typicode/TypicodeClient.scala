package typicode

import io.circe.generic.auto.*
import sttp.client3.*
import sttp.tapir.*
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

import concurrent.ExecutionContext.Implicits.global
import concurrent.Future

object TypicodeClient:
  private val typicodeUrl = uri"https://jsonplaceholder.typicode.com"

  private val users: PublicEndpoint[Unit, String, List[Domain.User], Any] =
    endpoint
      .in("users")
      .get
      .out(jsonBody[List[Domain.User]])
      .errorOut(jsonBody[String])

  private val user: PublicEndpoint[Int, String, Domain.User, Any] =
    endpoint
      .in("users" / path[Int]("userId"))
      .get
      .out(jsonBody[Domain.User])
      .errorOut(jsonBody[String])

  private val backend     = FetchBackend()
  private val interpreter = SttpClientInterpreter()

  def getUsers: Future[Either[String, List[Domain.User]]] =
    interpreter
      .toRequestThrowDecodeFailures(users, Some(typicodeUrl))
      .apply(())
      .send(backend)
      .map(_.body)

  def getUser(userId: Int): Future[Either[String, Domain.User]] =
    interpreter
      .toRequestThrowDecodeFailures(user, Some(typicodeUrl))
      .apply(userId)
      .send(backend)
      .map(_.body)
