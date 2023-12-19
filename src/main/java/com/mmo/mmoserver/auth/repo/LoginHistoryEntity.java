package com.mmo.mmoserver.auth.repo;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "login_history")
@Getter
@Setter
public class LoginHistoryEntity {

    @Id
    private UUID id;
    private String login;
    private Date date;
}
