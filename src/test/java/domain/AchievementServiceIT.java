package domain;

import com.fabioperettig.domain.Achievement;
import com.fabioperettig.domain.User;
import com.fabioperettig.service.AchievementService;
import com.fabioperettig.service.UserService;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
public class AchievementServiceIT {

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
    public void achievementCrudTest() {
        User user = createUser(novoUser());

        assertFalse(userService.possuiAchievements(user.getId()));

        Achievement achievement = createAchievement(novoAchievement(user));

        assertNotNull(achievement.getId());

        Achievement encontrada = achievementService.read(achievement.getId());

        assertNotNull(encontrada);
        assertEquals(achievement.getTitulo(), encontrada.getTitulo());
        assertEquals(achievement.getDescricao(), encontrada.getDescricao());
        assertEquals(achievement.getDataConquista(), encontrada.getDataConquista());
        assertEquals(user.getId(), encontrada.getUser().getId());
        assertEquals(Boolean.FALSE, encontrada.getPrivado());

        assertTrue(userService.possuiAchievements(user.getId()));

        encontrada.setTitulo("Conquista atualizada");
        encontrada.setDescricao("Descrição atualizada");
        encontrada.setPrivado(true);

        achievementService.update(encontrada);

        Achievement atualizada = achievementService.read(achievement.getId());

        assertEquals("Conquista atualizada", atualizada.getTitulo());
        assertEquals("Descrição atualizada", atualizada.getDescricao());
        assertEquals(Boolean.TRUE, atualizada.getPrivado());

        assertTrue(achievementService.findAll().stream().anyMatch(
                a -> achievement.getId().equals(a.getId()))
        );

        achievementService.delete(atualizada);

        assertNull(achievementService.read(achievement.getId()));
        assertFalse(userService.possuiAchievements(user.getId()));
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
}
