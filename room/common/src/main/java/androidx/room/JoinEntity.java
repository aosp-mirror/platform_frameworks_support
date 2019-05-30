/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an entity to be used for joining a relationship.
 * <p>
 * If a {@link Relation} should use an a associative table (also know as join table, junction table
 * or cross-reference table) then you can use this annotation to define such join entity. This is
 * useful for fetching many-to-many relations.
 * <pre>
 * {@literal @}Entity(
 *         primaryKeys = {"playlistId", "songId"},
 *         foreignKeys = {
 *                 {@literal @}ForeignKey(
 *                         entity = Playlist.class,
 *                         parentColumns = "playlistId",
 *                         childColumns = "playlistId",
 *                         onDelete = CASCADE),
 *                 {@literal @}ForeignKey(
 *                         entity = Song.class,
 *                         parentColumns = "songId",
 *                         childColumns = "songId",
 *                         onDelete = CASCADE),
 *         }
 * )
 * public class PlaylistSongXRef {
 *     int playlistId;
 *     int songId;
 * }
 * public class PlaylistWithSongs {
 *     {@literal @}Embedded
 *     Playlist playlist;
 *     {@literal @}Relation(
 *             parentColumn = "mPlaylistId",
 *             entity = Song.class,
 *             entityColumn = "mSongId",
 *             joinEntity = {@literal @}JoinEntity(PlaylistSongXRef.class))
 *     List&lt;String&gt; songs;
 * }
 * {@literal @}Dao
 * public interface MusicDao {
 *     {@literal @}Query("SELECT * FROM Playlist")
 *     List&lt;PlaylistWithSongs&gt; getAllPlaylistsWithSongs();
 * }
 * </pre>
 * <p>
 * In the above example the many-to-many relationship between {@code Song} and {@code Playlist} has
 * an associative table defined in the entity {@code PlaylistSongXRef}.
 *
 * @see Relation
 */
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface JoinEntity {
    /**
     * The entity or view to be used as a join table (also know as associative table,
     * cross-reference table or junction table) when fetching the relating entities.
     *
     * @return The entity to be used as a join table.
     */
    Class value();

    /**
     * The join entity field that will be used to match against {@link Relation#parentColumn()}.
     * <p>
     * If none is specified then the join entity must contain a matching column as defined in
     * {@link Relation#parentColumn()}
     */
    String parentColumn() default "";

    /**
     * The join entity field that will be used to match against {@link Relation#entityColumn()}.
     * <p>
     * If none is specified then the join entity must contain a matching column as defined in
     * {@link Relation#entityColumn()}
     */
    String entityColumn() default "";
}
