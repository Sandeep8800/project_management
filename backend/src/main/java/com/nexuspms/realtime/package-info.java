/**
 * HLD S8.4: real-time push over WebSocket/STOMP. Deliberately a separate,
 * independent consumer of the domain event bus -- NOT called by Notifications
 * or Sprint & Board directly, consistent with HLD S8.1's "both subscribe to
 * the domain event bus independently" design. See config/WebSocketConfig.java
 * for the STOMP wiring and auth.
 */
package com.nexuspms.realtime;
