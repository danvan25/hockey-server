package com.example.hockeyserver.repository;

import com.example.hockeyserver.entity.Lobby;
import com.example.hockeyserver.entity.LobbyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface LobbyRepository extends JpaRepository<Lobby, Long> {

    boolean existsByRoomCode(String roomCode);

    Optional<Lobby> findByRoomCode(String roomCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lobby from Lobby lobby where lobby.roomCode = :roomCode")
    Optional<Lobby> findByRoomCodeForUpdate(
            @Param("roomCode") String roomCode
    );

    @Query("""
            select case when count(lobby) > 0 then true else false end
            from Lobby lobby
            where lobby.status in :statuses
              and (lobby.host.id = :userId or lobby.guest.id = :userId)
            """)
    boolean existsActiveLobbyForUser(
            @Param("userId") Long userId,
            @Param("statuses") Collection<LobbyStatus> statuses
    );

    @Query("""
            select lobby from Lobby lobby
            where lobby.status in :statuses
              and (lobby.host.id = :userId or lobby.guest.id = :userId)
            """)
    Optional<Lobby> findActiveLobbyForUser(
            @Param("userId") Long userId,
            @Param("statuses") Collection<LobbyStatus> statuses
    );
}
