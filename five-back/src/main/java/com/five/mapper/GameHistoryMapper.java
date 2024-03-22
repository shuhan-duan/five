package com.five.mapper;

import com.five.pojo.entity.GameHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


@Mapper
public interface GameHistoryMapper {

    void insert(GameHistory gameHistory);

    @Update("update game_history set end_time=#{endTime} ,game_result=#{gameResult}")
    void update(GameHistory gameHistory);

    @Select("select * from game_history where room_id=#{roomId} order by begin_time desc limit 1 ")
    GameHistory selectByRoomId(Long roomId);

}
