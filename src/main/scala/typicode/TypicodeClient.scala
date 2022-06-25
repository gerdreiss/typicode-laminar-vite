package typicode

import cats.implicits.*
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
      .errorOut(stringBody)

  private val user: PublicEndpoint[Int, String, Domain.User, Any] =
    endpoint
      .in("users" / path[Int]("userId"))
      .get
      .out(jsonBody[Domain.User])
      .errorOut(stringBody)

  private val userTodos: PublicEndpoint[Int, String, List[Domain.Todo], Any] =
    endpoint
      .in("users" / path[Int]("userId") / "todos")
      .get
      .out(jsonBody[List[Domain.Todo]])
      .errorOut(stringBody)

  private val userPosts: PublicEndpoint[Int, String, List[Domain.Post], Any] =
    endpoint
      .in("users" / path[Int]("userId") / "posts")
      .get
      .out(jsonBody[List[Domain.Post]])
      .errorOut(stringBody)

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

  def getUserTodos(userId: Int): Future[Either[String, List[Domain.Todo]]] =
    interpreter
      .toRequestThrowDecodeFailures(userTodos, Some(typicodeUrl))
      .apply(userId)
      .send(backend)
      .map(_.body)

  def getUserPosts(userId: Int): Future[Either[String, List[Domain.Post]]] =
    interpreter
      .toRequestThrowDecodeFailures(userPosts, Some(typicodeUrl))
      .apply(userId)
      .send(backend)
      .map(_.body)

  def getUserPostsAndTodos(
      userId: Int
  ): Future[Either[String, (Domain.User, List[Domain.Post], List[Domain.Todo])]] =
    getUser(userId).zipWith(getUserPosts(userId).zip(getUserTodos(userId))) { //
      case (user, (posts, todos)) => (user, posts, todos).mapN((_, _, _))
    }
