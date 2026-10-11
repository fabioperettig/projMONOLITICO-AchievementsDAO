package domain;

import com.fabioperettig.domain.Achievement;
import com.fabioperettig.domain.User;
import com.fabioperettig.service.AchievementService;
import com.fabioperettig.service.UserService;
import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
public class UserServicesIT {

    @Inject
    private UserService userService;

    @Inject
    private AchievementService achievementService;

    private final List<Long> listaUsers = new ArrayList<>();
    private final List<Long> listaAchievements = new ArrayList<>();

    @Deployment
    public static WebArchive checkDeployTest() {
        return ShrinkWrap.create(WebArchive.class, "conquistas-test.war")
                .addPackages(
                        true,
                        "com.fabioperettig.domain",
                        "com.fabioperettig.dao",
                        "com.fabioperettig.service"
                )
                .addAsResource("META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @AfterEach
    public void limparDados() {
        /// Apagar as conquistas antes de apagar os usuários (FK)
        for (Long id : listaAchievements) {
            Achievement conquista = achievementService.read(id);

            if (conquista != null) {
                achievementService.delete(conquista);
            }
        }

        for (Long id : listaUsers) {
            User usuario = userService.read(id);

            if (usuario != null) {
                userService.delete(usuario);
            }
        }

        listaAchievements.clear();
        listaUsers.clear();
    }

    @Test
    public void userCrudTest() {
        User user = createUser(novoUser());

        /// não é mais necessário usar Assertion.assert...
        assertNotNull(user.getId());

        User uReslt = userService.read(user.getId());

        assertNotNull(uReslt);
        assertEquals(user.getApelido(), uReslt.getApelido());
        assertEquals(user.getEmail(), uReslt.getEmail());
        assertEquals(Integer.valueOf(1), uReslt.getNivel());

        uReslt.setApelido("alterado-" + UUID.randomUUID());
        uReslt.setNivel(2);

        userService.update(uReslt);

        User userUpdate = userService.read(user.getId());

        assertEquals(uReslt.getApelido(), userUpdate.getApelido());
        assertEquals(Integer.valueOf(2), userUpdate.getNivel());
        assertTrue(userService.findAll().stream().anyMatch(u -> user.getId().equals(u.getId())));

        userService.delete(userUpdate);

        assertNull(userService.read(user.getId()));
    }

    @Test
    public void emailDuplicadoTest() {
        User user = createUser(novoUser());

        User user2 = novoUser();
        user2.setEmail(user.getEmail());

        assertRegraRejeitada(() -> createUser(user2), "Já existe um usuário com este e-mail.");

        /// teste na sessão UPDATE usuário
        User user3 = createUser(novoUser());
        user3.setEmail(user.getEmail());

        assertRegraRejeitada(() -> userService.update(user3), "Já existe um usuário com este e-mail.");
        assertEquals(user.getEmail(), userService.read(user.getId()).getEmail());
    }

    @Test
    public void apelidoDuplicadoTest() {
        User original = createUser(novoUser());

        User user2 = novoUser();
        user2.setApelido(original.getApelido());

        assertRegraRejeitada(() -> createUser(user2), "Já existe um usuário com este apelido.");

        User user3 = createUser(novoUser());
        user3.setApelido(original.getApelido());

        assertRegraRejeitada(() -> userService.update(user3), "Já existe um usuário com este apelido.");
    }

    @Test
    public void deleteCascadeTest() {
        User user = createUser(novoUser());

        Achievement achievement = createAchievement(novoAchievement(user));

        assertTrue(userService.possuiAchievements(user.getId()));

        userService.delete(user);

        assertNull(userService.read(user.getId()));
        assertNull(achievementService.read(achievement.getId()));
    }


    private User novoUser() {
        String identificador = UUID.randomUUID().toString();

        User user = new User();
        user.setApelido("teste-" + identificador);
        user.setEmail("teste-" + identificador + "@example.com");

        return user;
    }

    private User createUser(User user) {
        User cadastrado = userService.create(user);

        listaUsers.add(cadastrado.getId());

        return cadastrado;
    }

    private Achievement novoAchievement(User user) {
        Achievement achievement = new Achievement();
        achievement.setTitulo("Primeiro teste de integração");
        achievement.setDescricao("Executar os services dentro do WildFly.");
        achievement.setDataConquista(LocalDate.of(2026, 10, 11));
        achievement.setPrivado(false);
        achievement.setUser(user);

        return achievement;
    }

    private Achievement createAchievement(Achievement achievement) {
        Achievement cadastrada = achievementService.create(achievement);

        listaAchievements.add(cadastrada.getId());

        return cadastrada;
    }

    private void assertRegraRejeitada(Executable operacao, String mensagemEsperada) {
        EJBException erro = assertThrows(EJBException.class, operacao);

        /// O WildFly envolve a exceção do service em uma EJBException.
        Throwable causa = erro;

        while (causa != null
                && !(causa instanceof IllegalArgumentException)) {
            causa = causa.getCause();
        }

        assertNotNull(
                causa,
                "Esperávamos uma IllegalArgumentException na cadeia de causas."
        );

        assertEquals(mensagemEsperada, causa.getMessage());
    }
}