package org.frostplain.kafkaproxy

import java.nio.ByteBuffer

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http._
import io.netty.util.AsciiString
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class HttpServerApplicationHandler extends ChannelInboundHandlerAdapter {
  private val CONTENT = "Hello world".getBytes
  private val CONTENT_TYPE = AsciiString.cached("Content-Type")
  private val CONTENT_LENGTH = AsciiString.cached("Content-Length")
  private val CONNECTION = AsciiString.cached("Connection")
  private val KEEP_ALIVE = AsciiString.cached("keep-alive")

  private val ALIVE_MESSAGE = Unpooled.wrappedBuffer("{status: 0}".getBytes())

  val logger = LoggerFactory.getLogger(classOf[HttpServerApplicationHandler])

  var keepAlive = false
  var receiving= false
  var buffer:Option[ByteBuffer] = None

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush
  }


  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    msg match {
      case req: HttpRequest =>
        keepAlive = HttpUtil.isKeepAlive(req)

        req.method() match {
          case HttpMethod.POST =>

            if (req.headers().contains(CONTENT_LENGTH)) {
              receiving = true
              buffer = Some(ByteBuffer.allocate(HttpUtil.getContentLength(req).toInt))
            }
          case HttpMethod.GET =>
            val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ALIVE_MESSAGE)
            response.headers.set(CONTENT_TYPE, "application/json")
            response.headers.setInt(CONTENT_LENGTH, response.content.readableBytes)
            this.responseWithKeepAlive(ctx, response)
          case _ =>
          //unsupported
        }
      case _ =>
    }

    if (receiving) {
      msg match {
        case chunk: HttpContent =>
          buffer match {
            case Some(actual) =>
              chunk.content().getBytes(actual.position(), actual)
            case None =>
              logger.warn("Get chunk but buffer is none. Something wrong")
          }
          if (chunk.isInstanceOf[LastHttpContent]) {
            logger.debug("receive last chunk")

            buffer match {
              case Some(actual) =>
                KafkaController.producer match {
                  case Some(producer) =>
                    val record = new ProducerRecord[String,Array[Byte]]("test", actual.array())
                    //todo: support callback and response
                    producer.send(record)
                  case None =>
                    logger.warn("Kafka Producer is not prepared.")
                }
              case None =>
                logger.warn("Get last chunk but buffer is none. Something wrong")
            }

            // explicit dereference
            buffer = None
            receiving = false
            val response = new DefaultFullHttpResponse(
              io.netty.handler.codec.http.HttpVersion.HTTP_1_1,
              io.netty.handler.codec.http.HttpResponseStatus.OK,
              Unpooled.wrappedBuffer(CONTENT)
            )
            response.headers.set(CONTENT_TYPE, "text/plain")
            response.headers.setInt(CONTENT_LENGTH, response.content.readableBytes)
            this.responseWithKeepAlive(ctx, response)
          }
        case _ =>
      }
    }
  }

  def responseWithKeepAlive(ctx: ChannelHandlerContext, response: HttpResponse): Unit ={
    if (!keepAlive) ctx.write(response).addListener(ChannelFutureListener.CLOSE)
    else {
      response.headers.set(CONNECTION, KEEP_ALIVE)
      ctx.write(response)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.error("caught exception", cause)
    ctx.close
  }
}
