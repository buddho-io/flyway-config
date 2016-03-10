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
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.api.MigrationInfo
import org.flywaydb.core.api.callback.FlywayCallback
import org.flywaydb.core.{Flyway => FlywayCore}

import scala.collection.JavaConversions._
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try


class Flyway(config: FlywayConfig)(implicit ec: ExecutionContext) extends StrictLogging {

  private val pool = new ConcurrentHashMap[Symbol, Option[FlywayCore]]()

  def getDataSource(name: Symbol): Option[DataSource] = pool.getOrElseUpdate(name, flyway(name)).map(_.getDataSource)

  // ------------------

  def migrate(name: Symbol): Option[Try[Int]] = migrate(name, 60.seconds)

  def migrate(name: Symbol, dataSource: DataSource): Option[Try[Int]] = migrate(name, dataSource, 60.seconds)

  def migrate(name: Symbol, dataSource: DataSource, timeout: FiniteDuration): Option[Try[Int]] =
    migrateAsync(name, dataSource) match {
      case Some(f) => Some(Await.result(f, timeout))
      case None => None
    }

  def migrate(name: Symbol, timeout: FiniteDuration): Option[Try[Int]] =
    migrateAsync(name) match {
      case Some(f) => Some(Await.result(f, timeout))
      case None => None
    }

  def migrateAll(timeout: FiniteDuration = 60.seconds): Seq[Try[Int]] =
    Await.result(Future.sequence(migrateAllAsync()), timeout)

  def migrateAsync(name: Symbol): Option[Future[Try[Int]]] =
    flyway(name).map(f => Future(Try(f.migrate())))

  def migrateAsync(name: Symbol, dataSource: DataSource): Option[Future[Try[Int]]] =
    flyway(name, dataSource).map(f => Future(Try(f.migrate())))

  def migrateAllAsync(): Seq[Future[Try[Int]]] =
    config.migrations.map(m => Future(Try(flyway(m.name).get.migrate())))

  // ------------------


  def validate(name: Symbol): Option[Try[Unit]] = validate(name, 60.seconds)

  def validate(name: Symbol, dataSource: DataSource): Option[Try[Unit]] = validate(name, dataSource, 60.seconds)

  def validate(name: Symbol, dataSource: DataSource, timeout: FiniteDuration): Option[Try[Unit]] =
    validateAsync(name, dataSource) match {
      case Some(f) => Some(Await.result(f, timeout))
      case None => None
    }

  def validate(name: Symbol, timeout: FiniteDuration): Option[Try[Unit]] =
    validateAsync(name) match {
      case Some(f) => Some(Await.result(f, timeout))
      case _ => None
    }

  def validateAll(timeout: FiniteDuration = 60.seconds): Seq[Try[Unit]] =
    Await.result(Future.sequence(validateAllAsync()), timeout)

  def validateAsync(name: Symbol): Option[Future[Try[Unit]]] =
    flyway(name).map(f => Future(Try(f.validate())))

  def validateAsync(name: Symbol, dataSource: DataSource): Option[Future[Try[Unit]]] =
    flyway(name, dataSource).map(f => Future(Try(f.validate())))

  def validateAllAsync(): Seq[Future[Try[Unit]]] =
    config.migrations.map(m => Future(Try(flyway(m.name).get.validate())))

  // ------------------

  def clean(name: Symbol): Option[Try[Unit]] = clean(name, 60.seconds)

  def clean(name: Symbol, dataSource: DataSource): Option[Try[Unit]] = clean(name, dataSource, 60.seconds)

  def clean(name: Symbol, dataSource: DataSource, timeout: FiniteDuration): Option[Try[Unit]] =
    cleanAsync(name, dataSource) match {
      case Some(f) => Some(Await.result(f, timeout))
      case None => None
    }

  def clean(name: Symbol, timeout: FiniteDuration): Option[Try[Unit]] =
    cleanAsync(name) match {
      case Some(f) => Some(Await.result(f, timeout))
      case None => None
    }

  def cleanAll(throwOnFailure: Boolean = false, timeout: FiniteDuration = 60.seconds): Seq[Try[Unit]] =
    Await.result(Future.sequence(cleanAllAsync()), timeout)

  def cleanAsync(name: Symbol): Option[Future[Try[Unit]]] =
    flyway(name).map(f => Future(Try(f.clean())))

  def cleanAsync(name: Symbol, dataSource: DataSource): Option[Future[Try[Unit]]] =
    flyway(name, dataSource).map(f => Future(Try(f.clean())))

  def cleanAllAsync(): Seq[Future[Try[Unit]]] =
    config.migrations.map(m => Future(Try(flyway(m.name).get.clean())))

  // ------------------

  def repair(name: Symbol, throwOnFailure: Boolean = false, timeout: FiniteDuration = 60.seconds): Option[Try[Unit]] =
    repairAsync(name) match {
      case Some(f) => Some(Await.result(f, timeout))
      case None => None
    }

  def repairAll(throwOnFailure: Boolean = false, timeout: FiniteDuration = 60.seconds): Seq[Try[Unit]] =
    Await.result(Future.sequence(repairAllAsync()), timeout)

  def repairAsync(name: Symbol): Option[Future[Try[Unit]]] =
    flyway(name).map(f => Future(Try(f.repair())))

  def repairAllAsync(): Seq[Future[Try[Unit]]] =
    config.migrations.map(m => Future(Try(flyway(m.name).get.repair())))

  private def flyway(name: Symbol, dataSource: DataSource): Option[FlywayCore] =
    pool.getOrElseUpdate(name, config.migrations.find(_.name == name).map { m =>
      val f = flyway(m)
      f.setDataSource(dataSource)
      f
    })

  private def flyway(name: Symbol): Option[FlywayCore] =
    pool.getOrElseUpdate(name, config.migrations.find(_.name == name).map(m => flyway(m)))


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
