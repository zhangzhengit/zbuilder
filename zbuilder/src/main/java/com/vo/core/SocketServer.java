package com.vo.core;

import java.net.InetSocketAddress;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.common.collect.Maps;
import com.vo.anno.ZComponent;
import com.vo.core.ZLog2;

import cn.hutool.core.lang.UUID;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年10月11日
 *
 */
//@ZComponent
// FIXME 2023年10月16日 下午8:04:51 zhanghen: TODO
// 1 获取mvn日志输出，改为用websocket，以及后面的全部耗时长的都是websocket实时输出
public class SocketServer extends WebSocketServer {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private final int port;

	public SocketServer(final int port) {
		super(new InetSocketAddress(port));
		this.port = port;
	}

	@Override
	public void onOpen(final WebSocket conn, final ClientHandshake handshake) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "SocketServer.onOpen()");
		this.add(conn);
		conn.getAttachment();
	}

	@Override
	public void onClose(final WebSocket conn, final int code, final String reason, final boolean remote) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "SocketServer.onClose()");
		this.remove(conn);
	}

	@Override
	public void onMessage(final WebSocket conn, final String message) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "SocketServer.onMessage()");
		System.out.println("message = " + message);
		conn.send("ok-这是从server主动发送来的");
	}

	@Override
	public void onError(final WebSocket conn, final Exception ex) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "SocketServer.onError()");

	}

	@Override
	public void onStart() {
		LOG.info("webSocket服务启动成功，port={}", this.port);
	}

	ConcurrentMap<String, WebSocket> map = Maps.newConcurrentMap();
	public WebSocket getByUUID(final String uuid) {
		final WebSocket v = this.map.get(uuid);
		return v;
	}

	private String add(final WebSocket webSocket) {
		final String uuid = UUID.randomUUID().toString();
		this.map.put(uuid, webSocket);
		return uuid;
	}

	private void remove(final WebSocket webSocket) {

		final Set<Entry<String, WebSocket>> es = this.map.entrySet();
		for (final Entry<String, WebSocket> entry : es) {
			if (entry.getValue() == webSocket) {
				this.map.remove(entry.getKey());
				break;
			}
		}
	}

}
