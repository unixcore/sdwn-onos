/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.openflow.controller.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import static org.onlab.util.Tools.groupedThreads;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Creates a ChannelPipeline for a server-side openflow channel.
 */
public class OpenflowChannelInitializer extends ChannelInitializer {

    private final Logger log = LoggerFactory.getLogger(getClass());


    private final SSLContext sslContext;
    protected Controller controller;
    protected ThreadPoolExecutor pipelineExecutor;
    protected Timer timer;
//    protected IdleStateHandler idleHandler;
//    protected ReadTimeoutHandler readTimeoutHandler;

    public OpenflowChannelInitializer(Controller controller,
                                      ThreadPoolExecutor pipelineExecutor,
                                      SSLContext sslContext) {
        super();
        this.controller = controller;
        this.pipelineExecutor = pipelineExecutor;
        this.timer = new HashedWheelTimer(groupedThreads("OpenflowChannelInitializer", "timer-%d", log));
//        this.idleHandler = new IdleStateHandler(20, 25, 0);
//        this.readTimeoutHandler = new ReadTimeoutHandler(30);
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        OFChannelHandler handler = new OFChannelHandler(controller);
        ChannelPipeline pipeline = ch.pipeline();

        if (sslContext != null) {
            log.debug("OpenFlow SSL enabled.");
            SSLEngine sslEngine = sslContext.createSSLEngine();

            sslEngine.setNeedClientAuth(true);
            sslEngine.setUseClientMode(false);
            sslEngine.setEnabledProtocols(sslEngine.getSupportedProtocols());
            sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites());
            sslEngine.setEnableSessionCreation(true);

            SslHandler sslHandler = new SslHandler(sslEngine);
            pipeline.addLast("ssl", sslHandler);
        } else {
            log.debug("OpenFlow SSL disabled.");
        }

        pipeline.addLast("ofmessagedecoder", new OFMessageDecoder());
        pipeline.addLast("ofmessageencoder", new OFMessageEncoder());
        pipeline.addLast("idle", new IdleStateHandler(20, 25, 0));
        pipeline.addLast("timeout", new ReadTimeoutHandler(30));
        // XXX S ONOS: was 15 increased it to fix Issue #296
        pipeline.addLast("handshaketimeout",
                         new HandshakeTimeoutHandler(handler, timer, 60));
        if (pipelineExecutor != null) {
           pipeline.addLast(new DefaultEventExecutor(pipelineExecutor));
        }
        pipeline.addLast("handler", handler);
    }

//    @Override
//    public ChannelPipeline getPipeline() throws Exception {
//        OFChannelHandler handler = new OFChannelHandler(controller);
//
//        ChannelPipeline pipeline = Channels.pipeline();
//        if (sslContext != null) {
//            log.debug("OpenFlow SSL enabled.");
//            SSLEngine sslEngine = sslContext.createSSLEngine();
//
//            sslEngine.setNeedClientAuth(true);
//            sslEngine.setUseClientMode(false);
//            sslEngine.setEnabledProtocols(sslEngine.getSupportedProtocols());
//            sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites());
//            sslEngine.setEnableSessionCreation(true);
//
//            SslHandler sslHandler = new SslHandler(sslEngine);
//            pipeline.addLast("ssl", sslHandler);
//        } else {
//            log.debug("OpenFlow SSL disabled.");
//        }
//        pipeline.addLast("ofmessagedecoder", new OFMessageDecoder());
//        pipeline.addLast("ofmessageencoder", new OFMessageEncoder());
//        pipeline.addLast("idle", idleHandler);
//        pipeline.addLast("timeout", readTimeoutHandler);
//        // XXX S ONOS: was 15 increased it to fix Issue #296
//        pipeline.addLast("handshaketimeout",
//                         new HandshakeTimeoutHandler(handler, timer, 60));
//        if (pipelineExecutor != null) {
//            pipeline.addLast("pipelineExecutor",
//                             new ExecutionHandler(pipelineExecutor));
//        }
//        pipeline.addLast("handler", handler);
//        return pipeline;
//    }

}
