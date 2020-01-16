package org.seekloud.geek.player.util

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString


//https://github.com/hseeberger/akka-http-json/blob/master/akka-http-circe/src/main/scala/de/heikoseeberger/akkahttpcirce/CirceSupport.scala
/**
  * Automatic to and from JSON marshalling/unmarshalling using an in-scope *Circe* protocol.
  *
  * To use automatic codec derivation, user need to import `circe.generic.auto._`.
  */
object CirceSupport extends CirceSupport

/**
  * JSON marshalling/unmarshalling using an in-scope *Circe* protocol.
  *
  * To use automatic codec derivation, user need to import `io.circe.generic.auto._`
  */
trait CirceSupport {

  import io.circe._
  import io.circe.parser._

  private val jsonStringUnmarshaller =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`application/json`)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset)       => data.decodeString(charset.nioCharset.name)
      }

  private val jsonStringMarshaller =
    Marshaller.stringMarshaller(`application/json`)

  /**
    * HTTP entity => `A`
    *
    * @param decoder decoder for `A`, probably created by `circe.generic`
    * @tparam A type to decode
    * @return unmarshaller for `A`
    */

  /*  implicit def circeUnmarshaller[A](
      implicit decoder: Decoder[A]
    ): FromEntityUnmarshaller[A] =
      jsonStringUnmarshaller.map(jawn.decode(_).fold(throw _, identity))
   */



  //out either.
  implicit def circeUnmarshaller[A](
                                     implicit decoder: Decoder[A]
                                   ): FromEntityUnmarshaller[Either[Error, A]] =
  jsonStringUnmarshaller.map(decode[A])



  /**
    * `A` => HTTP entity
    *
    * @param encoder encoder for `A`, probably created by `circe.generic`
    * @param printer pretty printer function
    * @tparam A type to encode
    * @return marshaller for any `A` value
    */
  implicit def circeToEntityMarshaller[A](
                                           implicit encoder: Encoder[A],
                                           printer: Json => String = Printer.noSpaces.pretty
                                         ): ToEntityMarshaller[A] =
    jsonStringMarshaller.compose(printer).compose(encoder.apply)
}