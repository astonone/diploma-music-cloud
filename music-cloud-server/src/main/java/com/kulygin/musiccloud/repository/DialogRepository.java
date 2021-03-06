package com.kulygin.musiccloud.repository;

import com.kulygin.musiccloud.domain.Dialog;
import com.kulygin.musiccloud.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface DialogRepository extends JpaRepository<Dialog,Long> {
    List<Dialog> findAllByUsersInOrderByTimeAsc(Set<User> users);
    List<Dialog> findByUsersInOrderByTimeAsc(Set<User> users);
}
