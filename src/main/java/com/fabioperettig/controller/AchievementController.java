package com.fabioperettig.controller;

import com.fabioperettig.domain.Achievement;
import com.fabioperettig.domain.User;
import com.fabioperettig.service.AchievementService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("achievementController")
@ViewScoped
public class AchievementController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(AchievementController.class.getName());

    @Inject
    private AchievementService achievementService;

    @Inject
    private UserService userService;

    @Getter
    @Setter
    private Achievement achievement;

    @Getter
    private List<Achievement> achievements;

    @Getter
    private List<User> users;

    @PostConstruct
    public void init() {
        iniciarAchievements();
        users = userService.findAll();
    }

    ///Metodos CRUD

    public void create() {
        try {
            achievementService.create(achievement);
        } catch (EJBException e) {
            erroSave(e, "Não foi possível cadastrar a conquista.");
            return;
        }

        iniciarAchievements();

        showMessage(FacesMessage.SEVERITY_INFO, "Conquista cadastrada.");
    }

    public void edit(Achievement achievementRead) {
        Achievement achievementResult =
                achievementService.read(achievementRead.getId());

        if (achievementResult == null) {
            showMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Conquista não encontrada."
            );
            return;
        }

        achievement = achievementResult;
    }

    public void update() {
        if (!isEditing()) {
            showMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Selecione uma conquista para editar."
            );
            return;
        }

        try {
            achievementService.update(achievement);
        } catch (EJBException e) {
            erroSave(e, "Não foi possível atualizar a conquista.");
            return;
        }

        iniciarAchievements();

        showMessage(FacesMessage.SEVERITY_INFO, "Conquista atualizada.");
    }

    public void cancel() {
        achievement = novoAchievement();
    }

    public void delete(Achievement achievementDelete) {
        try {
            achievementService.delete(achievementDelete);
        } catch (EJBException e) {
            erroSave(e, "Não foi possível excluir a conquista.");
            return;
        }
        iniciarAchievements();

        showMessage(FacesMessage.SEVERITY_INFO, "Conquista excluída.");
    }

    ///Auxiliares

    private void showMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(severity, message, null)
        );
    }

    public boolean isEditing(){
        return achievement != null && achievement.getId() != null;
    }

    ///Instancia Achievement e recarrega a lista
    private void iniciarAchievements() {
        achievement = novoAchievement();
        achievements = achievementService.findAll();
    }

    private Achievement novoAchievement() {
        Achievement achievement = new Achievement();
        achievement.setDataConquista(LocalDate.now());
        achievement.setPrivado(false);

        return achievement;
    }

    private void erroSave(EJBException exception, String mensagem) {
        Throwable exceptionCause = exception.getCause();

        if (exceptionCause instanceof IllegalArgumentException) {
            showMessage(FacesMessage.SEVERITY_WARN, exceptionCause.getMessage());
            return;
        }

        LOGGER.log(Level.SEVERE, mensagem, exception);
        showMessage(FacesMessage.SEVERITY_ERROR,mensagem);
    }
}
