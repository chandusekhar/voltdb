/* This file is part of VoltDB.
 * Copyright (C) 2008-2018 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb.calciteadapter.rel.physical;

import java.util.List;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.ImmutableBitSet;
import org.voltdb.calciteadapter.converter.ExpressionTypeConverter;
import org.voltdb.calciteadapter.converter.RelConverter;
import org.voltdb.calciteadapter.converter.RexConverter;
import org.voltdb.calciteadapter.planner.CalcitePlanningException;
import org.voltdb.expressions.AbstractExpression;
import org.voltdb.plannodes.AbstractPlanNode;
import org.voltdb.plannodes.AggregatePlanNode;
import org.voltdb.plannodes.NodeSchema;
import org.voltdb.types.ExpressionType;

public abstract class AbstractVoltDBPAggregate extends Aggregate implements VoltDBPRel {

    // HAVING expression
    final protected RexNode m_postPredicate;

    /** Constructor */
    protected AbstractVoltDBPAggregate(
            RelOptCluster cluster,
            RelTraitSet traitSet,
            RelNode child,
            boolean indicator,
            ImmutableBitSet groupSet,
            List<ImmutableBitSet> groupSets,
            List<AggregateCall> aggCalls,
            RexNode postPredicate) {
      super(cluster, traitSet, child, indicator, groupSet, groupSets, aggCalls);
      m_postPredicate = postPredicate;
    }

    @Override
    public RelWriter explainTerms(RelWriter pw) {
        super.explainTerms(pw);
        if (m_postPredicate != null) {
            pw.item("having", m_postPredicate);
        }
        return pw;
    }

    @Override
    protected String computeDigest() {
        String digest = super.computeDigest();
        if (m_postPredicate != null) {
            digest += m_postPredicate.toString();
        }
        return digest;
    }

    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner,
            RelMetadataQuery mq) {
        double rowCount = getInput().estimateRowCount(mq);
        RelTrait aggrCollationTrait = traitSet.getTrait(RelCollationTraitDef.INSTANCE);
        // @TODO Give a discount to Serial Aggregation based on the numer of collation fields
        if (aggrCollationTrait instanceof RelCollation) {
            RelCollation aggrCollation = (RelCollation) aggrCollationTrait;
            double discountFactor = 1.0;
            final double MAX_PER_COLLATION_DISCOUNT = 0.1;
            for (int i = 0; i < aggrCollation.getFieldCollations().size(); ++i) {
                discountFactor -= Math.pow(MAX_PER_COLLATION_DISCOUNT, i + 1);
            }
            rowCount *= discountFactor;
        }
        return planner.getCostFactory().makeCost(rowCount, 0, 0);
    }

    /**
     * Copy self
     * @param cluster
     * @param traitSet
     * @param input
     * @param indicator
     * @param groupSet
     * @param groupSets
     * @param aggCalls
     * @param postPredicate
     * @return
     */
    public abstract AbstractVoltDBPAggregate copy(
            RelOptCluster cluster,
            RelTraitSet traitSet,
            RelNode input,
            boolean indicator,
            ImmutableBitSet groupSet,
            List<ImmutableBitSet> groupSets,
            List<AggregateCall> aggCalls,
            RexNode postPredicate);

    protected abstract AggregatePlanNode getAggregatePlanNode();

    @Override
    public AbstractPlanNode toPlanNode() {
        AbstractPlanNode apn = toPlanNode(getInput().getRowType());

        // Convert child
        AbstractPlanNode child = inputRelNodeToPlanNode(this, 0);
        apn.addAndLinkChild(child);

        return apn;
    }

    /**
     * Convert self to a VoltDB plan node
     *
     * @param inputRowType
     * @return
     */
    public AbstractPlanNode toPlanNode(RelDataType inputRowType) {
        AggregatePlanNode apn = getAggregatePlanNode();

        // Generate output schema
        NodeSchema schema = RexConverter.convertToVoltDBNodeSchema(getRowType());
        apn.setOutputSchema(schema);

        // The Aggregate's record layout seems to be
        // - GROUP BY expressions
        // - AGGR expressions form SELECT clause - corresponding aggrCall has name matching the filed name
        // - AGGR expressions from HAVING clause - aggrCall name is NULL
        RelDataType aggrRowType = getRowType();
        List<RelDataTypeField> fields = inputRowType.getFieldList();
        // Aggreagte fields start right after the grouping ones in order of the aggregate calls
        int aggrFieldIdx = 0 + getGroupCount();
        for(AggregateCall aggrCall : getAggCallList()) {
            // Aggr type
            ExpressionType aggrType =
                    ExpressionTypeConverter.calicteTypeToVoltType(aggrCall.getAggregation().kind);
            if (aggrType == null) {
                throw new CalcitePlanningException("Unsupported aggregate function: " + aggrCall.getAggregation().kind.lowerName);
            }

            List<Integer> aggrExprIndexes = aggrCall.getArgList();
            // VoltDB supports aggregates with only one parameter
            assert(aggrExprIndexes.size() < 2);
            AbstractExpression aggrExpr = null;
            if (!aggrExprIndexes.isEmpty()) {
                RelDataTypeField field = fields.get(aggrExprIndexes.get(0));
                aggrExpr = RelConverter.convertDataTypeField(field);
            } else if (ExpressionType.AGGREGATE_COUNT == aggrType) {
                aggrType = ExpressionType.AGGREGATE_COUNT_STAR;
            }

            assert(aggrFieldIdx < aggrRowType.getFieldCount());
            apn.addAggregate(aggrType, aggrCall.isDistinct(),  aggrFieldIdx, aggrExpr);
            // Increment aggregate field index
            aggrFieldIdx++;
        }
        // Group by
        setGroupByExpressions(apn);
        // Having
        setPostPredicate(apn);

        return apn;
    }

    private void setGroupByExpressions(AggregatePlanNode hapn) {
        ImmutableBitSet groupBy = getGroupSet();
        List<RelDataTypeField> rowTypeList = this.getRowType().getFieldList();
        for (int index = groupBy.nextSetBit(0); index != -1; index = groupBy.nextSetBit(index + 1)) {
            assert(index < rowTypeList.size());
            AbstractExpression groupByExpr = RelConverter.convertDataTypeField(rowTypeList.get(index));
            hapn.addGroupByExpression(groupByExpr);
        }
    }

    private void setPostPredicate(AggregatePlanNode hapn) {
        if (m_postPredicate != null) {
            AbstractExpression havingExpression = RexConverter.convert(m_postPredicate);
            hapn.setPostPredicate(havingExpression);
        }
    }

}
