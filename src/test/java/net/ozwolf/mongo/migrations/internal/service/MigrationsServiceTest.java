package net.ozwolf.mongo.migrations.internal.service;

import net.ozwolf.mongo.migrations.MigrationCommand;
import net.ozwolf.mongo.migrations.MongoMigration;
import net.ozwolf.mongo.migrations.exception.DuplicateVersionException;
import net.ozwolf.mongo.migrations.exception.MissingAnnotationException;
import net.ozwolf.mongo.migrations.internal.dao.SchemaVersionDAO;
import net.ozwolf.mongo.migrations.internal.domain.Migration;
import net.ozwolf.mongo.migrations.internal.domain.MigrationStatus;
import org.joda.time.DateTime;
import org.jongo.Jongo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MigrationsServiceTest {
    private final SchemaVersionDAO schemaVersionDAO = mock(SchemaVersionDAO.class);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldReturnPendingMigrationCommandsOnly() throws Throwable {
        Migration previous1 = record("1.0.0", MigrationStatus.Successful);
        Migration previous2 = record("1.0.1", MigrationStatus.Successful);
        Migration previous3 = record("1.0.2", MigrationStatus.Failed);

        List<Migration> previouslyRun = migrations(previous1, previous3, previous2);

        when(schemaVersionDAO.findAll()).thenReturn(previouslyRun);

        List<MigrationCommand> commands = commands(
                new CommandOne(),
                new CommandTwo(),
                new CommandFour(),
                new CommandThree()
        );

        List<Migration> pendingMigrations = new MigrationsService(schemaVersionDAO).getPendingMigrations(commands);

        assertThat(pendingMigrations.size(), is(2));
        assertThat(pendingMigrations.get(0).getVersion(), is("1.0.2"));
        assertThat(pendingMigrations.get(1).getVersion(), is("2.0.0"));
    }

    @Test
    public void shouldReturnAllMigrations() throws Throwable {
        Migration previous1 = record("1.0.0", MigrationStatus.Successful);
        Migration previous2 = record("1.0.1", MigrationStatus.Successful);
        Migration previous3 = record("1.0.2", MigrationStatus.Failed);

        List<Migration> previouslyRun = migrations(previous1, previous3, previous2);

        when(schemaVersionDAO.findAll()).thenReturn(previouslyRun);

        List<MigrationCommand> commands = commands(
                new CommandOne(),
                new CommandTwo(),
                new CommandFour(),
                new CommandThree()
        );

        List<Migration> migrations = new MigrationsService(schemaVersionDAO).getFullState(commands);

        assertThat(migrations.size(), is(4));
        assertThat(migrations.get(0).getVersion(), is("1.0.0"));
        assertThat(migrations.get(1).getVersion(), is("1.0.1"));
        assertThat(migrations.get(2).getVersion(), is("1.0.2"));
        assertThat(migrations.get(3).getVersion(), is("2.0.0"));
    }

    @Test
    public void shouldThrowDuplicateVersionException() throws Throwable {
        when(schemaVersionDAO.findAll()).thenReturn(migrations());

        List<MigrationCommand> commands = commands(
                new CommandFour(),
                new CommandThree(),
                new DuplicateCommandFour()
        );

        exception.expect(DuplicateVersionException.class);
        exception.expectMessage("Migration [ 2.0.0 ] has duplicate commands.");
        new MigrationsService(schemaVersionDAO).getPendingMigrations(commands);
    }

    @Test
    public void shouldThrowMissingAnnotationException() throws Throwable {
        when(schemaVersionDAO.findAll()).thenReturn(migrations());

        List<MigrationCommand> commands = commands(
                new CommandFour(),
                new CommandThree(),
                new MissingAnnotationCommand()
        );

        exception.expect(MissingAnnotationException.class);
        exception.expectMessage("Migration command [ MissingAnnotationCommand ] is missing the [ @MongoMigration ] annotation.");
        new MigrationsService(schemaVersionDAO).getPendingMigrations(commands);
    }

    private Migration record(String version, MigrationStatus status) {
        return new Migration(
                version,
                String.format("Migration %s", version),
                DateTime.now(),
                (status == MigrationStatus.Successful) ? DateTime.now() : null,
                status,
                (status == MigrationStatus.Failed) ? "Failure" : null
        );
    }

    private static List<Migration> migrations(Migration... migrations) {
        return Arrays.asList(migrations);
    }

    private static List<MigrationCommand> commands(MigrationCommand... commands) {
        return Arrays.asList(commands);
    }

    @MongoMigration(version = "1.0.0", description = "First migration")
    public static class CommandOne implements MigrationCommand {
        @Override
        public void migrate(Jongo jongo) {
        }
    }

    @MongoMigration(version = "1.0.1", description = "Second migration")
    public static class CommandTwo implements MigrationCommand {
        @Override
        public void migrate(Jongo jongo) {
        }
    }

    @MongoMigration(version = "1.0.2", description = "Third migration")
    public static class CommandThree implements MigrationCommand {
        @Override
        public void migrate(Jongo jongo) {
        }
    }

    @MongoMigration(version = "2.0.0", description = "Fourth migration")
    public static class CommandFour implements MigrationCommand {
        @Override
        public void migrate(Jongo jongo) {
        }
    }

    @MongoMigration(version = "2.0.0", description = "Duplicate migration")
    public static class DuplicateCommandFour implements MigrationCommand {
        @Override
        public void migrate(Jongo jongo) {
        }
    }

    public static class MissingAnnotationCommand implements MigrationCommand {
        @Override
        public void migrate(Jongo jongo) {
        }
    }
}