package com.t2308e.repository;

import java.util.List;
import java.util.Optional;

public interface MyCrudRepository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
    long count();
    // Optional: boolean existsById(ID id);
    // Optional: void deleteAll();
}