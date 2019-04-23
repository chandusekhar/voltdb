/* This file is part of VoltDB.
 * Copyright (C) 2008-2019 VoltDB Inc.
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

package org.voltdb.plannerv2.rel.physical;

import java.util.Set;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.CorrelationId;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexNode;
import org.voltdb.plannerv2.converter.RexConverter;
import org.voltdb.plannodes.AbstractPlanNode;
import org.voltdb.plannodes.NestLoopPlanNode;
import org.voltdb.types.JoinType;

import com.google.common.collect.ImmutableList;

public class VoltPhysicalNestLoopJoin extends VoltPhysicalJoin {
    public VoltPhysicalNestLoopJoin(RelOptCluster cluster, RelTraitSet traitSet,
            RelNode left, RelNode right, RexNode condition,
            Set<CorrelationId> variablesSet, JoinRelType joinType,
            boolean semiJoinDone,
            ImmutableList<RelDataTypeField> systemFieldList, int splitCount) {
        this(cluster, traitSet, left, right, condition, variablesSet, joinType,
                semiJoinDone, systemFieldList, splitCount, null, null);
    }

    private VoltPhysicalNestLoopJoin(RelOptCluster cluster, RelTraitSet traitSet,
            RelNode left, RelNode right, RexNode condition,
            Set<CorrelationId> variablesSet, JoinRelType joinType,
            boolean semiJoinDone,
            ImmutableList<RelDataTypeField> systemFieldList, int splitCount,
            RexNode offset, RexNode limit) {
        super(cluster, traitSet, left, right, condition, variablesSet, joinType,
                semiJoinDone, systemFieldList, splitCount, offset, limit);
    }

    @Override
    public Join copy(
            RelTraitSet traitSet, RexNode conditionExpr, RelNode left, RelNode right,
            JoinRelType joinType, boolean semiJoinDone) {
        ImmutableList<RelDataTypeField> systemFieldList = ImmutableList.copyOf(getSystemFieldList());
        return new VoltPhysicalNestLoopJoin(getCluster(),
                traitSet, left, right, conditionExpr,
                variablesSet, joinType, semiJoinDone, systemFieldList, getSplitCount());
    }

    @Override
    public VoltPhysicalJoin copyWithLimitOffset(RelTraitSet traits, RexNode offset, RexNode limit) {
        ImmutableList<RelDataTypeField> systemFieldList = ImmutableList.copyOf(getSystemFieldList());
        return new VoltPhysicalNestLoopJoin(
                getCluster(), traits, left, right, condition, variablesSet, joinType, isSemiJoinDone(),
                systemFieldList, getSplitCount(), offset, limit);
    }

    @Override
    public AbstractPlanNode toPlanNode() {
        NestLoopPlanNode nlpn = new NestLoopPlanNode();

        // TODO: INNER join for now
        assert(joinType == JoinRelType.INNER);
        nlpn.setJoinType(JoinType.INNER);

        // Set children
        AbstractPlanNode lch = inputRelNodeToPlanNode(this, 0);
        AbstractPlanNode rch = inputRelNodeToPlanNode(this, 1);
        nlpn.addAndLinkChild(lch);
        nlpn.addAndLinkChild(rch);

        // Set join predicate
        int numLhsFields = getInput(0).getRowType().getFieldCount();
        nlpn.setJoinPredicate(RexConverter.convertJoinPred(numLhsFields, getCondition()));

        // Inline LIMIT / OFFSET
        addLimitOffset(nlpn);

        // Set output schema
        return setOutputSchema(nlpn);
    }
}
