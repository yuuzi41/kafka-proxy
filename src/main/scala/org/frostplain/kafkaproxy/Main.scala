package org.frostplain.kafkaproxy

import java.lang

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

//    KafkaController.init(new Properties())
    try {
      val bootstrap = new ServerBootstrap()
      bootstrap.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
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
        .localAddress(10000)

      val f = bootstrap.bind().sync()
      println("Binded")

      f.channel().closeFuture().sync()
    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}