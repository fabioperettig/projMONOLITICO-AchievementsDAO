package com.fabioperettig.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "TB_USER")
public class User implements Persistence, Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "u.seq")
    @SequenceGenerator(name = "u.seq", sequenceName = "sequence_user", initialValue = 1, allocationSize = 1)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank(message = "Digite seu apelido.")
    @Column(name = "USUARIO", nullable = false, unique = true)
    private String apelido;

    @Column(name = "NIVEL", nullable = false)
    private Integer nivel = 1;

    @NotBlank(message = "Digite seu email.")
    @Email(message = "Informe um email válido.")
    @Column(name = "EMAIL", nullable = false, unique = true)
    private String email;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private List<Achievement> achievements = new ArrayList<>();
}
