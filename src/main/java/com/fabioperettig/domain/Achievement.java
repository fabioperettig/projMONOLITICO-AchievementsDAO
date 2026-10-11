package com.fabioperettig.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "TB_ACHIEVEMENTS")
public class Achievement implements Persistence, Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ach.seq")
    @SequenceGenerator(name = "ach.seq", sequenceName = "sequence_achievements", initialValue = 1, allocationSize = 1)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank(message = "Digite o título da conquista.")
    @Column(name = "TITULO", nullable = false)
    private String titulo;

    @NotBlank(message = "Digite a descrição da conquista.")
    @Column(name = "DESCRICAO", nullable = false)
    private String descricao;

    @Column(name = "DATA", nullable = false)
    private LocalDate dataConquista;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "usuario_id",
            foreignKey = @ForeignKey(name = "fk_usuario_achievement"),
            nullable = false
    )
    private User user;

    @Column(name = "PRIVADO", nullable = false)
    private Boolean privado;
}
