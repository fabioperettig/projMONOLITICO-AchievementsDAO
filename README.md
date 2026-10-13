![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![WildFly](https://img.shields.io/badge/WildFly-%23f0473e.svg?style=for-the-badge&logo=redhat&logoColor=white)
![Curso EBAC](https://img.shields.io/badge/Curso--EBAC-f2f0ef?style=for-the-badge)

# 📚☕️ Projeto Monolítico para Web – AchievementsDAO 🏅

Primeiro projeto monolítico para Web com persistência PostgreSQL, implementação Wildfly e interação frontend em
JSF(Java Server Faces). O projeto consiste em cadastro de usuários e registros de Achievements que o usuário conquistou.
A estrutura é composta de duas `Entidades` simples "User" e "Achievements" com anotação JPA `@ManyToOne` (um usuário
com vários achievements).

| Classe        | Descrição                                                 |
|---------------|-----------------------------------------------------------|
| `User`        | Entidade central persistida @OneToMany                    |
| `Achievement` | Entidade persistida com dependência @ManyToOne de usuário |

<details><summary>Estrutura do projeto</summary>

````
main
└── java
    ├── com.fabioperettig
    |   ├── controller
    |   |   ├── UserController
    |   |   └── AchievementController
    |   ├── dao
    |   |   ├── generic
    |   |   |   ├── GenericDAO
    |   |   |   └── IGenericDAO
    |   |   ├── AchievementDAO
    |   |   └── UserDAO
    |   ├── domain
    |   |   ├── Achievement
    |   |   ├── Persistence
    |   |   └── User
    |   └── service
    |       ├── AchievementService
    |       └── UserService
    ├── resources
    |   └── META-INF
    |       └── persistence.xml
    └── webapp
        ├── WEB-INF
        |   └── web.xml
        ├── achievements.xhtml
        └── users.xhtml    
````
</details>

## 🐳 Docker

Tanto o `PostgreSQL` quanto o `Wildfly` foram contruidos em containers de [compose.yaml](compose.yaml), com dados
implementados em variáveis de ambiente com `.env`.

```dockerfile
services:
  postgres:
    image: postgres:15

    environment:
      POSTGRES_DB: ${POSTGRES_DB:?Defina POSTGRES_DB no .env}
      POSTGRES_USER: ${POSTGRES_USER:?Defina POSTGRES_USER no .env}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Defina POSTGRES_PASSWORD no .env}

    ports:
      - "127.0.0.1:${POSTGRES_PORT:-5434}:5432"

    volumes:
      - postgres_data:/var/lib/postgresql/data

    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U \"$${POSTGRES_USER}\" -d \"$${POSTGRES_DB}\""]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 10s

  wildfly:
    build:
      context: .
      dockerfile: Dockerfile

    image: conquistas-wildfly:dev

    environment:
      DB_URL: ${DB_URL:?Defina DB_URL no .env}
      DB_USER: ${POSTGRES_USER:?Defina POSTGRES_USER no .env}
      DB_PASSWORD: ${POSTGRES_PASSWORD:?Defina POSTGRES_PASSWORD no .env}


    ports:
      - "127.0.0.1:${WILDFLY_HTTP_PORT:-8080}:8080"

    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

## 👤 Entidades

As entidades foram implementadas em ***Java Persistence API***, implementando contrato `Persistence` para garantir o
método getId() e contrato `Serializable` para o uso de aplicação Web Wildfly.

<details><summary>Entidade Cliente</summary>

```java
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
```

</details>

## ⚙️ DAO com Windfly 

Agora com o uso do WildFly o gerenciamento da camada DAO sofrerá mudanças em comparação com JPA convencional. O Wildfly
assume o controle do **ciclo de vida do JPA** de forma automática, não sendo mais necessário o uso explícito de `begin()`,
`commit()` e nem de `close()` do EntityManager.

Essa automatização ocorre atráves da anotação `@PersistenceContext` que recebe a *unitName* e faz a injeção de 
ependência do EntityManager.

```java
import com.fabioperettig.domain.Persistence;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

public abstract class GenericDAO<T extends Persistence> implements IGenericDAO<T> {

    protected final Class<T> entityClass;

    @PersistenceContext(unitName = "ProjMONOAchievements")
    protected EntityManager entityManager;

    protected GenericDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }


    @Override
    public T create(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    @Override
    public T read(Long id) {
        return entityManager.find(entityClass, id);
    }

    @Override
    public T update(T entity) {
        return entityManager.merge(entity);
    }

    @Override
    public void delete(T entity) {
        if (entityManager.contains(entity)) {
            entityManager.remove(entity);
            return;
        }

        T managedEntity = entityManager.find(entityClass, entity.getId());

        if (managedEntity != null) {
            entityManager.remove(managedEntity);
        }
    }

    @Override
    public List<T> findAll() {
        String jpql = "SELECT entity FROM "
                + entityClass.getSimpleName()
                + " entity";

        return entityManager
                .createQuery(jpql, entityClass)
                .getResultList();
    }
}
```

## 🖥️ Interface Web com JSF

Com a JSF, os métodos CRUD das entidades ganham uma uma interatividade mais amigável estilo `frontend` com páginas
XHTML que acessam os controllers por expressões como `#{userController.create}`.

<details><summary>xhtml da sessão de Usuário</summary>

````xhtml
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="jakarta.faces.html"
      xmlns:f="jakarta.faces.core"
      xmlns:p="primefaces"
      lang="pt-BR">

<h:head>
    <title>Cadastro de usuários</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
</h:head>

<h:body>
    <h1>Usuários</h1>
    <p:button outcome="achievements" value="Conquistas" icon="pi pi-trophy"/>
    <p:messages id="messages" showDetail="false" closable="true"/>

    <!-- FORMULÁRIO -->
    <h:form id="userForm">
        <p:panel header="#{userController.editing ? 'Editar usuário' : 'Cadastrar usuário'}">
            <p:panelGrid columns="2" layout="tabular">

                <p:outputLabel for="apelido" value="Apelido:"/>
                <p:inputText id="apelido"
                             label="Apelido"
                             value="#{userController.user.apelido}"
                             required="true"
                             requiredMessage="Informe o apelido."/>

                <p:outputLabel for="email" value="E-mail:"/>
                <p:inputText id="email"
                             label="E-mail"
                             value="#{userController.user.email}"
                             required="true"
                             requiredMessage="Informe o e-mail."/>

            </p:panelGrid>
            <p:divider/>

            <p:commandButton value="Cadastrar" icon="pi pi-plus"
                             action="#{userController.create}"
                             rendered="#{not userController.editing}" ajax="false"/>

            <p:commandButton value="Salvar alteração" icon="pi pi-check"
                             action="#{userController.update}"
                             rendered="#{userController.editing}" ajax="false"/>

            <p:commandButton value="Cancelar" icon="pi pi-times" styleClass="ui-button-secondary"
                             action="#{userController.cancel}" process="@this" update="@form :messages" resetValues="true"/>
        </p:panel>
    </h:form>

    <h2>Usuários cadastrados</h2>

    <!-- PAINEL PRINCIPAL -->
    <h:form id="usersTableForm">
        <p:dataTable value="#{userController.users}"
                     var="item"
                     paginator="true" rows="10"
                     paginatorPosition="bottom" paginatorAlwaysVisible="false"
                     reflow="true"
                     emptyMessage="Nenhum usuário cadastrado.">

            <p:column headerText="ID">
                <h:outputText value="#{item.id}"/>
            </p:column>

            <p:column headerText="Apelido">
                <h:outputText value="#{item.apelido}"/>
            </p:column>

            <p:column headerText="E-mail">
                <h:outputText value="#{item.email}"/>
            </p:column>

            <p:column headerText="Nível">
                <p:tag value="Nível #{item.nivel}" severity="success"/>
            </p:column>

            <p:column headerText="Ações">

                <p:commandButton value="Editar" icon="pi pi-pencil" styleClass="ui-button-secondary"
                                 action="#{userController.edit(item)}" process="@this" update=":userForm :messages" resetValues="true"/>

                <p:commandButton value="Excluir" icon="pi pi-trash" styleClass="ui-button-danger"
                                 action="#{userController.prepareDelete(item)}" ajax="false"/>
            </p:column>

        </p:dataTable>
    </h:form>

    <!-- CONQUISTAS ENCONTRADAS NO DELETE -->
    <h:form id="deleteConfirmationForm">
        <p:panel header="Confirmar exclusão"
                 rendered="#{not empty userController.userToDelete}">

            <p>
                Excluir
                <h:outputText value="#{userController.userToDelete.apelido}"/>
                também apagará todas as suas conquistas.
                Deseja continuar?
            </p>

            <p:commandButton value="Confirmar exclusão" icon="pi pi-trash" styleClass="ui-button-danger"
                             action="#{userController.confirmDelete}" ajax="false"/>

            <p:commandButton value="Cancelar" icon="pi pi-times" styleClass="ui-button-secondary"
                             action="#{userController.cancelDelete}" ajax="false"/>

        </p:panel>
    </h:form>
</h:body>

</html>
````
</details>

>Usuários sem conquistas são excluídos diretamente. Já quando há achievements vinculados ao usuário, é solicitado
> uma confirmação antes de excluir, pois excluir o usuário também acarreta na exclusão de todos os achievements
> atrelados.

### 🎨 PrimeFaces e tema

Para deixar as telas de usuários e conquistas mais organizadas, usei o `PrimeFaces 16.0.0` junto do JSF,
com componentes prontos para formulários, botões, calendário e tabelas com paginação.

O tema escolhido foi o `aura-dark-emerald`, com visual escuro e detalhes em verde. As dependências
`primefaces` e `primefaces-themes` ficam no [pom.xml](pom.xml), e o tema é selecionado pelo parâmetro
`primefaces.THEME` no [web.xml](src/main/webapp/WEB-INF/web.xml). Não foi necessário criar CSS próprio:
a aparência vem do tema e dos componentes do PrimeFaces.

## 🚀 Build e Deploy 

Enquanto o `Maven` compila e cria o package em um `.war`, o `Docker-Compose` prepara os containers de PostgreSQL e
WildFly, facilitando a reprodução do ambiente sem instalar esses serviços na máquina local.

| Ferramenta | Responsabilidade |
|---|---|
| Maven | Compilar o código e gerar o pacote da aplicação |
| Docker Compose | Configurar e administrar os serviços do projeto, como PostgreSQL e WildFly |

### Comandos para build e deploy do projeto 👇

```bash
mvn clean package
```
Executa o Maven, remove build anteriores e contrói a nova compilação `target/conquistas.war`, sendo 'conquistas' o nome
usado neste projeto em específico;

```bash
docker compose cp target/conquistas.war wildfly:/tmp/conquistas.war
```

Administra os serviços definidos no [compose.yaml](compose.yaml), criando uma cópia do arquivo para a etapa seguinte;

```bash
docker compose exec wildfly /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command="deploy /tmp/conquistas.war --force"
```
Inicia a instalação via `WildFly` e faz o `Deploy` da aplicação.

## 👽✅ Testes e Arquillian

Além do `JUnit`, os testes utiliza o `Arquillian` para criar uma ponte entre os testes e o WildFly, cobrindo o CRUD
de usuários e de conquistas, testa o comportamento de emails e apelidos duplicados e exclusão em cascata, tudo em
um banco PostgreSQL próprio para testes.

<details><summary>Classe testes User</summary>

```java
package domain;

imports {...}

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
```
</details>

-----

## 📖 Projeto Guia

Como o professor deixou livre para criar o próprio sistema (desde que seguisse os ensinamentos do módulo), construí este
projeto como rascunho de uma ideia que pretendo implementar oficialmente no futuro. Mas, também, segui o projeto
disponibilizado no módulo 38. Usando como base durante toda a implementação, (a mesma estrutura e distribuição de
responsabilidades), mas também procurei tomando decisões diferentes e tentando dar sempre mais um passo que agregasse
nos aprendizado.

> Repositório oficial do projeto usado de base:
> https://github.com/digaomilleniun/backend-java-ebac/tree/main/mod38/VendasOnline

------

**Fabio Peretti Guimarães | tarefa Ebac mod 38 | OUT 2026**
