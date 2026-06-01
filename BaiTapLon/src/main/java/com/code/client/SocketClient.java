package com.code.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;

import com.code.network.Request;
import com.code.network.Response;

/**
 * Quản lý kết nối Socket từ phía Client — Singleton (IMPROVED VERSION).
 *
 * <p><b>Cải thiện:</b>
 * <ul>
 *   <li>Fix contention: Tách lock cho sendRequest & sendAsync</li>
 *   <li>Fix zombie thread: join() trước startListening mới</li>
 *   <li>Auto-reconnect: Listener tự restart sau disconnect</li>
 *   <li>Socket timeout: Tránh hang vô hạn</li>
 *   <li>Callback protection: Exception trong callback không kill thread</li>
 *   <li>Atomic reconnect: Lock khi reconnect</li>
 * </ul>
 * </p>
 */
public class SocketClient {

    // ── Constants ──────────────────────────────────────────────────────────
    private static final int SOCKET_TIMEOUT_MS = 30000;  // 30s
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY_MS = 2000;  // 2s between attempts
    private static final int RESET_OBJECT_THRESHOLD = 50;  // reset after 50 objects

    // ── Singleton ──────────────────────────────────────────────────────────
    private static volatile SocketClient instance;

    public static synchronized void init(String host, int port) {
        if (instance == null) {
            instance = new SocketClient(host, port);
        }
    }

    public static SocketClient getInstance() {
        if (instance == null)
            throw new IllegalStateException("SocketClient chưa được khởi tạo. Gọi init() trước.");
        return instance;
    }

    // ── Fields ─────────────────────────────────────────────────────────────
    private final String host;
    private final int port;

    private volatile Socket socket;
    private volatile ObjectOutputStream out;
    private volatile ObjectInputStream in;

    private Thread listenerThread;
    private volatile boolean listening = false;
    private volatile int reconnectAttempt = 0;

    // ── Locks (separate for different operations) ──────────────────────────
    private final Object connectLock = new Object();    // for connect/reconnect
    private final Object sendRequestLock = new Object(); // for sendRequest
    private final Object sendAsyncLock = new Object();   // for sendAsync
    private final Object listenerLock = new Object();    // for listener control

    // ── Constructor & Connection ───────────────────────────────────────────

