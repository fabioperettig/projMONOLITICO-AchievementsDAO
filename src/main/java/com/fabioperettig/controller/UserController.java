package com.fabioperettig.controller;

import com.fabioperettig.domain.User;
import com.fabioperettig.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("userController")
@ViewScoped
public class UserController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(UserController.class.getName());

    @Inject
    private UserService userService;

    @Getter
    @Setter
    private User user;

    @Getter
    private User userToDelete;

    @Getter
    private List<User> users;

    @PostConstruct
    public void init() {
        iniciarUsers();
    }

    ///Metodos CRUD

    public void create() {

        try {
        userService.create(user);
        } catch (EJBException e) {
            erroSave(e);
            return;
        }

        iniciarUsers();

        showMessage(FacesMessage.SEVERITY_INFO, "Usuário cadastrado.");
    }

    public void edit(User userRead) {
        User userResult = userService.read(userRead.getId());

        if (userResult == null) {
            showMessage(FacesMessage.SEVERITY_WARN, "Usuário não encontrado.");
            return;
        }

        user = userResult;
    }

    public void update() {
        if (!isEditing()) {
            showMessage(FacesMessage.SEVERITY_WARN, "Selecione um usuário para editar.");
            return;
        }

        try {
            userService.update(user);
        } catch (EJBException e) {
            erroSave(e);
            return;
        }
        iniciarUsers();

        showMessage(FacesMessage.SEVERITY_INFO, "Usuário atualizado.");
    }

    public void cancel() {
        user = new User();
    }

    private void deleteUser(User user) {
        try {
            userService.delete(user);
        } catch (EJBException e) {
            LOGGER.log(Level.SEVERE, "Erro ao excluir usuário.", e);

            showMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Não foi possível excluir o usuário."
            );
            return;
        }

        userToDelete = null;
        iniciarUsers();

        showMessage(FacesMessage.SEVERITY_INFO, "Usuário excluído.");
    }

    ///Auxiliares

    private void showMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(
                null, new FacesMessage(severity, message, null)
        );
    }

    public boolean isEditing() {
        return user != null && user.getId() != null;
    }

    ///Instancia User e recarrega a lista
    private void iniciarUsers() {
        user = new User();
        users = userService.findAll();
    }

    public void prepareDelete(User userDelete) {
        userToDelete = null;

        User userResult = userService.read(userDelete.getId());

        if (userResult == null) {
            showMessage(FacesMessage.SEVERITY_WARN, "Usuário não encontrado.");
            return;
        }

        if (userService.possuiAchievements(userResult.getId())) {
            userToDelete = userResult;

            showMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Este usuário possui conquistas. Elas também serão excluídas."
            );
            return;
        }

        deleteUser(userResult);
    }

    public void confirmDelete() {
        if (userToDelete == null) {
            return;
        }
        deleteUser(userToDelete);
    }

    public void cancelDelete() {
        userToDelete = null;
    }

    private void erroSave(EJBException exception) {
        Throwable exceptionCause = exception.getCause();

        if (exceptionCause instanceof IllegalArgumentException) {
            showMessage(FacesMessage.SEVERITY_WARN, exceptionCause.getMessage());
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro ao salvar usuário.", exception);
        showMessage(FacesMessage.SEVERITY_ERROR,"Não foi possível salvar o usuário.");
    }
}
