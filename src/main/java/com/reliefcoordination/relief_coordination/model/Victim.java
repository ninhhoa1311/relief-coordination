package com.reliefcoordination.relief_coordination.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "victims")
@Getter
@Setter
public class Victim extends User {
}