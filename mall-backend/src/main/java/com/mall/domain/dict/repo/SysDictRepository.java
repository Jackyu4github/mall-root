package com.mall.domain.dict.repo;

import com.mall.domain.dict.entity.DictStatus;
import com.mall.domain.dict.entity.SysDict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SysDictRepository extends JpaRepository<SysDict, Long> {

    List<SysDict> findAllByDictTypeAndStatusOrderBySortAsc(
            String dictType, DictStatus status);

    Optional<SysDict> findByDictTypeAndDictKey(String dictType, String dictKey);

    @Query("select max(d.updatedAt) from SysDict d")
    Instant findGlobalMaxUpdatedAt();


    Page<SysDict> findAllByOrderByDictTypeAscSortAscIdAsc(Pageable pageable);
}
