package com.fuxy.cookeasy.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.fuxy.cookeasy.entity.Recipe

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipe")
    fun getAll(): List<Recipe>

    @Query("SELECT * FROM recipe LIMIT :pageSize OFFSET :offset")
    fun getPage(offset: Int, pageSize: Int): List<Recipe>

    @Query("SELECT * FROM recipe WHERE lower(dish) LIKE '%' || lower(:dish) || '%' ORDER BY :orderBy ASC LIMIT :pageSize OFFSET :offset")
    fun getPageByDish(offset: Int, pageSize: Int, dish: String, orderBy: String = "dish"): List<Recipe>

    @RawQuery
    fun rawQuery(query: SupportSQLiteQuery): List<Recipe>

    @Query("SELECT * FROM recipe WHERE id = :id")
    fun getById(id: Int): Recipe

    @Insert
    fun insert(vararg recipe: Recipe): List<Long>
}