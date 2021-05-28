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
package org.pentaho.aggdes.algorithm.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.pentaho.aggdes.algorithm.Algorithm;
import org.pentaho.aggdes.algorithm.Progress;
import org.pentaho.aggdes.model.Aggregate;
import org.pentaho.aggdes.model.Attribute;
import org.pentaho.aggdes.model.Component;
import org.pentaho.aggdes.model.Parameter;
import org.pentaho.aggdes.model.Schema;
import org.pentaho.aggdes.util.AggDesUtil;
import org.pentaho.aggdes.util.BitSetPlus;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link org.pentaho.aggdes.algorithm.Algorithm}
 * that uses an exhaustive
 * algorithm. The algorithm constructs the full lattice in memory, and uses a
 * greedy algorithm which, at each step, chooses the aggregate which offers
 * the greatest benefit over the current set of aggregates.
 *
 * <p><b>Cost</b>
 *
 * <p>If there are <code>N</code> levels, then the lattice has size
 * <code>2<sup>N</sup></code>. Let's suppose that the algorithm creates
 * <code>M</code> aggregates before each aggregate sees only a marginal benefit.
 * Then the running time is <code>M * 2<sup>N</sup></code>.
 *
 * <p>The cost and memory usage of the algorithm is prohibitive for all but
 * very small schemas. For example,
 * for a schema with 20 levels and 40 aggregate tables, this requires
 * memory for a lattice of 1 million potential aggregates, and 40 million
 * steps to evaluate choose the actual set of aggregate tables.
 */
public abstract class AlgorithmImpl implements Algorithm {
  private static final Log LOGGER = LogFactory.getLog(AlgorithmImpl.class);

  protected final List<Parameter> parameterList = new ArrayList<Parameter>();
  private boolean cancelRequested;
  private boolean canceled;
  protected Schema schema; // set in run
  private long timeoutMillis;
  private Progress progress;

  /**
   * Creates an AggregateTableAdvisorImpl
   */
  protected AlgorithmImpl() {
    parameterList.addAll(Arrays.asList(ParameterEnum.values()));
  }

  public void cancel() {
    cancelRequested = true;
  }

  public String getName() {
    return getBaseName(getClass());
  }

  public static String getBaseName(Class<? extends Component> aClass) {
    String className = aClass.getName();
    int dot = className.lastIndexOf('.');
    if (dot >= 0) {
      className = className.substring(dot + 1);
    }
    return className;
  }

  public List<Parameter> getParameters() {
    return parameterList;
  }


  /**
   * Called by the {@link #run} method, resets the 'canceled' flag and
   * sets the start time for timeout purposes.
   *
   * @param parameterValues Parameters
   * @param progress Progress callback
   */
  protected void onStart(Map<Parameter, Object> parameterValues,
      Progress progress) {
    this.progress = progress;
    this.canceled = false;
    final Integer integer =
        (Integer) parameterValues.get(ParameterEnum.TIME_LIMIT_SECONDS);
    if (integer == null) {
      this.timeoutMillis = Long.MAX_VALUE;
    } else {
      this.timeoutMillis =
          System.currentTimeMillis()
              + 1000L * integer;
    }
  }

  /**
   * Checks whether this algorithm has been canceled or has exceeded its
   * maximum running time.
   *
   * <p>Algorithm must call this method regularly and terminate if it returns
   * true.
   *
   * @return whether algorithm needs to terminate due to either cancelation
   * or timeout
   */
  protected boolean checkCancelTimeout() {
    if (canceled) {
      return true;
    }
    if (cancelRequested) {
      canceled = true;
      cancelRequested = false;
      progress.report("Algorithm was canceled", 1.0);
      return true;
    }
    final long currentTimeMillis = System.currentTimeMillis();
    if (currentTimeMillis > timeoutMillis) {
      canceled = true;
      progress.report("Algorithm exceeded time limit", 1.0);
      return true;
    }
    return false;
  }

