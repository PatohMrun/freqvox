package com.freqvox.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&\u00a8\u0006\t"}, d2 = {"Lcom/freqvox/data/local/FreqvoxDatabase;", "Landroidx/room/RoomDatabase;", "()V", "playlistDao", "Lcom/freqvox/data/local/dao/PlaylistDao;", "tagDao", "Lcom/freqvox/data/local/dao/TagDao;", "trackDao", "Lcom/freqvox/data/local/dao/TrackDao;", "app_debug"})
@androidx.room.Database(entities = {com.freqvox.data.local.entities.TrackEntity.class, com.freqvox.data.local.entities.TagEntity.class, com.freqvox.data.local.entities.TrackTagCrossRef.class, com.freqvox.data.local.entities.PlayHistoryEntity.class, com.freqvox.data.local.entities.SkipHistoryEntity.class, com.freqvox.data.local.entities.PlaylistEntity.class}, version = 1, exportSchema = false)
public abstract class FreqvoxDatabase extends androidx.room.RoomDatabase {
    
    public FreqvoxDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.freqvox.data.local.dao.TrackDao trackDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.freqvox.data.local.dao.TagDao tagDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.freqvox.data.local.dao.PlaylistDao playlistDao();
}