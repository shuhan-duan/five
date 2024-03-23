package com.five.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.five.config.CustomSpringConfigurator;
import com.five.mapper.GameHistoryMapper;
import com.five.mapper.UserMapper;
import com.five.pojo.entity.GameHistory;
import com.five.pojo.entity.User;
import com.five.pojo.pojo.OnlineFiveActor;
import com.five.pojo.pojo.OnlineFiveMessage;
import com.five.pojo.vo.GameActor;
import com.five.pojo.vo.UserSelectByIdVO;
import com.five.utils.FiveGameUtil;
import com.five.utils.GameResult;
import com.five.utils.MessageType;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.five.utils.MessageType.*;
import static com.five.utils.GameResult.*;


@ServerEndpoint(value = "/game/online/five/{roomId}/{userId}", configurator = CustomSpringConfigurator.class)
@Slf4j
@Component
public class OnlineFiveServer {


    @Autowired
    private GameHistoryMapper gameHistoryMapper;

    @Autowired
    private UserMapper userMapper;
    

    // 使用 ConcurrentHashMap 来存储房间id和房间所关联的用户信息
    private static ConcurrentHashMap<Long, ConcurrentHashMap<Long, OnlineFiveActor>> roomSessions = new ConcurrentHashMap<>();
    // 存储每个房间对应的棋盘
    private static ConcurrentHashMap<Long, Integer[][]> roomBoards = new ConcurrentHashMap<>();


    // 暴露出去供服务器性能监控模块访问
    public ConcurrentHashMap<Long, ConcurrentHashMap<Long, OnlineFiveActor>> getRoomSessions() {
        return roomSessions;
    }

    public ConcurrentHashMap<Long, Integer[][]> getRoomBoards() {
        return roomBoards;
    }

    private ObjectMapper objectMapper = new ObjectMapper(); // Jackson的对象映射器


    // 该方法用于建立连接
    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") Long roomId, @PathParam("userId") Long userId) {
        roomSessions.putIfAbsent(roomId, new ConcurrentHashMap<>()); // 确保房间存在
        ConcurrentHashMap<Long, OnlineFiveActor> usersInRoom = roomSessions.get(roomId);

