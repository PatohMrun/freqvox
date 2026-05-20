package com.freqvox.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.freqvox.data.local.entities.SkipHistoryEntity;
import com.freqvox.data.local.entities.TrackEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TrackDao_Impl implements TrackDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TrackEntity> __insertionAdapterOfTrackEntity;

  private final EntityInsertionAdapter<SkipHistoryEntity> __insertionAdapterOfSkipHistoryEntity;

  private final EntityDeletionOrUpdateAdapter<TrackEntity> __updateAdapterOfTrackEntity;

  private final SharedSQLiteStatement __preparedStmtOfIncrementPlayCount;

  private final SharedSQLiteStatement __preparedStmtOfIncrementTrackSkipCount;

  public TrackDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrackEntity = new EntityInsertionAdapter<TrackEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tracks` (`id`,`title`,`artist`,`album`,`duration`,`dateAdded`,`path`,`artworkPath`,`playCount`,`lastPlayed`,`totalPlayTime`,`skipCount`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrackEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTitle() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTitle());
        }
        if (entity.getArtist() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getArtist());
        }
        if (entity.getAlbum() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getAlbum());
        }
        statement.bindLong(5, entity.getDuration());
        statement.bindLong(6, entity.getDateAdded());
        if (entity.getPath() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPath());
        }
        if (entity.getArtworkPath() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getArtworkPath());
        }
        statement.bindLong(9, entity.getPlayCount());
        if (entity.getLastPlayed() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getLastPlayed());
        }
        statement.bindLong(11, entity.getTotalPlayTime());
        statement.bindLong(12, entity.getSkipCount());
      }
    };
    this.__insertionAdapterOfSkipHistoryEntity = new EntityInsertionAdapter<SkipHistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `skip_history` (`id`,`trackId`,`skippedAt`) VALUES (nullif(?, 0),?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SkipHistoryEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTrackId());
        statement.bindLong(3, entity.getSkippedAt());
      }
    };
    this.__updateAdapterOfTrackEntity = new EntityDeletionOrUpdateAdapter<TrackEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `tracks` SET `id` = ?,`title` = ?,`artist` = ?,`album` = ?,`duration` = ?,`dateAdded` = ?,`path` = ?,`artworkPath` = ?,`playCount` = ?,`lastPlayed` = ?,`totalPlayTime` = ?,`skipCount` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrackEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTitle() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTitle());
        }
        if (entity.getArtist() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getArtist());
        }
        if (entity.getAlbum() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getAlbum());
        }
        statement.bindLong(5, entity.getDuration());
        statement.bindLong(6, entity.getDateAdded());
        if (entity.getPath() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPath());
        }
        if (entity.getArtworkPath() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getArtworkPath());
        }
        statement.bindLong(9, entity.getPlayCount());
        if (entity.getLastPlayed() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getLastPlayed());
        }
        statement.bindLong(11, entity.getTotalPlayTime());
        statement.bindLong(12, entity.getSkipCount());
        statement.bindLong(13, entity.getId());
      }
    };
    this.__preparedStmtOfIncrementPlayCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE tracks SET playCount = playCount + 1, lastPlayed = ?, totalPlayTime = totalPlayTime + ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementTrackSkipCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE tracks SET skipCount = skipCount + 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertTracks(final List<TrackEntity> tracks,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTrackEntity.insert(tracks);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSkipEvent(final SkipHistoryEntity skip,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSkipHistoryEntity.insert(skip);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTrack(final TrackEntity track, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTrackEntity.handle(track);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementPlayCount(final long trackId, final long timestamp,
      final long durationPlayed, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementPlayCount.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, durationPlayed);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, trackId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementPlayCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementTrackSkipCount(final long trackId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementTrackSkipCount.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, trackId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementTrackSkipCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TrackEntity>> getActiveTracks(final long recentThreshold) {
    final String _sql = "\n"
            + "        SELECT * FROM tracks \n"
            + "        WHERE id NOT IN (\n"
            + "            SELECT trackId FROM skip_history \n"
            + "            WHERE skippedAt > ? \n"
            + "            GROUP BY trackId \n"
            + "            HAVING COUNT(*) >= 3\n"
            + "        )\n"
            + "        ORDER BY dateAdded DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, recentThreshold);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks",
        "skip_history"}, new Callable<List<TrackEntity>>() {
      @Override
      @NonNull
      public List<TrackEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfArtworkPath = CursorUtil.getColumnIndexOrThrow(_cursor, "artworkPath");
          final int _cursorIndexOfPlayCount = CursorUtil.getColumnIndexOrThrow(_cursor, "playCount");
          final int _cursorIndexOfLastPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayed");
          final int _cursorIndexOfTotalPlayTime = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPlayTime");
          final int _cursorIndexOfSkipCount = CursorUtil.getColumnIndexOrThrow(_cursor, "skipCount");
          final List<TrackEntity> _result = new ArrayList<TrackEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrackEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final String _tmpPath;
            if (_cursor.isNull(_cursorIndexOfPath)) {
              _tmpPath = null;
            } else {
              _tmpPath = _cursor.getString(_cursorIndexOfPath);
            }
            final String _tmpArtworkPath;
            if (_cursor.isNull(_cursorIndexOfArtworkPath)) {
              _tmpArtworkPath = null;
            } else {
              _tmpArtworkPath = _cursor.getString(_cursorIndexOfArtworkPath);
            }
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final Long _tmpLastPlayed;
            if (_cursor.isNull(_cursorIndexOfLastPlayed)) {
              _tmpLastPlayed = null;
            } else {
              _tmpLastPlayed = _cursor.getLong(_cursorIndexOfLastPlayed);
            }
            final long _tmpTotalPlayTime;
            _tmpTotalPlayTime = _cursor.getLong(_cursorIndexOfTotalPlayTime);
            final int _tmpSkipCount;
            _tmpSkipCount = _cursor.getInt(_cursorIndexOfSkipCount);
            _item = new TrackEntity(_tmpId,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpDuration,_tmpDateAdded,_tmpPath,_tmpArtworkPath,_tmpPlayCount,_tmpLastPlayed,_tmpTotalPlayTime,_tmpSkipCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TrackEntity>> getAllTracks() {
    final String _sql = "SELECT * FROM tracks ORDER BY dateAdded DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<TrackEntity>>() {
      @Override
      @NonNull
      public List<TrackEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfArtworkPath = CursorUtil.getColumnIndexOrThrow(_cursor, "artworkPath");
          final int _cursorIndexOfPlayCount = CursorUtil.getColumnIndexOrThrow(_cursor, "playCount");
          final int _cursorIndexOfLastPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayed");
          final int _cursorIndexOfTotalPlayTime = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPlayTime");
          final int _cursorIndexOfSkipCount = CursorUtil.getColumnIndexOrThrow(_cursor, "skipCount");
          final List<TrackEntity> _result = new ArrayList<TrackEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrackEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final String _tmpPath;
            if (_cursor.isNull(_cursorIndexOfPath)) {
              _tmpPath = null;
            } else {
              _tmpPath = _cursor.getString(_cursorIndexOfPath);
            }
            final String _tmpArtworkPath;
            if (_cursor.isNull(_cursorIndexOfArtworkPath)) {
              _tmpArtworkPath = null;
            } else {
              _tmpArtworkPath = _cursor.getString(_cursorIndexOfArtworkPath);
            }
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final Long _tmpLastPlayed;
            if (_cursor.isNull(_cursorIndexOfLastPlayed)) {
              _tmpLastPlayed = null;
            } else {
              _tmpLastPlayed = _cursor.getLong(_cursorIndexOfLastPlayed);
            }
            final long _tmpTotalPlayTime;
            _tmpTotalPlayTime = _cursor.getLong(_cursorIndexOfTotalPlayTime);
            final int _tmpSkipCount;
            _tmpSkipCount = _cursor.getInt(_cursorIndexOfSkipCount);
            _item = new TrackEntity(_tmpId,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpDuration,_tmpDateAdded,_tmpPath,_tmpArtworkPath,_tmpPlayCount,_tmpLastPlayed,_tmpTotalPlayTime,_tmpSkipCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTrackById(final long trackId,
      final Continuation<? super TrackEntity> $completion) {
    final String _sql = "SELECT * FROM tracks WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, trackId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TrackEntity>() {
      @Override
      @Nullable
      public TrackEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfArtworkPath = CursorUtil.getColumnIndexOrThrow(_cursor, "artworkPath");
          final int _cursorIndexOfPlayCount = CursorUtil.getColumnIndexOrThrow(_cursor, "playCount");
          final int _cursorIndexOfLastPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayed");
          final int _cursorIndexOfTotalPlayTime = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPlayTime");
          final int _cursorIndexOfSkipCount = CursorUtil.getColumnIndexOrThrow(_cursor, "skipCount");
          final TrackEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final String _tmpPath;
            if (_cursor.isNull(_cursorIndexOfPath)) {
              _tmpPath = null;
            } else {
              _tmpPath = _cursor.getString(_cursorIndexOfPath);
            }
            final String _tmpArtworkPath;
            if (_cursor.isNull(_cursorIndexOfArtworkPath)) {
              _tmpArtworkPath = null;
            } else {
              _tmpArtworkPath = _cursor.getString(_cursorIndexOfArtworkPath);
            }
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final Long _tmpLastPlayed;
            if (_cursor.isNull(_cursorIndexOfLastPlayed)) {
              _tmpLastPlayed = null;
            } else {
              _tmpLastPlayed = _cursor.getLong(_cursorIndexOfLastPlayed);
            }
            final long _tmpTotalPlayTime;
            _tmpTotalPlayTime = _cursor.getLong(_cursorIndexOfTotalPlayTime);
            final int _tmpSkipCount;
            _tmpSkipCount = _cursor.getInt(_cursorIndexOfSkipCount);
            _result = new TrackEntity(_tmpId,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpDuration,_tmpDateAdded,_tmpPath,_tmpArtworkPath,_tmpPlayCount,_tmpLastPlayed,_tmpTotalPlayTime,_tmpSkipCount);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TrackEntity>> getMostPlayedTracks() {
    final String _sql = "SELECT * FROM tracks ORDER BY playCount DESC LIMIT 50";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<TrackEntity>>() {
      @Override
      @NonNull
      public List<TrackEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfArtworkPath = CursorUtil.getColumnIndexOrThrow(_cursor, "artworkPath");
          final int _cursorIndexOfPlayCount = CursorUtil.getColumnIndexOrThrow(_cursor, "playCount");
          final int _cursorIndexOfLastPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayed");
          final int _cursorIndexOfTotalPlayTime = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPlayTime");
          final int _cursorIndexOfSkipCount = CursorUtil.getColumnIndexOrThrow(_cursor, "skipCount");
          final List<TrackEntity> _result = new ArrayList<TrackEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrackEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final String _tmpPath;
            if (_cursor.isNull(_cursorIndexOfPath)) {
              _tmpPath = null;
            } else {
              _tmpPath = _cursor.getString(_cursorIndexOfPath);
            }
            final String _tmpArtworkPath;
            if (_cursor.isNull(_cursorIndexOfArtworkPath)) {
              _tmpArtworkPath = null;
            } else {
              _tmpArtworkPath = _cursor.getString(_cursorIndexOfArtworkPath);
            }
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final Long _tmpLastPlayed;
            if (_cursor.isNull(_cursorIndexOfLastPlayed)) {
              _tmpLastPlayed = null;
            } else {
              _tmpLastPlayed = _cursor.getLong(_cursorIndexOfLastPlayed);
            }
            final long _tmpTotalPlayTime;
            _tmpTotalPlayTime = _cursor.getLong(_cursorIndexOfTotalPlayTime);
            final int _tmpSkipCount;
            _tmpSkipCount = _cursor.getInt(_cursorIndexOfSkipCount);
            _item = new TrackEntity(_tmpId,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpDuration,_tmpDateAdded,_tmpPath,_tmpArtworkPath,_tmpPlayCount,_tmpLastPlayed,_tmpTotalPlayTime,_tmpSkipCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSimilarTracks(final long trackId, final int limit,
      final Continuation<? super List<TrackEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT t.* FROM tracks t\n"
            + "        JOIN track_tags tt ON t.id = tt.trackId\n"
            + "        WHERE tt.tagId IN (SELECT tagId FROM track_tags WHERE trackId = ?)\n"
            + "        AND t.id != ?\n"
            + "        GROUP BY t.id\n"
            + "        ORDER BY COUNT(tt.tagId) DESC\n"
            + "        LIMIT ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, trackId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, trackId);
    _argIndex = 3;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TrackEntity>>() {
      @Override
      @NonNull
      public List<TrackEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfArtworkPath = CursorUtil.getColumnIndexOrThrow(_cursor, "artworkPath");
          final int _cursorIndexOfPlayCount = CursorUtil.getColumnIndexOrThrow(_cursor, "playCount");
          final int _cursorIndexOfLastPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayed");
          final int _cursorIndexOfTotalPlayTime = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPlayTime");
          final int _cursorIndexOfSkipCount = CursorUtil.getColumnIndexOrThrow(_cursor, "skipCount");
          final List<TrackEntity> _result = new ArrayList<TrackEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrackEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final String _tmpPath;
            if (_cursor.isNull(_cursorIndexOfPath)) {
              _tmpPath = null;
            } else {
              _tmpPath = _cursor.getString(_cursorIndexOfPath);
            }
            final String _tmpArtworkPath;
            if (_cursor.isNull(_cursorIndexOfArtworkPath)) {
              _tmpArtworkPath = null;
            } else {
              _tmpArtworkPath = _cursor.getString(_cursorIndexOfArtworkPath);
            }
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final Long _tmpLastPlayed;
            if (_cursor.isNull(_cursorIndexOfLastPlayed)) {
              _tmpLastPlayed = null;
            } else {
              _tmpLastPlayed = _cursor.getLong(_cursorIndexOfLastPlayed);
            }
            final long _tmpTotalPlayTime;
            _tmpTotalPlayTime = _cursor.getLong(_cursorIndexOfTotalPlayTime);
            final int _tmpSkipCount;
            _tmpSkipCount = _cursor.getInt(_cursorIndexOfSkipCount);
            _item = new TrackEntity(_tmpId,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpDuration,_tmpDateAdded,_tmpPath,_tmpArtworkPath,_tmpPlayCount,_tmpLastPlayed,_tmpTotalPlayTime,_tmpSkipCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TrackEntity>> getHiddenTracks(final long recentThreshold) {
    final String _sql = "\n"
            + "        SELECT * FROM tracks \n"
            + "        WHERE id IN (\n"
            + "            SELECT trackId FROM skip_history \n"
            + "            WHERE skippedAt > ? \n"
            + "            GROUP BY trackId \n"
            + "            HAVING COUNT(*) >= 3\n"
            + "        )\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, recentThreshold);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks",
        "skip_history"}, new Callable<List<TrackEntity>>() {
      @Override
      @NonNull
      public List<TrackEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfArtworkPath = CursorUtil.getColumnIndexOrThrow(_cursor, "artworkPath");
          final int _cursorIndexOfPlayCount = CursorUtil.getColumnIndexOrThrow(_cursor, "playCount");
          final int _cursorIndexOfLastPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayed");
          final int _cursorIndexOfTotalPlayTime = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPlayTime");
          final int _cursorIndexOfSkipCount = CursorUtil.getColumnIndexOrThrow(_cursor, "skipCount");
          final List<TrackEntity> _result = new ArrayList<TrackEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrackEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final String _tmpPath;
            if (_cursor.isNull(_cursorIndexOfPath)) {
              _tmpPath = null;
            } else {
              _tmpPath = _cursor.getString(_cursorIndexOfPath);
            }
            final String _tmpArtworkPath;
            if (_cursor.isNull(_cursorIndexOfArtworkPath)) {
              _tmpArtworkPath = null;
            } else {
              _tmpArtworkPath = _cursor.getString(_cursorIndexOfArtworkPath);
            }
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final Long _tmpLastPlayed;
            if (_cursor.isNull(_cursorIndexOfLastPlayed)) {
              _tmpLastPlayed = null;
            } else {
              _tmpLastPlayed = _cursor.getLong(_cursorIndexOfLastPlayed);
            }
            final long _tmpTotalPlayTime;
            _tmpTotalPlayTime = _cursor.getLong(_cursorIndexOfTotalPlayTime);
            final int _tmpSkipCount;
            _tmpSkipCount = _cursor.getInt(_cursorIndexOfSkipCount);
            _item = new TrackEntity(_tmpId,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpDuration,_tmpDateAdded,_tmpPath,_tmpArtworkPath,_tmpPlayCount,_tmpLastPlayed,_tmpTotalPlayTime,_tmpSkipCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object deleteRemovedTracks(final List<Long> remainingIds,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM tracks WHERE id NOT IN (");
        final int _inputSize = remainingIds.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (Long _item : remainingIds) {
          if (_item == null) {
            _stmt.bindNull(_argIndex);
          } else {
            _stmt.bindLong(_argIndex, _item);
          }
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
