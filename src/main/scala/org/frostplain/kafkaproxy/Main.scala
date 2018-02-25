package org.frostplain.kafkaproxy

import java.io.{FileInputStream, IOException}
import java.lang
import java.util.Properties

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelInitializer, ChannelOption}
import io.netty.handler.codec.http.{HttpServerCodec, HttpServerExpectContinueHandler}
import io.netty.handler.logging.{LogLevel, LoggingHandler}

object Main {
  def main(argv: Array[String]): Unit = {
    val bossGroup = new NioEventLoopGroup()
    val workerGroup = new NioEventLoopGroup()

    val properties = new Properties()

    try {
      val stream = new FileInputStream("kafka.properties")
      properties.load(stream)
    } catch {
      case e: IOException =>
        e.printStackTrace()
        return
    }
    KafkaController.init(properties)

    try {
      val bootstrap = new ServerBootstrap()
      bootstrap.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .childHandler(new ChannelInitializer[SocketChannel] {
          override def initChannel(ch: SocketChannel): Unit = {
            val pipeline = ch.pipeline()
            pipeline.addLast(new HttpServerCodec())
            pipeline.addLast(new HttpServerExpectContinueHandler())
            pipeline.addLast(new HttpServerApplicationHandler())
          }
        })
        .option(ChannelOption.SO_BACKLOG, new Integer(128))
        .childOption(ChannelOption.SO_KEEPALIVE, new lang.Boolean(true))
        .localAddress(18080)

      val f = bootstrap.bind().sync()
      println("Binded")

      f.channel().closeFuture().sync()
    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}