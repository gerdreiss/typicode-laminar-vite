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

  val defaultHeader = div(cls := "content", p("Users"))

  val headerVar: Var[ReactiveHtmlElement[HTMLElement]] = Var(defaultHeader)
  val usersVar: Var[List[Domain.User]]                 = Var(List.empty)
  val userVar: Var[Option[Domain.User]]                = Var(None)
  val postsVar: Var[List[Domain.Post]]                 = Var(List.empty)
  val todosVar: Var[List[Domain.Todo]]                 = Var(List.empty)

  val commandObserver = Observer[Command] {
    case Command.ShowAllUsers =>
      TypicodeClient.getUsers
        .onComplete {
          case Success(Right(users)) =>
            headerVar.set(defaultHeader)
            usersVar.set(users)
            userVar.set(None)
            postsVar.set(List.empty)
            todosVar.set(List.empty)
          case Success(Left(error)) =>
            headerVar.set(div(cls := "content", p(error)))
            usersVar.set(List.empty)
            userVar.set(None)
            postsVar.set(List.empty)
            todosVar.set(List.empty)
          case Failure(error) =>
            headerVar.set(div(cls := "content", p(error.getMessage)))
            usersVar.set(List.empty)
            userVar.set(None)
            postsVar.set(List.empty)
            todosVar.set(List.empty)
        }
    case Command.ShowUser(userId) =>
      TypicodeClient
        .getUserPostsAndTodos(userId)
        .onComplete {
          case Success(Right((user, posts, todos))) =>
            headerVar.set(userHeader(user.name))
            usersVar.set(List.empty)
            userVar.set(Some(user))
            postsVar.set(posts)
            todosVar.set(todos)
          case Success(Left(error)) =>
            headerVar.set(div(cls := "content", p(error)))
            usersVar.set(List.empty)
            userVar.set(None)
            postsVar.set(List.empty)
            todosVar.set(List.empty)
          case Failure(error) =>
            headerVar.set(div(cls := "content", p(error.getMessage)))
            usersVar.set(List.empty)
            userVar.set(None)
            postsVar.set(List.empty)
            todosVar.set(List.empty)
        }
  }

  def renderApp: ReactiveHtmlElement[HTMLElement] =
    div(
      cls := "ui raised very padded container segment",
      h1(
        cls := "ui header",
        i(cls   := "circular users icon"),
        div(cls := "content", child <-- headerVar.signal)
      ),
      div(cls := "ui divider"),
      children <-- usersVar.signal
        .combineWith(userVar.signal, postsVar.signal, todosVar.signal)
        .map {
          case (users, None, _, _)           => renderUserList(users)
          case (_, Some(user), posts, todos) => renderUser(user, posts, todos)
        },
      onMountCallback { ctx =>
        commandObserver.onNext(Command.ShowAllUsers)
      }
    )

  def userHeader(t: String): ReactiveHtmlElement[HTMLElement] =
    div(
      cls := "content",
      div(
        cls := "ui grid",
        div(
          cls := "row",
          div(cls := "fourteen wide column", p(t)),
          div(
            cls := "two wide column",
            button(
              cls := "ui labeled button",
              i(cls := "left arrow icon"),
              "Back",
              onClick.mapTo(()) --> commandObserver.contramap(_ => Command.ShowAllUsers)
            )
          )
        )
      )
    )

  def renderError(error: String): List[ReactiveHtmlElement[HTMLElement]] =
    div(cls := "content", p(s"Loading users failed: $error"), color := "red") :: Nil

  def renderUserList(users: List[Domain.User]): List[ReactiveHtmlElement[HTMLElement]] =
    users.map { user =>
      div(
        cls := "ui grid",
        div(cls := "four wide column", renderUserCard(user)),
        div(cls := "three wide column", renderAddressCard(user.address)),
        div(cls := "three wide column", renderGeoCard(user.address.geo)),
        div(cls := "six wide column", renderCompanyCard(user.company))
      )
    }

  def renderUser(
      user: Domain.User,
      posts: List[Domain.Post],
      todos: List[Domain.Todo]
  ): List[ReactiveHtmlElement[HTMLElement]] =
    div(
      cls := "ui grid",
      div(
        cls := "five wide column",
        renderUserCard(user),
        renderAddressCard(user.address),
        renderGeoCard(user.address.geo),
        renderCompanyCard(user.company)
      ),
      div(
        cls := "five wide column",
        h3(cls  := "ui header", div(cls := "content", i(cls := "list icon"), p("To-Do List"))),
        div(cls := "ui relaxed divided list", renderTodoList(todos))
      ),
      div(
        cls := "five wide column",
        h3(cls  := "ui header", div(cls := "content", i(cls := "edit icon"), p("Posts"))),
        div(cls := "ui relaxed divided list", renderPostList(posts))
      )
    ) :: Nil

  def renderUserCard(user: Domain.User): ReactiveHtmlElement[HTMLElement] =
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

  def renderAddressCard(address: Domain.Address): ReactiveHtmlElement[HTMLElement] =
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

  def renderGeoCard(geo: Domain.Geo): ReactiveHtmlElement[HTMLElement] =
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

  def renderCompanyCard(company: Domain.Company): ReactiveHtmlElement[HTMLElement] =
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

  def renderTodoList(todos: List[Domain.Todo]): List[ReactiveHtmlElement[HTMLElement]] =
    todos.map { todo =>
      div(
        cls := "item",
        if todo.completed then i(cls := "check icon")
        else i(cls                   := "square outline icon"),
        div(cls := "content", div(cls := "description", todo.title))
      )
    }

  def renderPostList(posts: List[Domain.Post]): List[ReactiveHtmlElement[HTMLElement]] =
    posts.map { post =>
      div(
        cls := "item",
        i(cls := "edit icon"),
        div(
          cls := "content",
          a(cls   := "header", a(post.title)),
          div(cls := "description", p(post.body))
        )
      )
    }
