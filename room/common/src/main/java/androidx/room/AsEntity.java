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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows declaring a Pojo parameter in an {@link Insert}, {@link Update} or {@link Delete} DAO
 * method to be used as a partial entity.
 * <pre>
 * {@literal @}Entity
 * public class Product {
 *     {@literal @}PrimaryKey(autoGenerate = true);
 *     long id;
 *     String name;
 *     String description;
 *     {@literal @}ColumnInfo(defaultValue = "0")
 *     int price;
 * }
 *
 * public class ProductDescription {
 *     {@literal @}ColumnInfo(name = "name")
 *     String title;
 *     {@literal @}ColumnInfo(name = "description")
 *     String text;
 * }
 *
 * {@literal @}Dao
 * public interface ProductDao {
 *     {@literal @}Insert
 *     long insertNewProduct(@AsEntity(Product.class) ProductDescription description);
 * }
 * </pre>
 * @see Insert
 * @see Update
 * @see Delete
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface AsEntity {
    /**
     * The entity that the annotated parameter represents.
     */
    Class value();
}
