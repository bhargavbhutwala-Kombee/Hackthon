package com.kombee.orderly.repository;

import com.kombee.orderly.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query(value = "SELECT * FROM products p WHERE " +
           "(CAST(:name AS varchar) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS varchar), '%'))) AND " +
           "(CAST(:sku AS varchar) IS NULL OR LOWER(p.sku) LIKE LOWER(CONCAT('%', CAST(:sku AS varchar), '%'))) " +
           "ORDER BY p.name ASC",
           countQuery = "SELECT COUNT(*) FROM products p WHERE " +
           "(CAST(:name AS varchar) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS varchar), '%'))) AND " +
           "(CAST(:sku AS varchar) IS NULL OR LOWER(p.sku) LIKE LOWER(CONCAT('%', CAST(:sku AS varchar), '%')))",
           nativeQuery = true)
    Page<Product> findAllFiltered(@Param("name") String name, @Param("sku") String sku, Pageable pageable);
}
