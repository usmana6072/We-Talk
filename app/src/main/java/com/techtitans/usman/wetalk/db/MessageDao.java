package com.techtitans.usman.wetalk.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Query("SELECT * FROM (SELECT * FROM messages WHERE chatRoomId = :chatRoomId ORDER BY messageTime DESC LIMIT :limit) ORDER BY messageTime ASC")
    LiveData<List<MessageEntity>> observeLatestMessages(String chatRoomId, int limit);

    @Query("SELECT * FROM (SELECT * FROM messages WHERE chatRoomId = :chatRoomId ORDER BY messageTime DESC LIMIT :limit) ORDER BY messageTime ASC")
    List<MessageEntity> getLatestMessagesSync(String chatRoomId, int limit);

    @Query("SELECT * FROM (SELECT * FROM messages WHERE chatRoomId = :chatRoomId AND messageTime < :beforeTimestamp ORDER BY messageTime DESC LIMIT :limit) ORDER BY messageTime ASC")
    List<MessageEntity> getOlderMessagesSync(String chatRoomId, long beforeTimestamp, int limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<MessageEntity> messages);

    @Query("DELETE FROM messages WHERE chatRoomId = :chatRoomId AND messageId = :messageId")
    void deleteMessage(String chatRoomId, String messageId);

    @Query("SELECT MAX(messageTime) FROM messages WHERE chatRoomId = :chatRoomId")
    Long getNewestTimestamp(String chatRoomId);

    @Query("SELECT MIN(messageTime) FROM messages WHERE chatRoomId = :chatRoomId")
    Long getOldestTimestamp(String chatRoomId);

    @Query("SELECT COUNT(*) FROM messages WHERE chatRoomId = :chatRoomId")
    int getMessageCount(String chatRoomId);

    @Query("SELECT COUNT(*) FROM messages WHERE chatRoomId = :chatRoomId AND messageTime < :beforeTimestamp")
    int getOlderMessageCount(String chatRoomId, long beforeTimestamp);
}
