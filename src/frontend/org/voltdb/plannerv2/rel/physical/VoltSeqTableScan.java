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

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexProgram;
import org.apache.calcite.rex.RexProgramBuilder;
import org.voltdb.plannerv2.VoltTable;
import org.voltdb.plannerv2.rel.AbstractVoltTableScan;
import org.voltdb.plannerv2.rel.util.PlanCostUtil;
import org.voltdb.plannerv2.rules.physical.Constants;

import com.google.common.base.Preconditions;

/**
 * The relational expression that represent a sequential VoltDB physical table scan.
 *
 * @author Michael Alexeev
 * @since 9.0
 */
public class VoltSeqTableScan extends VoltPhysicalTableScan {

    public VoltSeqTableScan(RelOptCluster cluster,
                               RelTraitSet traitSet,
                               RelOptTable table,
                               VoltTable voltTable,
                               RexProgram program,
                               RexNode offset,
                               RexNode limit,
                               RelNode aggregate,
                               RelDataType preAggregateRowType,
                               RexProgram preAggregateProgram,
                               int splitCount) {
        super(cluster,
                traitSet,
                table,
                voltTable,
                program,
                offset,
                limit,
                aggregate,
                preAggregateRowType,
                preAggregateProgram,
                splitCount);
    }

    public VoltSeqTableScan(RelOptCluster cluster,
                               RelTraitSet traitSet,
                               RelOptTable table,
                               VoltTable voltTable,
                               int splitCount) {
        this(cluster,
                traitSet,
                table,
                voltTable,
                RexProgram.createIdentity(voltTable.getRowType(cluster.getTypeFactory())),
                null, // offset
                null, // limit
                null, // aggregate
                null, // preAggregateRowType
                null, // preAggregateProgram
                splitCount);
        Preconditions.checkArgument(getConvention() == VoltPhysicalRel.CONVENTION);
    }

    @Override
    public double estimateRowCount(RelMetadataQuery mq) {
        Preconditions.checkNotNull(mq);
        double rowCount = Constants.MAX_TABLE_ROW_COUNT;
        rowCount = PlanCostUtil.discountRowCountTableScan(rowCount, m_program);
        // SeqScanPlanNode does not pay attention to limit

        // If table is distributed divide the row count by the split count.
        // The exchange node would combine individual fragments counts into a total.
        int splitCount = mq.splitCount(this);
        return rowCount / splitCount;
    }

    @Override
    public AbstractVoltTableScan copyWithLimitOffset(RelTraitSet traitSet, RexNode offset, RexNode limit) {
        VoltSeqTableScan newScan = new VoltSeqTableScan(
                getCluster(),
                traitSet,
                getTable(),
                getVoltTable(),
                getProgram(),
                offset,
                limit,
                getAggregateRelNode(),
                getPreAggregateRowType(),
                getPreAggregateProgram(),
                m_splitCount);
        return newScan;
    }

    @Override
    public AbstractVoltTableScan copyWithProgram(RelTraitSet traitSet, RexProgram newProgram, RexBuilder programRexBuilder) {
        // Merge two programs program / m_program into a new merged program
        RexProgram mergedProgram = RexProgramBuilder.mergePrograms(
                newProgram,
                m_program,
                programRexBuilder);

        VoltSeqTableScan newScan = new VoltSeqTableScan(
                getCluster(),
                traitSet,
                getTable(),
                getVoltTable(),
                mergedProgram,
                getOffsetRexNode(),
                getLimitRexNode(),
                getAggregateRelNode(),
                getPreAggregateRowType(),
                getPreAggregateProgram(),
                m_splitCount);
        return newScan;
    }

    @Override
    public AbstractVoltTableScan copyWithAggregate(RelTraitSet traitSet, RelNode aggregate) {
        // Need to create a Program for the inline aggregate because it will define
        // the output row type for the scan
        // Preserve the original program and row type
        RexProgram aggProgram = RexProgram.createIdentity(aggregate.getRowType());
        RelDataType preAggRowType = getRowType();
        RexProgram preAggProgram = getProgram();
        VoltSeqTableScan newScan = new VoltSeqTableScan(
                getCluster(),
                traitSet,
                getTable(),
                getVoltTable(),
                aggProgram,
                getOffsetRexNode(),
                getLimitRexNode(),
                aggregate,
                preAggRowType,
                preAggProgram,
                m_splitCount);
        return newScan;
    }
}
