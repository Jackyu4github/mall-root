package com.mall.notice.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.auth.AuthUser;              // 你的 auth-core 里那个 AuthUser/TokenInfo
import com.mall.notice.model.NotifyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserNotifyWebSocketHandler implements WebSocketHandler {

    // userId -> Set<session>
    private final ConcurrentMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    Authentication authentication = securityContext.getAuthentication();
                    if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
                        return session.close(CloseStatus.NOT_ACCEPTABLE.withReason("UNAUTHORIZED"));
                    }
                    Long userId = authUser.userId();

                    addSession(userId, session);
                    log.info("WS connected: userId={} session={}", userId, session.getId());

                    Mono<Void> input = session.receive()
                            .doOnNext(msg -> {
                                // 如果需要心跳/ack，可在这里处理
                                // String payload = msg.getPayloadAsText();
                            })
                            .then();

                    return input.doFinally(signalType -> {
                        removeSession(userId, session);
                        log.info("WS disconnected: userId={} session={}", userId, session.getId());
                    });
                });
    }

    private void addSession(Long userId, WebSocketSession session) {
        userSessions.compute(userId, (k, set) -> {
            if (set == null) set = ConcurrentHashMap.newKeySet();
            set.add(session);
            return set;
        });
    }

    private void removeSession(Long userId, WebSocketSession session) {
        userSessions.computeIfPresent(userId, (k, set) -> {
            set.remove(session);
            return set.isEmpty() ? null : set;
        });
    }

    // 被 Redis 订阅方调用：给指定用户发消息
    public void sendToUser(Long userId, NotifyMessage msg) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            String json = objectMapper.writeValueAsString(msg);

            sessions.forEach(s -> {
                if (s.isOpen()) {
                    s.send(Mono.just(s.textMessage(json)))
                            .doOnError(e -> log.error("WS send error userId={}, session={}", userId, s.getId(), e))
                            .subscribe();
                }
            });

        } catch (Exception e) {
            log.error("serialize NotifyMessage error", e);
        }
    }

    public void broadcast(NotifyMessage msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);

            userSessions.values().forEach(set -> {
                set.forEach(s -> {
                    if (s.isOpen()) {
                        s.send(Mono.just(s.textMessage(json)))
                                .doOnError(e -> log.error("WS broadcast error", e))
                                .subscribe();
                    }
                });
            });
        } catch (Exception e) {
            log.error("serialize NotifyMessage error", e);
        }
    }

}
