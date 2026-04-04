import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function useWebSocket(accessToken) {
  const clientRef = useRef(null);
  const [lastMessage, setLastMessage] = useState(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!accessToken) return;
    // Don't connect if there's no token — user not logged in yet

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8083/ws'),
      // ↑ SockJS factory — @stomp/stompjs calls this to create the transport

      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
        // ↑ This header lands in the STOMP CONNECT frame.
        //   JwtChannelInterceptor in Notification Service reads it here.
      },

      onConnect: () => {
        setConnected(true);
        client.subscribe('/user/queue/notifications', (message) => {
          try {
            const parsed = JSON.parse(message.body);
            setLastMessage(parsed);
            // ↑ Every time a notification arrives, we update lastMessage.
            //   Components that use this hook re-render automatically.
          } catch {
            console.warn('Failed to parse WebSocket message:', message.body);
          }
        });
      },

      onDisconnect: () => setConnected(false),

      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
      },

      reconnectDelay: 5000,
      // ↑ If connection drops, retry every 5 seconds automatically.
    });

    client.activate();
    clientRef.current = client;

    return () => {
      // Cleanup on unmount or when token changes
      client.deactivate();
    };
  }, [accessToken]);
  // ↑ Re-run when accessToken changes — connects after login, disconnects after logout

  return { lastMessage, connected };
}