  public Aggregate createAggregate(Schema schema, List<Attribute> attributeList) {
    this.schema = schema;
    final BitSetPlus bitSet =
        new BitSetPlus(schema.getAttributes().size());
    for (Attribute attribute : attributeList) {
      bitSet.set(schema.getAttributes().indexOf(attribute));
    }
    return new AggregateImpl(schema, bitSet);
  }

  protected ResultImpl runAlgorithm(Lattice lattice, double costLimit,
      double minCostBenefitRatio, int aggregateLimit) {
    double costPerAggregate;
    if (aggregateLimit < Integer.MAX_VALUE) {
      // Simulate an aggregate limit by charging 1/n for each aggregate.
      // WG: the algorithm was returning N -1 aggregates, I've modified
      // this as a temporary solution to the problem
      costPerAggregate = costLimit / (double) (aggregateLimit + 1);
    } else {
      costPerAggregate = 0;
    }
    Cost aggCost = new Cost();
    Cost totalCost = new Cost();
    final double minBenefit = 1.0;
    double remainingCost = costLimit;
    while (true) {
      // Have we timed out or been canceled?
      if (checkCancelTimeout()) {
        break;
      }
      // Choose an aggregate to materialize.
      AggregateImpl aggregate =
          lattice.chooseAggregate(
              remainingCost, minCostBenefitRatio, aggCost);
      // If there is no aggregate left, we're done.
      if (aggregate == null) {
        break;
      }
      // If the aggregate does not have a minimum amount of benefit (for
      // example, an aggregate with 999K rows for a fact table with 1m
      // rows), ignore it.
      if (aggCost.benefit < minBenefit) {
        break;
      }
      final double cost = aggCost.cost + costPerAggregate;
      totalCost.cost += cost;
      totalCost.benefit += aggCost.benefit;
      totalCost.benefitCount += aggCost.benefitCount;
      remainingCost -= cost;
      if (remainingCost <= 0) {
        break;
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("materialize " + aggregate.getDescription()
            + ": rows=" + aggregate.estimateRowCount()
            + ", aggCost=" + aggCost.cost
            + ", benefit=" + aggCost.benefit
            + ", count=" + aggCost.benefitCount);
      }

      // Materialize it, and propagate aggCost improvements to children.
      lattice.materialize(aggregate);
    }

    final List<Aggregate> aggregateList =
        AggDesUtil.cast(lattice.getMaterializedAggregates());

    final List<CostBenefit> costBenefitList =
        computeAggregateCosts(schema, null, aggregateList);

    // Print out the result.
    if (LOGGER.isDebugEnabled()) {
      for (AggregateImpl aggregate : lattice.getMaterializedAggregates()) {
        LOGGER.debug("table: " + aggregate.getDescription()
            + " (" + aggregate.estimateRowCount() + " rows)");
      }
    }
    return new ResultImpl(aggregateList, costBenefitList, costLimit,
        totalCost.cost, totalCost.benefit);
  }

  /**
   * Implementation of {@link CostBenefit}.
   */
  static class CostBenefitImpl implements CostBenefit {
    private final AggregateImpl aggregate;
    private final double costSavingPerQuery;
    private Schema schema;

    CostBenefitImpl(Schema schema, AggregateImpl aggregate,
        double costSavingPerQuery) {
      this.aggregate = aggregate;
      this.costSavingPerQuery = costSavingPerQuery;
      this.schema = schema;
    }

    public double getRowCount() {
      return aggregate.estimateRowCount();
    }

    public double getSpace() {
      return aggregate.estimateSpace();
    }

    public double getLoadTime() {
      return schema.getStatisticsProvider()
          .getLoadTime(aggregate.getAttributes());
    }

    public double getSavedQueryRowCount() {
      return costSavingPerQuery;
    }

    public void describe(PrintWriter pw) {
      pw.printf(
          "%d rows, %d bytes, %d load cost, %d query rows saved, used by %d%% of queries",
          (int) getRowCount(),
          (int) getSpace(),
          (int) getLoadTime(),
          (int) getSavedQueryRowCount(),
          (int) (aggregate.queryLoad * 100d));
    }
  }
}

// End AlgorithmImpl.java
