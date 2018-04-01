// Copyright (C) 2011-2012 the original author or authors.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.buddho.flyway

import java.util

import com.typesafe.config.{Config, ConfigObject}
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.resolver.MigrationResolver
import org.flywaydb.core.internal.util.{Locations => UtilLocations}

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._


case class DatabaseConfig(
                           driver: String,
                           url: String,
                           user: Option[String],
                           password: Option[String])

case class MigrationConfig(
                            name: Symbol,
                            database: DatabaseConfig,
                            enabled: Boolean,
                            cleanDisabled: Boolean,
                            group: Boolean,
                            locations: Seq[String],
                            placeholderPrefix: String,
                            placeholderSuffix: String,
                            placeholderReplacement: Boolean,
                            sqlMigrationPrefix: String,
                            sqlMigrationSeparator: String,
                            installedBy: String,
                            encoding: String,
                            schemas: Seq[String],
                            table: String,
                            mixed: Boolean,
                            cleanOnValidationError: Boolean,
                            validateOnMigrate: Boolean,
                            baselineVersion: MigrationVersion,
                            baselineDescription: String,
                            baselineOnMigrate: Boolean,
                            ignoreFutureMigrations: Boolean,
                            ignoreMissingMigrations: Boolean,
                            target: MigrationVersion,
                            outOfOrder: Boolean,
                            resolvers: Seq[String],
                            callbacks: Seq[String],
                            placeholders: Map[String, String],
                            skipDefaultCallbacks: Boolean,
                            skipDefaultResolvers: Boolean,
                            sqlMigrationSuffixes: Seq[String],
                            repeatableSqlMigrationPrefix: String
                          )


case class FlywayConfig(migrations: Seq[MigrationConfig])


object Keys {
  object Flyway {
    val Enabled = "flyway.enabled"
    val Group = "flyway.group"
    val Locations = "flyway.locations"
    val InstalledBy = "flyway.installedBy"
    val Mixed = "flyway.mixed"
    val Resolvers = "flyway.resolvers"
    val CleanDisabled = "flyway.cleanDisabled"
    val PlaceholderPrefix = "flyway.placeholderPrefix"
    val PlaceholderSuffix = "flyway.placeholderSuffix"
    val PlaceholderReplacement = "flyway.placeholderReplacement"
    val SqlMigrationPrefix = "flyway.sqlMigrationPrefix"
    val SqlMigrationSeparator = "flyway.sqlMigrationSeparator"
    val Encoding = "flyway.encoding"
    val Schemas = "flyway.schemas"
    val Table = "flyway.table"
    val CleanOnValidationError = "flyway.cleanOnValidationError"
    val ValidateOnMigrate = "flyway.validateOnMigrate"
    val BaselineVersion = "flyway.baselineVersion"
    val BaselineDescription = "flyway.baselineDescription"
    val BaselineOnMigrate = "flyway.baselineOnMigrate"
    val BaselineVersionAsString = "flyway.baselineVersionAsString"
    val IgnoreFutureMigrations = "flyway.ignoreFutureMigrations"
    val IgnoreMissingMigrations = "flyway.ignoreMissingMigrations"
    val Target = "flyway.target"
    val OutOfOrder = "flyway.outOfOrder"
    val Callbacks = "flyway.callbacks"
    val Placeholders = "flyway.placeholders"
    val SkipDefaultCallbacks = "flyway.skipDefaultCallbacks"
    val SkipDefaultResolvers = "flyway.skipDefaultResolvers"
    val SqlMigrationSuffixes = "flyway.sqlMigrationSuffixes"
    val RepeatableSqlMigrationPrefix = "flyway.repeatableSqlMigrationPrefix"


    object Migration {
      object Database {
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
        import Flyway._
        import Migration._

        def get[T](path: String, primary: String => T, default: String => T): T = {
          if (v.hasPath(s"$name.$path")) primary(s"$name.$path") else default(path)
        }

        MigrationConfig(
          name = Symbol(name),
          group = get(Group, v.getBoolean, f.getBoolean),
          database = DatabaseConfig(
            driver = v.getString(Database.Driver.format(name)),
            url = v.getString(Database.Url.format(name)),
            user = if (v.hasPath(Database.User.format(name))) Some(v.getString(Database.User.format(name))) else None,
            password = if (v.hasPath(Database.Password.format(name))) Some(v.getString(Database.Password.format(name))) else None
          ),
          enabled = get(Enabled, v.getBoolean, f.getBoolean),
          locations = get(Locations, v.getStringList, f.getStringList).toList match {
            case Nil => List(s"db/migration/$name")
            case x: List[String] => x
          },
          cleanDisabled = get(CleanDisabled, v.getBoolean, f.getBoolean),
          mixed = get(Mixed, v.getBoolean, f.getBoolean),
          installedBy = if (v.hasPath(InstalledBy)) get(InstalledBy, v.getString, f.getString) else null,
          placeholderPrefix = get(PlaceholderPrefix, v.getString, f.getString),
          placeholderSuffix = get(PlaceholderSuffix, v.getString, f.getString),
          placeholderReplacement = get(PlaceholderReplacement, v.getBoolean, f.getBoolean),
          sqlMigrationPrefix = get(SqlMigrationPrefix, v.getString, f.getString),
          sqlMigrationSeparator = get(SqlMigrationSeparator, v.getString, f.getString),
          encoding = get(Encoding, v.getString, f.getString),
          schemas = get(Schemas, v.getStringList, f.getStringList),
          table = get(Table, v.getString, f.getString),
          cleanOnValidationError = get(CleanOnValidationError, v.getBoolean, f.getBoolean),
          validateOnMigrate = get(ValidateOnMigrate, v.getBoolean, f.getBoolean),
          baselineVersion = MigrationVersion.fromVersion(get(BaselineVersion, v.getString, f.getString)),
          baselineDescription = get(BaselineDescription, v.getString, f.getString),
          baselineOnMigrate = get(BaselineOnMigrate, v.getBoolean, f.getBoolean),
          ignoreFutureMigrations = get(IgnoreFutureMigrations, v.getBoolean, f.getBoolean),
          ignoreMissingMigrations = get(IgnoreMissingMigrations, v.getBoolean, f.getBoolean),
          target = MigrationVersion.fromVersion(get(Target, v.getString, f.getString)),
          outOfOrder = get(OutOfOrder, v.getBoolean, f.getBoolean),
          resolvers = get(Resolvers, v.getStringList, f.getStringList),
          callbacks = get(Callbacks, v.getStringList, f.getStringList),
          skipDefaultCallbacks = get(SkipDefaultCallbacks, v.getBoolean, f.getBoolean),
          skipDefaultResolvers = get(SkipDefaultResolvers, v.getBoolean, f.getBoolean),
          sqlMigrationSuffixes = get(SqlMigrationSuffixes, v.getStringList, f.getStringList),
          repeatableSqlMigrationPrefix = get(RepeatableSqlMigrationPrefix, v.getString, f.getString),
          placeholders = get(Placeholders, v.getObject, f.getObject).mapValues(_.unwrapped().asInstanceOf[String]).toMap
        )
    }.toSeq
  }
}
