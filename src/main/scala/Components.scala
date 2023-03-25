import cats.data.{Store => *, *}
import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.concurrent.*
import fs2.dom.{Event => _, *}
import io.circe.parser.*
import io.circe.syntax.*
import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import scoin.*
import snow.*

import Utils.*

object Components {
  def renderEventPointer(
      evp: snow.EventPointer
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "text-md",
      entry("event id (hex)", evp.id),
      if evp.relays.size > 0 then
        Some(entry("relay hints", evp.relays.reduce((a, b) => s"$a, $b")))
      else None,
      evp.author.map { pk =>
        entry("author hint (pubkey hex)", pk.value.toHex)
      }
    )

  def renderProfilePointer(
      pp: snow.ProfilePointer,
      sk: Option[PrivateKey] = None
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "text-md",
      sk.map { k => entry("private key (hex)", k.value.toHex) },
      entry("public key (hex)", pp.pubkey.value.toHex),
      if pp.relays.size > 0 then
        Some(entry("relay hints", pp.relays.reduce((a, b) => s"$a, $b")))
      else None
    )

  def renderAddressPointer(
      addr: snow.AddressPointer
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "text-md",
      entry("author (pubkey hex)", addr.author.value.toHex),
      entry("identifier", addr.d),
      entry("kind", addr.kind.toString),
      if addr.relays.size > 0 then
        Some(entry("relay hints", addr.relays.reduce((a, b) => s"$a, $b")))
      else None
    )

  def renderEvent(
      event: Event,
      store: Store
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "text-md",
      if event.pubkey.isEmpty then
        Some(
          div(
            cls := "flex items-center",
            entry("missing", "pubkey"),
            button(
              Styles.buttonSmall,
              "fill with a debugging key",
              onClick --> (_.foreach { _ =>
                store.input.set(
                  event
                    .copy(pubkey = Some(keyOne.publicKey.xonly))
                    .asJson
                    .printWith(jsonPrinter)
                )
              })
            )
          )
        )
      else None,
      if event.id.isEmpty then
        Some(
          div(
            cls := "flex items-center",
            entry("missing", "id"),
            if event.pubkey.isDefined then
              Some(
                button(
                  Styles.buttonSmall,
                  "fill id",
                  onClick --> (_.foreach(_ =>
                    store.input.set(
                      event
                        .copy(id = Some(event.hash.toHex))
                        .asJson
                        .printWith(jsonPrinter)
                    )
                  ))
                )
              )
            else None
          )
        )
      else None,
      if event.sig.isEmpty then
        Some(
          div(
            cls := "flex items-center",
            entry("missing", "sig"),
            if event.id.isDefined && event.pubkey == Some(
                keyOne.publicKey.xonly
              )
            then
              Some(
                button(
                  Styles.buttonSmall,
                  "sign",
                  onClick --> (_.foreach(_ =>
                    store.input.set(
                      event
                        .sign(keyOne)
                        .asJson
                        .printWith(jsonPrinter)
                    )
                  ))
                )
              )
            else None
          )
        )
      else None,
      entry("serialized event", event.serialized),
      entry("implied event id", event.hash.toHex),
      entry(
        "does the implied event id match the given event id?",
        event.id == Some(event.hash.toHex) match {
          case true => "yes"; case false => "no"
        }
      ),
      entry(
        "is signature valid?",
        event.isValid match {
          case true => "yes"; case false => "no"
        }
      )
    )

  private def entry(
      key: String,
      value: String
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      span(cls := "font-bold", key + " "),
      span(Styles.mono, value)
    )
}