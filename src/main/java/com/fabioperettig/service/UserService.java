package com.fabioperettig.service;

import com.fabioperettig.dao.UserDAO;
import com.fabioperettig.domain.User;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;

@Stateless
public class UserService {

    @Inject
    private UserDAO userDAO;

    public User create(User user) {
        checkDuplicidade(user);
        return userDAO.create(user);
    }

    public User read(Long id) {
        return userDAO.read(id);
    }

    public User update(User user) {
        checkDuplicidade(user);
        return userDAO.update(user);
    }

    public void delete(User user) {
        userDAO.delete(user);
    }

    public List<User> findAll() {
        return userDAO.findAll();
    }

    public boolean possuiAchievements(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Informe o ID do usuário.");
        }

        return userDAO.possuiAchievements(userId);
    }

    private void checkDuplicidade(User user) {
        if (userDAO.checkEmailDuplicado(user.getEmail(), user.getId())) {
            throw new IllegalArgumentException(
                    "Já existe um usuário com este e-mail."
            );
        }

        if (userDAO.checkApelidoDuplicado(user.getApelido(), user.getId())) {
            throw new IllegalArgumentException(
                    "Já existe um usuário com este apelido."
            );
        }
    }
}
