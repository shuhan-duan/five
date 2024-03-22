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
            case 0:
                initializeRoom(roomId, userId, session);
                break;
            case 1:
                addUserToRoom(roomId, userId, session);
                startGameInRoom(roomId);
                break;
            default:
                sendMessageForConfirm(session);
                break;
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

        // 解析json数据
        // 解析消息来确定type，
        OnlineFiveMessage onlineFiveMessage = objectMapper.readValue(message, OnlineFiveMessage.class);
        Integer type = onlineFiveMessage.getType();

        if (type == 1) {    // 1：确认进入，添加用户到room集合中
            roomSessions.get(roomId).put(userId, new OnlineFiveActor("观战者", session));
            sendToAllUserForRoomCount(roomId);
            sendToAllUserForObserver(roomId, userId, session);
            log.info("当前房间id为：{}，加入房间的用户为{}是观战者", roomId, userId);
        } else if (type == 3) {   // 2：聊天消息，广播给所有用户（意味着点击发送按钮时前端先不用渲染）
            sendToAllUser(3, roomId, onlineFiveMessage.getMessage(), userId, onlineFiveMessage.getRole(), 0);
        } else if (type == 4) {   //4: 重头戏，要做三件事情。1：判断棋局状态并把消息广播出去。 2：维护game_details表。
            // 3：结束了以后维护game_history和user表并移除所有session

            Integer[][] board = roomBoards.get(roomId);// 获取这个房间对应的棋盘
            // 更新棋盘状态
            String steps = onlineFiveMessage.getMessage();
            // System.out.println("步数" + steps);
            // 对字符串处理拿到x和y坐标
            String cleanedSteps = steps.replaceAll("[()]", ""); // 去除括号
            String[] split = cleanedSteps.split(","); // 根据逗号分割
            int color = Objects.equals(onlineFiveMessage.getRole(), "执黑棋者") ? 1 : 2;// 获取颜色
            board[Integer.parseInt(split[0])][Integer.parseInt(split[1])] = color;// 更新棋局的状态
            int gameOver = FiveGameUtil.isGameOver(board);
            // 1：判断棋局状态并把消息广播出去
            if (gameOver == 0) {    // 游戏还在进行中，直接广播出去
                sendToAllUser(4, roomId, onlineFiveMessage.getMessage(), userId, onlineFiveMessage.getRole(), gameOver);
            } else {    // 游戏已经结束了
                UserSelectByIdVO userSelectByIdVO = userMapper.selectUserById(userId);
                if (gameOver == 1) {   // 黑色棋子胜利
                    GameHistory gameHistory = GameHistory.builder()
                            .endTime(LocalDateTime.now())
                            .gameResult(0)
                            .build();
                    gameHistoryMapper.update(gameHistory);
                    sendToAllUser(4, roomId, onlineFiveMessage.getMessage(), userId, onlineFiveMessage.getRole(), gameOver);
                    // 3：更新用户表
                    // 更新胜利的那一方
                    User user = User.builder()
                            .gameTotalCounts(userSelectByIdVO.getGameTotalCounts() + 1)
                            .gameSuccessCounts(userSelectByIdVO.getGameSuccessCounts() + 1)
                            .id(userId)
                            .build();
                    userMapper.update(user);
                    // 更新失败的那一方
                    // 获取失败的用户id
                    ConcurrentHashMap<Long, OnlineFiveActor> actors = roomSessions.get(roomId);
                    Long defeatUserId = 0L;
                    for (Long userId1 : actors.keySet()) {
                        OnlineFiveActor actor = actors.get(userId1);
                        if (Objects.equals(actor.getRole(), "执白棋者")) {
                            defeatUserId = userId1;
                        }
                    }
                    User userCopy = User.builder()
                            .gameTotalCounts(userSelectByIdVO.getGameTotalCounts() + 1)
                            .gameFailCounts(userSelectByIdVO.getGameSuccessCounts() + 1)
                            .id(defeatUserId)
                            .build();
                    userMapper.update(userCopy);

                } else if (gameOver == 2) {   // 白色棋子胜利
                    GameHistory gameHistory = GameHistory.builder()
                            .endTime(LocalDateTime.now())
                            .gameResult(1)
                            .build();
                    gameHistoryMapper.update(gameHistory);
                    // 更新用户表
                    // 更新胜利的那一方
                    User user = User.builder()
                            .gameTotalCounts(userSelectByIdVO.getGameTotalCounts() + 1)
                            .gameSuccessCounts(userSelectByIdVO.getGameSuccessCounts() + 1)
                            .id(userId)
                            .build();
                    userMapper.update(user);
                    // 更新失败的那一方
                    // 获取失败的用户id
                    ConcurrentHashMap<Long, OnlineFiveActor> actors = roomSessions.get(roomId);
                    Long defeatUserId = 0L;
                    for (Long userId1 : actors.keySet()) {
                        OnlineFiveActor actor = actors.get(userId1);
                        if (Objects.equals(actor.getRole(), "执黑棋者")) {
                            defeatUserId = userId1;
                        }
                    }
                    User userCopy = User.builder()
                            .gameTotalCounts(userSelectByIdVO.getGameTotalCounts() + 1)
                            .gameFailCounts(userSelectByIdVO.getGameSuccessCounts() + 1)
                            .id(defeatUserId)
                            .build();
                    userMapper.update(userCopy);


                    sendToAllUser(4, roomId, onlineFiveMessage.getMessage(), userId, onlineFiveMessage.getRole(), gameOver);
                } else if (gameOver == 3) { // 平局
                    GameHistory gameHistory = GameHistory.builder()
                            .endTime(LocalDateTime.now())
                            .gameResult(2)
                            .build();
                    gameHistoryMapper.update(gameHistory);

                    sendToAllUser(4, roomId, onlineFiveMessage.getMessage(), userId, onlineFiveMessage.getRole(), gameOver);
                    // 更新用户表
                    ConcurrentHashMap<Long, OnlineFiveActor> actors = roomSessions.get(roomId);
                    for (Long userId1 : actors.keySet()) {
                        OnlineFiveActor actor = actors.get(userId1);
                        if (!Objects.equals(actor.getRole(), "观战者")) {
                            User userCopy = User.builder()
                                    .gameTotalCounts(userSelectByIdVO.getGameTotalCounts() + 1)
                                    .gameDeadHeatCounts(userSelectByIdVO.getGameSuccessCounts() + 1)
                                    .id(userId1)
                                    .build();
                            userMapper.update(userCopy);
                        }
                    }

                }


                // 3：游戏结束，移除这个房间中的所有session和棋盘
                ConcurrentHashMap<Long, OnlineFiveActor> room = roomSessions.get(roomId);
                roomBoards.remove(roomId); // 清除棋盘
                if (room != null) {
                    room.forEach((key, actor) -> {
                        actor.closeSession(); // 关闭连接
                    });
                    room.clear();   // 清除map中的session和其他信息
                }

            }
        }


    }

    // 该方法用于关闭连接
    @OnClose
    public void onClose(Session session, @PathParam("roomId") Long roomId, @PathParam("userId") Long userId) throws IOException {
        log.info("房间{}退出了用户{}", roomId, userId);
        // 判断当前退出的是不是对战者
        ConcurrentHashMap<Long, OnlineFiveActor> map = roomSessions.get(roomId);
        String role = map.get(userId).getRole();
        if (!Objects.equals(role, "观战者")) {
            // 这样直接移除所有session
            // 移除这个房间中的所有session和棋盘
            ConcurrentHashMap<Long, OnlineFiveActor> room = roomSessions.get(roomId);
            roomBoards.remove(roomId); // 清除棋盘
            if (room != null) {
                room.forEach((key, actor) -> {
                    actor.closeSession(); // 关闭连接
                });
                room.clear();   // 清除map中的session和其他信息
            }
            roomSessions.remove(roomId);    // 从map中移除
        } else {
            map.remove(userId);   // 移除当前这个用户信息
            sendToAllUserForRoomCount(roomId);
        }
    }

    // 该方法用于处理错误
    @OnError
    public void onError(@PathParam("roomId") Long roomId, @PathParam("userId") Long userId, Throwable throwable) {
        log.error("房间{}，用户{}的连接发生错误", roomId, userId, throwable);
    }


    // 该方法用于广播房间人数变化
    private void sendToAllUserForRoomCount(Long roomId) {
        // 获取当前房间的所有session
        ConcurrentHashMap<Long, OnlineFiveActor> sessionsMap = roomSessions.get(roomId);
        log.info("广播人数变化{}", roomId);

        Enumeration<Long> keys = sessionsMap.keys();
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
        List<GameActor> list = Collections.synchronizedList(new ArrayList<>());
        // 遍历集合
        while (keys.hasMoreElements()) {
            Long userId = keys.nextElement();
            // 在这里处理每个key
            // 根据id查询用户数据
            UserSelectByIdVO userSelectByIdVO = userMapper.selectUserById(userId);
            GameActor gameActor = new GameActor();
            BeanUtils.copyProperties(userSelectByIdVO, gameActor);
            OnlineFiveActor actor = sessionsMap.get(userId);
            gameActor.setRole(actor.getRole());
            list.add(gameActor);
        }
        map.put("type", 6);
        map.put("actors", list);
        try {
            String jsonText = objectMapper.writeValueAsString(map);
            System.out.println("广播人数变化的文本：" + jsonText);
            // 转换成json
            keys = sessionsMap.keys();
            while (keys.hasMoreElements()) {
                Long userId = keys.nextElement();
                OnlineFiveActor actor = sessionsMap.get(userId);
                if (actor != null) {
                    log.info("向用户广播：{}", userId);
                    actor.getSession().getBasicRemote().sendText(jsonText);
                }
            }
        } catch (Exception e) {
            log.error("广播人数变化时出现了异常:{}", e.getMessage());
        }


    }


    // 该方法用于向用户发送消息 1：确认进入房间 2：确认角色
    private void sendUserActorMessage(Session session, String role, Long gameId) {
        if (session != null && session.isOpen()) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", 2);
            map.put("role", role);
            map.put("gameId", gameId);
            try {
                String jsonMessage = objectMapper.writeValueAsString(map);
                session.getBasicRemote().sendText(jsonMessage);
            } catch (IOException e) {
                log.error("向用户发送警告信息时出现了异常");
            }
        }
    }


    // 该方法用于向所有观战者发送消息
    private void sendToAllUserForObserver(Long roomId, Long userId, Session session) {

        try {
            // 只给新加的观战者发
            // 组装数据
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", 7);
            map.put("id", userId);
            map.put("role", "观战者");
            map.put("message", roomBoards.get(roomId));
            int gameOver = FiveGameUtil.isGameOver(roomBoards.get(roomId));
            map.put("isGameOver", gameOver);
            String jsonMessage = objectMapper.writeValueAsString(map);
            // 服务器向客户端发送消息
            session.getBasicRemote().sendText(jsonMessage);
        } catch (Exception e) {
            log.error("传输数据发生异常");
        }


    }

    // 该方法用于提示用户是否还要继续加入房间——房间人数大于2的时候
    private void sendMessageForConfirm(Session session) {
        if (session != null && session.isOpen()) {
            HashMap<String, Integer> map = new HashMap<>();
            map.put("type", 0);
            try {
                String jsonMessage = objectMapper.writeValueAsString(map);
                session.getBasicRemote().sendText(jsonMessage);
            } catch (IOException e) {
                log.error("向用户发送警告信息时出现了异常");
            }
        }

    }

    // 该方法用于向所有用户发送消息
    public void sendToAllUser(Integer type, Long roomId, String message, Long userId, String role, Integer isGameOver) {
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
                log.info("josnMessage:{}", jsonMessage);
                // 服务器向客户端发送消息
                session.getSession().getBasicRemote().sendText(jsonMessage);
            } catch (Exception e) {
                log.error("传输数据发生异常");
            }
        }
    }
}

