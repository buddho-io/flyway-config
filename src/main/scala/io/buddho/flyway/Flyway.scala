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

import java.sql.Connection

import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.api.MigrationInfo
import org.flywaydb.core.api.callback.FlywayCallback
import org.flywaydb.core.{Flyway => FlywayCore}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.util.{Success, Failure, Try}
import scala.collection.JavaConversions._

import scala.concurrent.duration._

class Flyway(config: FlywayConfig, implicit val ec: ExecutionContext) extends StrictLogging {

  def migrate(throwOnFailure: Boolean = false, timeout: FiniteDuration = 60.seconds): Seq[Try[Int]] =
    awaitOrFail(throwOnFailure, timeout, migrateAsync)

  def migrateAsync(): Future[Seq[Try[Int]]] = {
    Future.sequence {
      config.migrations.map(m => Future(Try(flyway(m).migrate())))
    }
  }

  def validate(throwOnFailure: Boolean = false, timeout: FiniteDuration = 60.seconds): Seq[Try[Unit]] =
    awaitOrFail(throwOnFailure, timeout, validateAsync)

  def validateAsync(): Future[Seq[Try[Unit]]] = {
    Future.sequence {
      config.migrations.map(m => Future(Try(flyway(m).validate())))
    }
  }

  def clean(throwOnFailure: Boolean = false, timeout: FiniteDuration = 60.seconds): Seq[Try[Unit]] =
    awaitOrFail(throwOnFailure, timeout, cleanAsync)

  def cleanAsync(): Future[Seq[Try[Unit]]] = {
    Future.sequence {
      config.migrations.map(m => Future(Try(flyway(m).clean())))
    }
  }

  def repair(throwOnFailure: Boolean = false, timeout: FiniteDuration = 60.seconds): Seq[Try[Unit]] =
    awaitOrFail(throwOnFailure, timeout, repairAsync)

  def repairAsync(): Future[Seq[Try[Unit]]] = {
    val results = config.migrations.map(m => Future(Try(flyway(m).repair())))
    Future.sequence(results.toList)
  }

  private def awaitOrFail[T](throwOnFailure: Boolean = false, timeout: FiniteDuration, f: () => Future[Seq[Try[T]]]): Seq[Try[T]] = {
    val results = Await.result(f(), timeout)
    if (throwOnFailure) {
      results.toList.filter(_.isFailure) match {
        case Failure(e) :: _ => throw e
        case _ =>
      }
    }
    results
  }

  private def flyway(m: MigrationConfig): FlywayCore = {
    val f = new FlywayCore()
    f.setLocations(m.locations: _*)
    f.setPlaceholderPrefix(m.placeholderPrefix)
    f.setPlaceholderSuffix(m.placeholderSuffix)
    f.setSqlMigrationPrefix(m.sqlMigrationPrefix)
    f.setSqlMigrationSeparator(m.sqlMigrationSeparator)
    f.setSqlMigrationSuffix(m.sqlMigrationSuffix)
    f.setEncoding(m.encoding)
    f.setSchemas(m.schemas: _*)
    f.setTable(m.table)
    f.setCleanOnValidationError(m.cleanOnValidationError)
    f.setValidateOnMigrate(m.validateOnMigrate)
    f.setBaselineVersion(m.baselineVersion)
    f.setBaselineDescription(m.baselineDescription)
    f.setBaselineOnMigrate(m.baselineOnMigrate)
    f.setIgnoreFailedFutureMigration(m.ignoreFailedFutureMigration)
    f.setTarget(m.target)
    f.setOutOfOrder(m.outOfOrder)
    f.setResolversAsClassNames(m.resolvers: _*)
    f.setCallbacksAsClassNames(m.callbacks: _*)
    f.setPlaceholders(m.placeholders)
    f.setCallbacks(new LoggingCallback :: f.getCallbacks.toList: _*)
    f.setDataSource(m.database.url, m.database.user.orNull, m.database.password.orNull)
    f
  }

  class LoggingCallback extends FlywayCallback {
    override def afterInfo(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > info task starting")

    override def beforeInit(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > init task starting")

    override def beforeBaseline(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > baseline task starting")

    override def beforeRepair(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > repair task starting")

    override def afterInit(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > init task finished")

    override def afterRepair(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > repair task finished")

    override def afterValidate(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > validate task finished")

    override def beforeEachMigrate(connection: Connection, info: MigrationInfo): Unit =
      logger.info(s"${connection.getCatalog} > starting migration for ${info.getDescription} - ${info.getVersion}")

    override def afterEachMigrate(connection: Connection, info: MigrationInfo): Unit =
      logger.info(s"${connection.getCatalog} > finished migration for ${info.getDescription} - ${info.getVersion} in ${info.getExecutionTime} millis")

    override def afterMigrate(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > migration task finished")

    override def beforeValidate(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > validate task starting")

    override def beforeInfo(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > info task starting")

    override def afterBaseline(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > baseline task finished")

    override def afterClean(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > clean task finished")

    override def beforeClean(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > clean task starting")

    override def beforeMigrate(connection: Connection): Unit =
      logger.info(s"${connection.getCatalog} > migration task starting")
  }
}
