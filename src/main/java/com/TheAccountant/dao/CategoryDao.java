package com.TheAccountant.dao;

import com.TheAccountant.model.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;

/**
 * Created by florinIacob on 18.12.2015.
 * Data access object class for 'category' 
 */
@Transactional
public interface CategoryDao extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.name = ?1 AND c.user.username = ?2")
    Category findByNameAndUsername(String categoryName, String username);

    @Query("SELECT c FROM Category c WHERE c.user.username = ?1")
    List<Category> findByUsername(String username);

    @Modifying
    @Query(value = "delete from category WHERE userId IN (SELECT app_user.userId FROM app_user WHERE app_user.username= ?1)", nativeQuery = true)
    void deleteAllByUsername(String username);

}
