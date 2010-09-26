/**
 * Classes implementing SSL support for Netty
 *
 * To generate the stores for GoldenGate for instance, you need to create 2 JKS keyStore.
 * To generate those files, you can use the "keytool" command from the JDK or using the free
 * tool KeyTool IUI (last known version in 2.4.1).<br><br>
 *
 * See Certificate-Howto.txt file<br><br>
 *
 * Usage:<br>
 * In order to use the SSL support, here are the different steps.<br><br>
 *
 * <b>On Client side:</b><br>
 * <ul>
 * <li>Create the KeyStore for the Client<br>
 * <b>For no client authentication:</b><br>
 * ggSecureKeyStore = new GgSecureKeyStore(keyStorePasswd, keyPasswd);<br>
 * <b>For client authentication:</b><br>
 * ggSecureKeyStore = new GgSecureKeyStore(keyStoreFilename, keyStorePasswd, keyPasswd);</li>
 * <li>Create the TrustStore for the Client<br>
 * <b>For Trusting everyone:</b><br>
 * ggSecureKeyStore.initEmptyTrustStore(keyTrustStorePasswd);<br>
 * <b>For Trusting only known Certificates:</b><br>
 * ggSecureKeyStore.initTrustStore(keyTrustStoreFilename, keyTrustStorePasswd);</li>
 * <li>Create the GgSslContextFactory:<br>
 * GgSslContextFactory ggSslContextFactory = new GgSslContextFactory(ggSecureKeyStore, <b>false</b>);</li>
 * <li>Create your own PipelineFactory:<br>
 * As first item in the pipeline, add:<br>
 * pipeline.addLast("ssl", ggSslContextFactory.initPipelineFactory(<b>false</b>, ggSslContextFactory.hasTrustStore(), executor));<br>
 * where executor is generally a Executors.newCachedThreadPool();<br><br>
 *
 * For example, see GoldenGate Local Exec module using SSL:<br>
 * localExecClientPipelineFactory = new LocalExecSslClientPipelineFactory(ggSslContextFactory);<br>
 * bootstrap.setPipelineFactory(localExecClientPipelineFactory);</li>
 * <li>In the final Handler, you need to add the handshake:<br>
 * @Override<br>
 * public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)<br>
 * throws Exception {<br>
 *      ...<br>
 *      SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);<br>
 *      // Begin handshake<br>
 *      ChannelFuture handshakeFuture = sslHandler.handshake();<br>
 *      handshakeFuture.addListener(new ChannelFutureListener() {<br>
 *          public void operationComplete(ChannelFuture future)<br>
 *                  throws Exception {<br>
 *              if (future.isSuccess()) {<br>
 *                  //OK<br>
 *              } else {<br>
 *                  future.getChannel().close();<br>
 *              }<br>
 *          }<br>
 *      });<br>
 * }</li>
 * <li>At the end of your connection, you need to release the Executor passes as argument
 * to ggSslContextFactory.initPipelineFactory</li>
 * </ul>
 * <br><br>
 *
 * <b>On Server side:</b><br>
 * <ul>
 * <li>Create the KeyStore for the Server<br>
 * ggSecureKeyStore = new GgSecureKeyStore(keyStoreFilename, keyStorePasswd, keyPasswd);</li>
 * <li>Create the TrustStore for the Client<br>
 * <b>For Trusting everyone:</b><br>
 * ggSecureKeyStore.initEmptyTrustStore(keyTrustStorePasswd);<br>
 * <b>For Trusting only known Certificates:</b><br>
 * ggSecureKeyStore.initTrustStore(keyTrustStoreFilename, keyTrustStorePasswd);</li>
 * <li>Create the GgSslContextFactory:<br>
 * GgSslContextFactory ggSslContextFactory = new GgSslContextFactory(ggSecureKeyStore, <b>true</b>);</li>
 * <li>Create your own PipelineFactory:<br>
 * As first item in the pipeline, add:<br>
 * pipeline.addLast("ssl", ggSslContextFactory.initPipelineFactory(<b>true</b>, ggSslContextFactory.hasTrustStore(), executor));<br>
 * where executor is generally a Executors.newCachedThreadPool();<br><br>
 *
 * For example, see GoldenGate Local Exec module using SSL:<br>
 * bootstrap.setPipelineFactory(new LocalExecSslServerPipelineFactory(ggSslContextFactory, delay));</li>
 * <li>In the final Handler, you need to add the handshake:<br>
 * @Override<br>
 * public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)<br>
 * throws Exception {<br>
 *      ...<br>
 *      SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);<br>
 *      // Begin handshake<br>
 *      ChannelFuture handshakeFuture = sslHandler.handshake();<br>
 *      handshakeFuture.addListener(new ChannelFutureListener() {<br>
 *          public void operationComplete(ChannelFuture future)<br>
 *                  throws Exception {<br>
 *              if (future.isSuccess()) {<br>
 *                  //OK<br>
 *              } else {<br>
 *                  future.getChannel().close();<br>
 *              }<br>
 *          }<br>
 *      });<br>
 * }</li>
 * <li>At the end of your connection, you need to release the Executor passes as argument
 * to ggSslContextFactory.initPipelineFactory</li>
 * </ul>
 *
 */
package goldengate.common.crypto.ssl;