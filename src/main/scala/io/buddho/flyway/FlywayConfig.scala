package io.buddho.flyway

import java.util

import com.typesafe.config.{ConfigObject, Config}
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.internal.util.{Locations => UtilLocations}
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._


case class DatabaseConfig(
                           driver: String,
                           url: String,
                           user: Option[String],
                           password: Option[String])

case class MigrationConfig(
                            name: String,
                            database: DatabaseConfig,
                            enabled: Boolean,

                            locations: Seq[String],
                            placeholderPrefix: String,
                            placeholderSuffix: String,
                            sqlMigrationPrefix: String,
                            sqlMigrationSeparator: String,
                            sqlMigrationSuffix: String,
                            encoding: String,
                            schemas: Seq[String],
                            table: String,
                            cleanOnValidationError: Boolean,
                            validateOnMigrate: Boolean,
                            baselineVersion: MigrationVersion,
                            baselineDescription: String,
                            baselineOnMigrate: Boolean,
                            ignoreFailedFutureMigration: Boolean,
                            target: MigrationVersion,
                            outOfOrder: Boolean,
                            resolvers: Seq[String],
                            callbacks: Seq[String],
                            placeholders: Map[String, String])


case class FlywayConfig(migrations: Seq[MigrationConfig])


object Keys {
  object flyway {
    val Enabled = "flyway.enabled"
    val Locations = "flyway.locations"
    val PlaceholderPrefix = "flyway.placeholderPrefix"
    val PlaceholderSuffix = "flyway.placeholderSuffix"
    val SqlMigrationPrefix = "flyway.sqlMigrationPrefix"
    val SqlMigrationSeparator = "flyway.sqlMigrationSeparator"
    val SqlMigrationSuffix = "flyway.sqlMigrationSuffix"
    val Encoding = "flyway.encoding"
    val Schemas = "flyway.schemas"
    val Table = "flyway.table"
    val CleanOnValidationError = "flyway.cleanOnValidationError"
    val ValidateOnMigrate = "flyway.validateOnMigrate"
    val BaselineVersion = "flyway.baselineVersion"
    val BaselineDescription = "flyway.baselineDescription"
    val BaselineOnMigrate = "flyway.baselineOnMigrate"
    val IgnoreFailedFutureMigration = "flyway.ignoreFailedFutureMigration"
    val Target = "flyway.target"
    val OutOfOrder = "flyway.outOfOrder"
    val Resolvers = "flyway.resolvers"
    val Callbacks = "flyway.callbacks"
    val Placeholders = "flyway.placeholders"


    object migration {
      object database {
        val Driver = "%s.driver"
        val Url = "%s.url"
        val User = "%s.user"
        val Password = "%s.password"
      }
    }
  }
}


object FlywayConfig {

  import Keys._

  def apply(config: Config, environment: Option[String] = None): FlywayConfig = {
    FlywayConfig(
      migrations = migrations(config, config.getObject(environment.map(e=>s"$e.db").getOrElse("db"))) //Seq[MigrationConfig]()
    )
  }

  private def migrations(f: Config, config: ConfigObject): Seq[MigrationConfig] = {

    config.map {
      case (name, value) =>
        val v = value.atPath(name)
        import flyway._
        import migration._

        def get[T](path: String, primary: String => T, default: String => T): T = {
          if (v.hasPath(s"$name.$path")) primary(s"$name.$path") else default(path)
        }

        MigrationConfig(
          name = name,
          database = DatabaseConfig(
            driver = v.getString(database.Driver.format(name)),
            url = v.getString(database.Url.format(name)),
            user = if (v.hasPath(database.User.format(name))) Some(v.getString(database.User.format(name))) else None,
            password = if (v.hasPath(database.Password.format(name))) Some(v.getString(database.Password.format(name))) else None
          ),
          enabled = get(Enabled, v.getBoolean, f.getBoolean),
          locations = (get(Locations, v.getStringList, f.getStringList).toList match {
            case Nil => List(s"db/migration/$name")
            case x: List[String] => x
          }).toSeq,
          placeholderPrefix = get(PlaceholderPrefix, v.getString, f.getString),
          placeholderSuffix = get(PlaceholderSuffix, v.getString, f.getString),
          sqlMigrationPrefix = get(SqlMigrationPrefix, v.getString, f.getString),
          sqlMigrationSeparator = get(SqlMigrationSeparator, v.getString, f.getString),
          sqlMigrationSuffix = get(SqlMigrationSuffix, v.getString, f.getString),
          encoding = get(Encoding, v.getString, f.getString),
          schemas = get(Schemas, v.getStringList, f.getStringList),
          table = get(Table, v.getString, f.getString),
          cleanOnValidationError = get(CleanOnValidationError, v.getBoolean, f.getBoolean),
          validateOnMigrate = get(ValidateOnMigrate, v.getBoolean, f.getBoolean),
          baselineVersion = MigrationVersion.fromVersion(get(BaselineVersion, v.getString, f.getString)),
          baselineDescription = get(BaselineDescription, v.getString, f.getString),
          baselineOnMigrate = get(BaselineOnMigrate, v.getBoolean, f.getBoolean),
          ignoreFailedFutureMigration = get(IgnoreFailedFutureMigration, v.getBoolean, f.getBoolean),
          target = MigrationVersion.fromVersion(get(Target, v.getString, f.getString)),
          outOfOrder = get(OutOfOrder, v.getBoolean, f.getBoolean),
          resolvers = get(Resolvers, v.getStringList, f.getStringList),
          callbacks = get(Callbacks, v.getStringList, f.getStringList),
          placeholders = get(Placeholders, v.getObject, f.getObject).mapValues(_.unwrapped().asInstanceOf[String]).toMap
        )
    }.toSeq.sortBy(_.name)
  }





}