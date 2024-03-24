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
    

    // Use ConcurrentHashMap to store the room ID and the associated user information
    private static ConcurrentHashMap<Long, ConcurrentHashMap<Long, OnlineFiveActor>> roomSessions = new ConcurrentHashMap<>();
    // Store the chessboard for each room
    private static ConcurrentHashMap<Long, Integer[][]> roomBoards = new ConcurrentHashMap<>();


    // Expose for server performance monitoring module access
    public ConcurrentHashMap<Long, ConcurrentHashMap<Long, OnlineFiveActor>> getRoomSessions() {
        return roomSessions;
    }

    public ConcurrentHashMap<Long, Integer[][]> getRoomBoards() {
        return roomBoards;
    }

    private ObjectMapper objectMapper = new ObjectMapper(); // Object mapper for Jackson


    // This method is used to establish a connection
    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") Long roomId, @PathParam("userId") Long userId) {
        roomSessions.putIfAbsent(roomId, new ConcurrentHashMap<>()); // Ensure the room exists
        ConcurrentHashMap<Long, OnlineFiveActor> usersInRoom = roomSessions.get(roomId);
        log.info("The room id is: {}, the user is {}, onOpen()", roomId, userId);
        // Handle new connection based on the number of existing users in the room
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
            Arrays.fill(row, 0); // Initialize the board
        }
        roomBoards.put(roomId, board); // Store the board

        roomSessions.get(roomId).put(userId, new OnlineFiveActor("Joueur Noir", session));
        log.info("The current room id is: {}, the user joining the room is {}, black", roomId, userId);
    }

    private void addUserToRoom(Long roomId, Long userId, Session session) {
        roomSessions.get(roomId).put(userId, new OnlineFiveActor("Joueur Blanc", session));
        log.info("The current room id is: {}, the user joining the room is {}, white", roomId, userId);
    }

    private void startGameInRoom(Long roomId) {
        ConcurrentHashMap<Long, OnlineFiveActor> map = roomSessions.get(roomId);
        List<Long> userIds = new ArrayList<>(map.keySet());
        Long blackId = userIds.get(0);
        Long whiteId = userIds.size() > 1 ? userIds.get(1) : null; // Prevents the case of having only one user

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
            sendUserActorMessage(whiteSession, "Joueur Blanc", gameHistory.getId());
        }
        sendUserActorMessage(blackSession, "Joueur Noir", gameHistory.getId());
        sendToAllUserForRoomCount(roomId);
    }


    // This method is used to receive messages sent by the frontend
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
        roomSessions.get(roomId).put(userId, new OnlineFiveActor("Spectateur", session));
        sendToAllUserForRoomCount(roomId);
        sendToAllUserForObserver(roomId, userId, session);
        log.info("The current room id is: {}, the user joining the room is {} and is an Observer", roomId, userId);
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
        Integer[][] board = roomBoards.get(roomId);// Get the board corresponding to this room
        // Update the board state
        String steps = onlineFiveMessage.getMessage();
        String cleanedSteps = steps.replaceAll("[()]", ""); // Remove parentheses
        String[] split = cleanedSteps.split(","); // Split by comma
        int color = Objects.equals(onlineFiveMessage.getRole(), "Black Player") ? 1 : 2;// Get the color
        board[Integer.parseInt(split[0])][Integer.parseInt(split[1])] = color;// Update the state of the board
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
                    .game_total_counts(userInfo.getGame_total_counts() + 1)
                    .build();

            // Determine win/lose/draw count increment based on game result and user role
            switch (gameResult) {
                case BLACK_WIN:
                case WHITE_WIN:
                    if ((gameResult == BLACK_WIN && actors.get(actorUserId).getRole().equals("Joueur Noir")) ||
                            (gameResult == WHITE_WIN && actors.get(actorUserId).getRole().equals("Joueur Blanc"))) {
                        user.setGame_success_counts(userInfo.getGame_success_counts() + 1);
                    } else {
                        user.setGame_fail_counts(userInfo.getGame_fail_counts() + 1);
                    }
                    break;
                case DRAW:
                    user.setGame_dead_heat_counts(userInfo.getGame_dead_heat_counts() + 1);
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

        if (!"Spectateur".equals(role)) {
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

    // This method is used to handle errors
    @OnError
    public void onError(@PathParam("roomId") Long roomId, @PathParam("userId") Long userId, Throwable throwable) {
        log.error("Error occurred in room {}, user {}'s connection", roomId, userId, throwable);
    }

    // This method is used to broadcast the change in the number of users in the room
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
            map.put("role", "Spectateur");
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
            map.put("type", WARNING.getValue());
            try {
                String jsonMessage = objectMapper.writeValueAsString(map);
                session.getBasicRemote().sendText(jsonMessage);
            } catch (IOException e) {
                log.error("An error occurred while sending a warning message to the user", e);
            }
        }
    }


    // This method is used to send messages to all users
    public void sendToAllUser(MessageType type, Long roomId, String message, Long userId, String role, GameResult isGameOver) {
        // Get all sessions in the current room
        ConcurrentHashMap<Long, OnlineFiveActor> sessionsMap = roomSessions.get(roomId);
        Collection<OnlineFiveActor> sessionUsers = sessionsMap.values();   // Get all values
        for (OnlineFiveActor session : sessionUsers) {
            try {
                // Assemble data
                HashMap<Object, Object> map = new HashMap<>();
                map.put("type", type.getValue());
                map.put("id", userId);
                map.put("role", role);
                map.put("message", message);
                map.put("isGameOver", isGameOver.getValue());
                String jsonMessage = objectMapper.writeValueAsString(map);
                log.info("jsonMessage:{}", jsonMessage);
                // Server sends message to client
                session.getSession().getBasicRemote().sendText(jsonMessage);
            } catch (Exception e) {
                log.error("An exception occurred during data transmission");
            }
        }
    }
}

