/*
 * Copyright 2018 phData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.phdata.pipewrench.domain

import java.io.FileWriter

import com.typesafe.scalalogging.LazyLogging
import net.jcazevedo.moultingyaml.DefaultYamlProtocol

trait YamlSupport extends DefaultYamlProtocol with LazyLogging {

  import net.jcazevedo.moultingyaml._

  implicit def pipewrenchEnvironmentFormat = yamlFormat7(Environment)

  implicit def pipewrenchColumnFormat        = yamlFormat5(Column)
  implicit def pipewrenchTableFormat         = yamlFormat7(Table)
  implicit def pipewrenchConfigurationFormat = yamlFormat10(Configuration)

  implicit class WriteEnvironmentYamlFile(environment: Environment) {
    def writeYamlFile(path: String): Unit = writeFile(environment.toYaml, path)
  }

  implicit class WriteConfigurationYamlFile(configuration: Configuration) {
    def writeYamlFile(path: String): Unit = writeFile(configuration.toYaml, path)
  }

  private def writeFile(yaml: YamlValue, path: String): Unit = {
    val fw = new FileWriter(path)
    logger.debug(s"Writing file: $path")
    fw.write(yaml.prettyPrint)
    fw.close()
  }
}
