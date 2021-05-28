/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.pentaho.aggdes.model;

import java.util.List;
import java.util.Map;

/**
 * Aggregate designer component that loads a schema.
 */
public interface SchemaLoader extends Component {
  /**
   * Creates a Schema.
   *
   * @param parameterValues Map of parameter values
   * @return Schema
   */
  Schema createSchema(Map<Parameter, Object> parameterValues);

  /**
   * Validates a Schema.
   *
   * @param parameterValues Map of parameter values
   * @return list of validation messages
   */
  List<ValidationMessage> validateSchema(Map<Parameter, Object> parameterValues);
}

// End SchemaLoader.java