        // 根据房间中已有用户数量处理新连接
        switch (usersInRoom.size()) {
            case 0 -> initializeRoom(roomId, userId, session);
            case 1 -> {
                addUserToRoom(roomId, userId, session);
                startGameInRoom(roomId);
            }
            default -> sendMessageForConfirm(session);
        }
    }

    private void initializeRoom(Long roomId, Long userId, Session session) {
        Integer[][] board = new Integer[15][15];
        for (Integer[] row : board) {
            Arrays.fill(row, 0); // 初始化棋盘
        }
        roomBoards.put(roomId, board); // 存储棋盘

        roomSessions.get(roomId).put(userId, new OnlineFiveActor("执黑棋者", session));
        log.info("初始化房间，当前房间id为：{}，建立房间的用户{}执黑子", roomId, userId);
    }

    private void addUserToRoom(Long roomId, Long userId, Session session) {
        roomSessions.get(roomId).put(userId, new OnlineFiveActor("执白棋者", session));
        log.info("当前房间id为：{}，加入房间的用户为{}执白子", roomId, userId);
    }

    private void startGameInRoom(Long roomId) {
        ConcurrentHashMap<Long, OnlineFiveActor> map = roomSessions.get(roomId);
        List<Long> userIds = new ArrayList<>(map.keySet());
        Long blackId = userIds.get(0);
        Long whiteId = userIds.size() > 1 ? userIds.get(1) : null; // 防止出现只有一个用户的情况

        Session blackSession = map.get(blackId).getSession();
        Session whiteSession = whiteId != null ? map.get(whiteId).getSession() : null;

        GameHistory gameHistory = GameHistory.builder()
                .roomId(roomId)
                .whiteId(whiteId)
                .blackId(blackId)
                .beginTime(LocalDateTime.now())
                .build();
        gameHistoryMapper.insert(gameHistory);

        if (whiteSession != null) {
            sendUserActorMessage(whiteSession, "执白棋者", gameHistory.getId());
        }
        sendUserActorMessage(blackSession, "执黑棋者", gameHistory.getId());
        sendToAllUserForRoomCount(roomId);
    }


    // 该方法用于接收前端发送的消息
    @OnMessage
    public void onMessage(Session session, @PathParam("roomId") Long roomId, @PathParam("userId") Long userId, String message) throws JsonProcessingException {
        OnlineFiveMessage onlineFiveMessage = objectMapper.readValue(message, OnlineFiveMessage.class);
        switch (onlineFiveMessage.getType()) {
            case JOIN_ROOM -> handleJoinRoom(roomId, userId, session);
            case CHAT_MESSAGE -> handleChatMessage(roomId, userId, onlineFiveMessage);
            case MOVE -> handleMove(roomId, userId, onlineFiveMessage);
            default -> log.warn("Unknown message type: {}", onlineFiveMessage.getType());
        }
    }

    private void handleChatMessage(Long roomId, Long userId, OnlineFiveMessage onlineFiveMessage) {
        sendToAllUser(CHAT_MESSAGE, roomId, onlineFiveMessage.getMessage(), userId, onlineFiveMessage.getRole(), CONTINUE);
    }

    private void handleJoinRoom(Long roomId, Long userId, Session session) {
        roomSessions.get(roomId).put(userId, new OnlineFiveActor("观战者", session));
        sendToAllUserForRoomCount(roomId);
        sendToAllUserForObserver(roomId, userId, session);
        log.info("当前房间id为：{}，加入房间的用户为{}是观战者", roomId, userId);
    }

    private void handleMove(Long roomId, Long userId, OnlineFiveMessage onlineFiveMessage) {
        try {
            GameResult gameResult = updateBoardAndCheckGameState(roomId, onlineFiveMessage);

            if (gameResult == GameResult.CONTINUE) {
                broadcastMove(roomId, userId, onlineFiveMessage);
            } else {
                concludeGame(roomId, userId, gameResult, onlineFiveMessage);
            }
        } catch (Exception e) {
            log.error("Error handling move: ", e);
            // Handle the error appropriately
        }
    }

    private GameResult updateBoardAndCheckGameState(Long roomId, OnlineFiveMessage onlineFiveMessage) {
        // Assuming you have a method to parse the message and update the board.
        // Also assuming FiveGameUtil.isGameOver now returns an instance of GameResult.
        updateBoard(roomId, onlineFiveMessage);
        return FiveGameUtil.isGameOver(roomBoards.get(roomId));
    }

    private void updateBoard(Long roomId, OnlineFiveMessage onlineFiveMessage) {
        Integer[][] board = roomBoards.get(roomId);// 获取这个房间对应的棋盘
        // 更新棋盘状态
        String steps = onlineFiveMessage.getMessage();
        String cleanedSteps = steps.replaceAll("[()]", ""); // 去除括号
        String[] split = cleanedSteps.split(","); // 根据逗号分割
        int color = Objects.equals(onlineFiveMessage.getRole(), "执黑棋者") ? 1 : 2;// 获取颜色
        board[Integer.parseInt(split[0])][Integer.parseInt(split[1])] = color;// 更新棋局的状态
    }

    private void broadcastMove(Long roomId, Long userId, OnlineFiveMessage onlineFiveMessage) {
        sendToAllUser(MOVE, roomId, onlineFiveMessage.getMessage(), userId, onlineFiveMessage.getRole(), CONTINUE);
    }

    private void concludeGame(Long roomId, Long userId, GameResult gameResult, OnlineFiveMessage onlineFiveMessage) {
        // Handle game conclusion, update records, send final messages etc.
        handleGameOverActions(roomId, userId, gameResult ,onlineFiveMessage);
        clearRoomAfterGame(roomId);
    }

    private void handleGameOverActions(Long roomId, Long userId, GameResult gameResult, OnlineFiveMessage onlineFiveMessage) {
        // Record the game history
        recordGameHistory(gameResult);

        // Send final state message to all users
        sendToAllUser(MOVE, roomId, onlineFiveMessage.getMessage(), userId, onlineFiveMessage.getRole(), gameResult);

        // Update user statistics
        updateUsersStatistics(roomId, gameResult);
    }

    private void recordGameHistory(GameResult gameResult) {
        GameHistory gameHistory = GameHistory.builder()
                .endTime(LocalDateTime.now())
                .gameResult(gameResult.getValue())
                .build();
        gameHistoryMapper.update(gameHistory);
    }

    private void updateUsersStatistics(Long roomId, GameResult gameResult) {
        ConcurrentHashMap<Long, OnlineFiveActor> actors = roomSessions.get(roomId);
        actors.keySet().forEach(actorUserId -> {
            UserSelectByIdVO userInfo = userMapper.selectUserById(actorUserId);
            User user = User.builder()
                    .id(actorUserId)
                    .gameTotalCounts(userInfo.getGameTotalCounts() + 1)
                    .build();

            // Determine win/lose/draw count increment based on game result and user role
            switch (gameResult) {
                case BLACK_WIN:
                case WHITE_WIN:
                    if ((gameResult == BLACK_WIN && actors.get(actorUserId).getRole().equals("执黑棋者")) ||
                            (gameResult == WHITE_WIN && actors.get(actorUserId).getRole().equals("执白棋者"))) {
                        user.setGameSuccessCounts(userInfo.getGameSuccessCounts() + 1);
                    } else {
                        user.setGameFailCounts(userInfo.getGameFailCounts() + 1);
                    }
                    break;
                case DRAW:
                    user.setGameDeadHeatCounts(userInfo.getGameDeadHeatCounts() + 1);
                    break;
                default:
                    // No action needed for CONTINUE
                    break;
            }

            userMapper.update(user);
        });
    }
    @OnClose
    public void onClose(@PathParam("roomId") Long roomId, @PathParam("userId") Long userId) {
        log.info("User {} left room {}", userId, roomId);

        // Retrieve the room and check if it exists
        ConcurrentHashMap<Long, OnlineFiveActor> roomActors = roomSessions.get(roomId);
        if (roomActors == null) {
            log.warn("Room {} does not exist.", roomId);
            return;
        }

        // Check the role of the leaving user
        OnlineFiveActor leavingActor = roomActors.get(userId);
        if (leavingActor == null) {
            log.warn("User {} not found in room {}.", userId, roomId);
            return;
        }
        String role = leavingActor.getRole();

        if (!"观战者".equals(role)) {
            // If the user is not an observer, clear the room for the next game.
            clearRoomAfterGame(roomId);
            log.info("Cleared room {} after a player left.", roomId);
        } else {
            // If the user is an observer, just remove the user from the room.
            roomActors.remove(userId);
            sendToAllUserForRoomCount(roomId);
            log.info("Removed observer {} from room {}. Updated room count.", userId, roomId);
        }
    }

    private void clearRoomAfterGame(Long roomId)     {
        // Clear the room's state for the next game.
        ConcurrentHashMap<Long, OnlineFiveActor> room = roomSessions.get(roomId);
        if (room != null) {
            room.forEach((userId, actor) -> {
                actor.closeSession(); // Try to close each session safely.
            });
            room.clear();   // Clear all sessions and other information from the map.
        }
        roomBoards.remove(roomId); // Remove the board associated with the room.
        roomSessions.remove(roomId);    // Remove the room from the sessions map.
        log.info("Room {} has been cleared and ready for the next game.", roomId);
    }

    // 该方法用于处理错误
    @OnError
    public void onError(@PathParam("roomId") Long roomId, @PathParam("userId") Long userId, Throwable throwable) {
        log.error("房间{}，用户{}的连接发生错误", roomId, userId, throwable);
    }


    // 该方法用于广播房间的人数变化
    private void sendToAllUserForRoomCount(Long roomId) {
        ConcurrentHashMap<Long, OnlineFiveActor> sessionsMap = roomSessions.get(roomId);
        if (sessionsMap == null) {
            log.info("Room {} does not exist or has no sessions.", roomId);
            return;
        }

        log.info("Broadcasting room count change for room {}", roomId);

        List<GameActor> list = new ArrayList<>();
        sessionsMap.forEach((userId, actor) -> {
            UserSelectByIdVO userSelectByIdVO = userMapper.selectUserById(userId);
            if (userSelectByIdVO != null) {
                GameActor gameActor = new GameActor();
                BeanUtils.copyProperties(userSelectByIdVO, gameActor);
                gameActor.setRole(actor.getRole());
                list.add(gameActor);
            }
        });

        Map<String, Object> map = new HashMap<>();
        map.put("type", ROOM_COUNT_UPDATE.getValue());
        map.put("actors", list);

        String jsonText;
        try {
            jsonText = objectMapper.writeValueAsString(map);
            log.debug("Broadcasting room count change text: {}", jsonText);

            sessionsMap.values().forEach(actor -> {
                try {
                    actor.getSession().getBasicRemote().sendText(jsonText);
                } catch (IOException e) {
                    log.error("Error broadcasting room count change to user {}: {}", actor.getRole(), e.getMessage(), e);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing room count change message: {}", e.getMessage(), e);
        }
    }

    // This method sends a message to the user to confirm entrance to the room and assign a role
    private void sendUserActorMessage(Session session, String role, Long gameId) {
        if (session != null && session.isOpen()) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("type", USER_ACTOR_CONFIRM.getValue());
            map.put("role", role);
            map.put("gameId", gameId);
            try {
                String jsonMessage = objectMapper.writeValueAsString(map);
                session.getBasicRemote().sendText(jsonMessage);
            } catch (IOException e) {
                log.error("Exception occurred while sending role confirmation message to the user{}: {}",role,e.getMessage(), e);
            }
        }
    }

    // This method sends a message to all observers
    private void sendToAllUserForObserver(Long roomId, Long userId, Session session) {
        try {
            // Sending the message only to the newly added observers
            // Assembling the data
            HashMap<String, Object> map = new HashMap<>();
            map.put("type", OBSERVER_UPDATE.getValue());
            map.put("id", userId);
            map.put("role", "观战者");
            map.put("message", roomBoards.get(roomId));
            GameResult gameOver = FiveGameUtil.isGameOver(roomBoards.get(roomId));
            map.put("isGameOver", gameOver);
            String jsonMessage = objectMapper.writeValueAsString(map);
            // Server sends the message to the client
            session.getBasicRemote().sendText(jsonMessage);
        } catch (Exception e) {
            log.error("An exception occurred during data transmission", e);
        }
    }


    // This method prompts the user to confirm whether they want to continue joining a room
    // when the room's player count exceeds 2
    private void sendMessageForConfirm(Session session) {
        if (session != null && session.isOpen()) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("type", CONFIRM_JOIN_ROOM.getValue());
            try {
                String jsonMessage = objectMapper.writeValueAsString(map);
                session.getBasicRemote().sendText(jsonMessage);
            } catch (IOException e) {
                log.error("An error occurred while sending a warning message to the user", e);
            }
        }
    }


    // 该方法用于向所有用户发送消息
    public void sendToAllUser(MessageType type, Long roomId, String message, Long userId, String role, GameResult isGameOver) {
        // 获取当前房间的所有session
        ConcurrentHashMap<Long, OnlineFiveActor> sessionsMap = roomSessions.get(roomId);
        Collection<OnlineFiveActor> sessionUsers = sessionsMap.values();   // 获取所有值
        for (OnlineFiveActor session : sessionUsers) {
            try {
                // 组装数据
                HashMap<Object, Object> map = new HashMap<>();
                map.put("type", type);
                map.put("id", userId);
                map.put("role", role);
                map.put("message", message);
                map.put("isGameOver", isGameOver);
                String jsonMessage = objectMapper.writeValueAsString(map);
                log.info("jsonMessage:{}", jsonMessage);
                // 服务器向客户端发送消息
                session.getSession().getBasicRemote().sendText(jsonMessage);
            } catch (Exception e) {
                log.error("传输数据发生异常");
            }
        }
    }
}

