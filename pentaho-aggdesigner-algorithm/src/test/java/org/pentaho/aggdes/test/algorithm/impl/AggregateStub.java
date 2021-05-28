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
package org.pentaho.aggdes.test.algorithm.impl;

import org.pentaho.aggdes.model.Aggregate;
import org.pentaho.aggdes.model.Attribute;
import org.pentaho.aggdes.model.Measure;

import java.util.List;

/** Implementation of {@link Aggregate}. */
public class AggregateStub implements Aggregate {

  public double estimateRowCount() {
    // TODO Auto-generated method stub
    return 0;
  }

  public double estimateSpace() {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Attribute> getAttributes() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getCandidateTableName() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Measure> getMeasures() {
    // TODO Auto-generated method stub
    return null;
  }

}

// End AggregateStub.java
