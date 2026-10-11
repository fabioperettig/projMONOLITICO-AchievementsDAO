package com.fabioperettig.dao;

import com.fabioperettig.dao.generic.GenericDAO;
import com.fabioperettig.domain.User;
import jakarta.enterprise.context.Dependent;
import jakarta.persistence.TypedQuery;

@Dependent
public class UserDAO extends GenericDAO<User> {

    protected UserDAO() {
        super(User.class);
    }

    /// Metodos JPQL
    public boolean possuiAchievements(Long userId) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(a) FROM Achievement a WHERE a.user.id = :userId",
                        Long.class
                )
                .setParameter("userId", userId)
                .getSingleResult();

        return count > 0;
    }

    public boolean checkEmailDuplicado(String email, Long idUsuario) {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.email = :email";

        if (idUsuario != null) {
            jpql += " AND u.id <> :idUsuario";
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("email", email);

        if (idUsuario != null) {
            query.setParameter("idUsuario", idUsuario);
        }

        return query.getSingleResult() > 0;
    }

    public boolean checkApelidoDuplicado(String apelido, Long idUsuario) {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.apelido = :apelido";

        if (idUsuario != null) {
            jpql += " AND u.id <> :idUsuario";
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("apelido", apelido);

        if (idUsuario != null) {
            query.setParameter("idUsuario", idUsuario);
        }

        return query.getSingleResult() > 0;
    }
}