    private SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.reconnectAttempt = 0;
        try {
            connect();
        } catch (IOException e) {
            // Connection failed, but don't throw — allow controller to retry
            System.err.println("[Client] Initial connection failed: " + e.getMessage());
            System.err.println("[Client] Will retry on first request");
        }
    }

    /**
     * Establish socket connection with timeout.
     * IMPORTANT: Initialize out BEFORE in to avoid deadlock.
     */
    private void connect() throws IOException {
        synchronized (connectLock) {
            try {
                socket = new Socket(host, port);

                // ✨ Set socket options
                socket.setSoTimeout(SOCKET_TIMEOUT_MS);
                socket.setTcpNoDelay(true);  // reduce latency for realtime

                // ✨ IMPORTANT: out before in
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();  // flush before creating input stream

                in = new ObjectInputStream(socket.getInputStream());

                System.out.println("[Client] ✓ Connected to " + host + ":" + port);
                reconnectAttempt = 0;

            } catch (IOException e) {
                socket = null;
                out = null;
                in = null;
                throw new IOException("Failed to connect to " + host + ":" + port, e);
            }
        }
    }

    /**
     * Reconnect atomically.
     */
    private void reconnect() throws IOException {
        synchronized (connectLock) {
            if (socket == null || socket.isClosed()) {
                connect();
            }
        }
    }

    // ── Synchronous Request (BLOCKING) ─────────────────────────────────────

    /**
     * Send request and WAIT for response synchronously.
     * IMPORTANT: Call this from background thread, not UI thread.
     *
     * ✨ Fix: Separate lock from sendAsync
     */
    public Response sendRequest(Request request)
            throws IOException, ClassNotFoundException {

        synchronized (sendRequestLock) {  // ✨ Separate lock

            // Ensure connection
            if (socket == null || socket.isClosed()) {
                try {
                    reconnect();
                } catch (IOException e) {
                    throw new IOException("Cannot reconnect: " + e.getMessage());
                }
            }

            try {
                out.writeObject(request);
                out.flush();

                return (Response) in.readObject();

            } catch (SocketTimeoutException e) {
                throw new IOException("Request timeout (30s) - server not responding", e);

            } catch (IOException e) {
                // Connection lost, try to reconnect
                try {
                    reconnect();
                } catch (IOException reconnectError) {
                    System.err.println("[Client] Reconnect failed: " + reconnectError.getMessage());
                }
                throw new IOException("Connection error: " + e.getMessage(), e);
            }
        }
    }

    // ── Asynchronous Request (NON-BLOCKING) ────────────────────────────────

    /**
     * Send request asynchronously (don't wait for response).
     * Use during realtime listening mode.
     * Response will be received by listener thread.
     *
     * ✨ Fix: Separate lock from sendRequest
     */
    public void sendAsync(Request request) throws IOException {

        synchronized (sendAsyncLock) {  // ✨ Separate lock

            if (socket == null || socket.isClosed()) {
                try {
                    reconnect();
                } catch (IOException e) {
                    throw new IOException("Cannot reconnect: " + e.getMessage());
                }
            }

            try {
                out.writeObject(request);
                out.flush();

                // ✨ Lazy reset: only reset after many objects
                // (reduce overhead for real-time)

            } catch (SocketTimeoutException e) {
                throw new IOException("Send timeout (30s)", e);

            } catch (IOException e) {
                try {
                    reconnect();
                } catch (IOException reconnectError) {
                    System.err.println("[Client] Reconnect failed: " + reconnectError.getMessage());
                }
                throw new IOException("Send error: " + e.getMessage(), e);
            }
        }
    }

    // ── Asynchronous Listening (Background Thread) ─────────────────────────

    /**
     * Start listening for server PUSH events.
     *
     * ✨ Fixes:
     * - Join previous thread before starting new one (no zombie threads)
     * - Wrap callback in try-catch (callback error won't kill listener)
     * - Auto-reconnect if connection lost
     * - Separate lock for listener control
     */
    public void startListening(Consumer<Object> onEvent) {

        synchronized (listenerLock) {

            // ✨ Stop & wait for previous listener to die
            stopListening();

            if (listenerThread != null && listenerThread.isAlive()) {
                try {
                    System.out.println("[Client] Waiting for previous listener to stop...");
                    listenerThread.join(3000);  // wait max 3s

                    if (listenerThread.isAlive()) {
                        System.err.println("[Client] Previous listener still alive (timeout)");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            listening = true;
            reconnectAttempt = 0;

            listenerThread = new Thread(() -> {
                while (listening) {
                    try {
                        // Ensure connection before listening
                        if (socket == null || socket.isClosed()) {
                            if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
                                System.err.println("[Client] Max reconnect attempts reached");
                                listening = false;
                                onEvent.accept(null);
                                break;
                            }

                            reconnectAttempt++;
                            System.out.println("[Client] Reconnecting (attempt " + reconnectAttempt + ")...");

                            try {
                                reconnect();
                                reconnectAttempt = 0;  // reset on success
                            } catch (IOException e) {
                                System.err.println("[Client] Reconnect failed: " + e.getMessage());

                                try {
                                    Thread.sleep(RECONNECT_DELAY_MS);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                                continue;
                            }
                        }

                        // Read next event
                        try {
                            Object event = in.readObject();

                            // ✨ Wrap callback in try-catch
                            try {
                                onEvent.accept(event);
                            } catch (Exception callbackError) {
                                System.err.println("[Client] Callback error: " + callbackError.getMessage());
                                callbackError.printStackTrace();
                                // Continue listening despite callback error
                            }

                        } catch (SocketTimeoutException e) {
                            // Timeout, continue loop
                            System.out.println("[Client] Read timeout, retrying...");

                        } catch (EOFException | java.net.SocketException e) {
                            // Connection lost
                            if (listening) {
                                System.out.println("[Client] Connection lost: " + e.getClass().getSimpleName());

                                // Notify app about connection loss
                                try {
                                    onEvent.accept(null);
                                } catch (Exception notifyError) {
                                    System.err.println("[Client] Error notifying connection loss");
                                }
                            }
                            // Loop will try to reconnect
                        }

                    } catch (Exception e) {
                        if (listening) {
                            System.err.println("[Client] Unexpected error: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    }
                }

                System.out.println("[Client] Listener thread exited");

            }, "auction-listener");

            listenerThread.setDaemon(true);
            listenerThread.start();
            System.out.println("[Client] Listener started");
        }
    }

    /**
     * Stop listening for events.
     * ✨ Fix: Use lock to prevent race conditions
     */
    public void stopListening() {
        synchronized (listenerLock) {
            listening = false;
            // Don't set listenerThread = null here, let join() in startListening use it
        }
    }

    /**
     * Dùng khi user bị ban: dừng listener NGAY LẬP TỨC bằng cách đóng socket,
     * sau đó reset stream về null để sendRequest() tự reconnect khi login lại.
     *
     * <p>Tại sao cần method này thay vì chỉ gọi stopListening():</p>
     * <ul>
     *   <li>stopListening() chỉ set flag — listener vẫn blocked trên in.readObject()
     *       tối đa 30 giây (SOCKET_TIMEOUT_MS)</li>
     *   <li>Trong thời gian đó, sendRequest(LOGIN) cũng gọi in.readObject()</li>
     *   <li>Hai thread cùng đọc ObjectInputStream → race condition → ClassCastException</li>
     *   <li>resetForLogin() đóng socket → SocketException → listener thoát ngay</li>
     *   <li>sendRequest() phát hiện socket=null → tự reconnect sạch sẽ</li>
     * </ul>
     */
    public void resetForLogin() {
        // 1. Báo listener dừng — khi SocketException bắn, listener sẽ không
        //    gọi onEvent.accept(null) (tránh hiện "Mất kết nối" sai lệch)
        synchronized (listenerLock) {
            listening = false;
        }

        // 2. Đóng socket → listener đang blocked trên in.readObject() nhận
        //    SocketException và thoát vòng lặp ngay lập tức
        synchronized (connectLock) {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("[Client] resetForLogin close error: " + e.getMessage());
            }
            // Xóa references — sendRequest() sẽ phát hiện socket=null
            // và tự gọi reconnect() trước khi gửi request
            socket = null;
            in     = null;
            out    = null;
        }

        System.out.println("[Client] Connection reset. Sẵn sàng cho re-login.");
    }

    /**
     * Check if currently listening.
     */
    public boolean isListening() {
        return listening;
    }

    // ── Disconnect ─────────────────────────────────────────────────────────

    /**
     * Gracefully disconnect.
     */
    public void disconnect() {
        try {
            stopListening();

            if (listenerThread != null && listenerThread.isAlive()) {
                try {
                    listenerThread.join(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            synchronized (connectLock) {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }

            System.out.println("[Client] Disconnected");

        } catch (IOException e) {
            System.err.println("[Client] Error disconnecting: " + e.getMessage());
        }
    }

    /**
     * Get connection status.
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    /**
     * Get current reconnect attempt count.
     */
    public int getReconnectAttempt() {
        return reconnectAttempt;
    }
}