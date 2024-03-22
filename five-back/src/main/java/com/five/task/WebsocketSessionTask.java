package com.five.task;

import com.five.mapper.GameHistoryMapper;
import com.five.pojo.entity.GameHistory;
import com.five.pojo.pojo.OnlineFiveActor;
import com.five.websocket.OnlineFiveServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class WebsocketSessionTask {

    @Autowired
    private OnlineFiveServer onlineFiveServer;

    @Autowired
    private GameHistoryMapper gameHistoryMapper;

    private static AtomicLong closeRoomCount = new AtomicLong(0); // Use atomic operation to avoid concurrency issues in multi-threading

    public AtomicLong getCloseRoomCount() {
        return closeRoomCount;
    }

    
    @Scheduled(cron = "0 0 * * * ?")  // Execute every hour
    public void removeSessions() {
        // Get roomSessions and roomBoards
        ConcurrentHashMap<Long, ConcurrentHashMap<Long, OnlineFiveActor>> roomSessions = onlineFiveServer.getRoomSessions();
        ConcurrentHashMap<Long, Integer[][]> roomBoards = onlineFiveServer.getRoomBoards();

        // Iterate through all rooms and check how long they have been active
        for (Map.Entry<Long, ConcurrentHashMap<Long, OnlineFiveActor>> roomEntry : roomSessions.entrySet()) {
            Long roomId = roomEntry.getKey(); // Room ID
            ConcurrentHashMap<Long, OnlineFiveActor> userSessions = roomEntry.getValue();

            // Perform validation logic here
            GameHistory gameHistory = gameHistoryMapper.selectByRoomId(roomId);

            if (gameHistory != null) {
                // If the difference between the current time and the start time is greater than one hour, close the connections
                LocalDateTime beginTime = gameHistory.getBeginTime();
                // Check the time difference between the current time and the start time
                if (Duration.between(beginTime, LocalDateTime.now()).toHours() > 1) {
                    long l = closeRoomCount.incrementAndGet();
                    log.info("Room {} has exceeded the time limit, closing all sessions. Total closed rooms: {}", roomId, l);
                    // If the time difference is greater than one hour, close all related sessions
                    for (OnlineFiveActor actor : userSessions.values()) {
                        try {
                            if (actor.getSession().isOpen()) {
                                actor.getSession().close();
                            }
                        } catch (IOException e) {
                            log.error("An exception occurred while closing user session in scheduled task: {}", e.getMessage());
                        }
                    }
                    // Remove the sessions
                    roomSessions.remove(roomId);
                    roomBoards.remove(roomId);
                }
            }
        }

    }


}
