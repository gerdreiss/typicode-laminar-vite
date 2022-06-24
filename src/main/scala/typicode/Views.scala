package typicode

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

enum Command:
  case ShowAllUsers
  case ShowUser(userId: Int)

object Views:
  val $userStream = EventStream.fromFuture(TypicodeClient.getUsers)

  val headerVar: Var[String]            = Var("Users")
  val usersVar: Var[List[Domain.User]]  = Var(List.empty)
  val userVar: Var[Option[Domain.User]] = Var(None)

  val commandObserver = Observer[Command] {
    case Command.ShowAllUsers =>
      TypicodeClient.getUsers
        .onComplete {
          case Success(Right(users)) =>
            headerVar.set("Users")
            usersVar.set(users)
            userVar.set(None)
          case Success(Left(error)) =>
            headerVar.set(error)
            usersVar.set(List.empty)
            userVar.set(None)
          case Failure(error) =>
            headerVar.set(error.getMessage)
            usersVar.set(List.empty)
            userVar.set(None)
        }
    case Command.ShowUser(userId) =>
      TypicodeClient
        .getUser(userId)
        .onComplete {
          case Success(Right(user)) =>
            headerVar.set(user.name)
            usersVar.set(List.empty)
            userVar.set(Some(user))
          case Success(Left(error)) =>
            headerVar.set(error)
            usersVar.set(List.empty)
            userVar.set(None)
          case Failure(error) =>
            headerVar.set(error.getMessage)
            usersVar.set(List.empty)
            userVar.set(None)
        }
  }

  def renderApp: ReactiveHtmlElement[HTMLElement] =
    div(
      cls := "ui raised very padded container segment",
      h1(
        cls := "ui header",
        i(cls   := "circular users icon"),
        div(cls := "content", child.text <-- headerVar.signal)
      ),
      div(cls := "ui divider"),
      children <-- $userStream.map {
        case Left(error)  => renderError(error)
        case Right(users) => renderUserList(users)
      }
    )

  def renderError(error: String): List[ReactiveHtmlElement[HTMLElement]] =
    div(cls := "content", p(s"Loading users failed: $error"), color := "red") :: Nil

  def renderUserList(users: List[Domain.User]): List[ReactiveHtmlElement[HTMLElement]] =
    users.map { user =>
      div(
        cls := "ui grid",
        div(cls := "four wide column", renderUser(user)),
        div(cls := "three wide column", renderAddress(user.address)),
        div(cls := "three wide column", renderGeo(user.address.geo)),
        div(cls := "six wide column", renderCompany(user.company))
      )
    }

  def renderUser(user: Domain.User): ReactiveHtmlElement[HTMLElement] =
    div(
      cls := "ui card",
      div(
        cls := "content",
        div(
          cls := "header",
          a(user.name),
          onClick.mapTo(user.id) --> commandObserver.contramap[Int] { userId =>
            Command.ShowUser(userId)
          }
        ),
        div(cls := "description", i(cls := "envelope icon"), user.email),
        div(cls := "description", i(cls := "phone icon"), user.phone),
        div(cls := "description", i(cls := "globe icon"), user.website),
        br()
      )
    )

  def renderAddress(address: Domain.Address): ReactiveHtmlElement[HTMLElement] =
    div(
      cls := "ui card",
      div(
        cls := "content",
        div(cls := "header", i(cls := "address book icon"), "Address"),
        div(cls := "description", address.street),
        div(cls := "description", address.suite),
        div(cls := "description", address.city),
        div(cls := "description", address.zipcode)
      )
    )

  def renderGeo(geo: Domain.Geo): ReactiveHtmlElement[HTMLElement] =
    div(
      cls := "ui card",
      div(
        cls := "content",
        div(cls := "header", i(cls := "location arrow icon"), "Position"),
        div(cls := "description", geo.lat),
        div(cls := "description", geo.lng),
        br(),
        br()
      )
    )

  def renderCompany(company: Domain.Company): ReactiveHtmlElement[HTMLElement] =
    div(
      cls := "ui card",
      div(
        cls := "content",
        div(cls := "header", i(cls := "building icon"), "Company"),
        div(cls := "description", company.name),
        div(cls := "description", company.catchPhrase),
        div(cls := "description", company.bs),
        br()
      )
    )
