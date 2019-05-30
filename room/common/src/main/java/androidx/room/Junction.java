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
 * Declares a junction to be used for joining a relationship.
 * <p>
 * If a {@link Relation} should use an a associative table (also know as junction table or join
 * table) then you can use this annotation to define such table. This is useful for fetching
 * many-to-many relations.
 * <pre>
 * {@literal @}Entity(
 *         primaryKeys = {"pId", "sId"},
 *         // foreignKeys = { ... } // omitted for simplicity
 * )
 * public class PlaylistSongXRef {
 *     int pId;
 *     int sId;
 * }
 * public class PlaylistWithSongs {
 *     {@literal @}Embedded
 *     Playlist playlist;
 *     {@literal @}Relation(
 *             parentColumn = "mPlaylistId",
 *             entity = Song.class,
 *             entityColumn = "mSongId",
 *             associateBy = {@literal @}Junction(
 *                     value = PlaylistSongXRef.class,
 *                     parentColumn = "pId",
 *                     entityColumn = "sId)
 *     )
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
 * an associative table defined by the entity {@code PlaylistSongXRef}.
 *
 * @see Relation
 */
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface Junction {
    /**
     * An entity or view to be used as a junction table when fetching the relating entities.
     *
     * @return The entity to be used as a junction table.
     */
    Class value();

    /**
     * The junction column that will be used to match against the {@link Relation#parentColumn()}.
     * <p>
     * If none is specified then the junction table must contain a column with the same name as
     * defined in {@link Relation#parentColumn()}.
     */
    String parentColumn() default "";

    /**
     * The junction column that will be used to match against the {@link Relation#entityColumn()}.
     * <p>
     * If none is specified then the junction table must contain a column with the same name as
     * defined in {@link Relation#entityColumn()}.
     */
    String entityColumn() default "";
}
