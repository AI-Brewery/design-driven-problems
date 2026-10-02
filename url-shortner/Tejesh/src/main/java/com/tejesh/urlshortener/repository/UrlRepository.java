package com.tejesh.urlshortener.repository;

import com.tejesh.urlshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<Url> findByExpiresAtBefore(Instant instant);

    @Modifying
    @Transactional
    @Query("update Url u set u.clicks = :clicks where u.shortCode = :shortCode")
    int updateClicksByShortCode(@Param("shortCode") String shortCode, @Param("clicks") long clicks);
}
