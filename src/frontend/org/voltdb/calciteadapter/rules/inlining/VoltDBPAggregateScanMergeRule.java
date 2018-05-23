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

package org.voltdb.calciteadapter.rules.inlining;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.rel.RelNode;
import org.voltdb.calciteadapter.rel.physical.AbstractVoltDBPAggregate;
import org.voltdb.calciteadapter.rel.physical.AbstractVoltDBPTableScan;

public class VoltDBPAggregateScanMergeRule extends RelOptRule {

    public static final VoltDBPAggregateScanMergeRule INSTANCE = new VoltDBPAggregateScanMergeRule();

    private VoltDBPAggregateScanMergeRule() {
        super(operand(AbstractVoltDBPAggregate.class,
                operand(AbstractVoltDBPTableScan.class, none())));
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
        AbstractVoltDBPAggregate aggregate = call.rel(0);
        AbstractVoltDBPTableScan scan = call.rel(1);

        RelNode newScan = scan.copyWithAggregate(scan.getTraitSet(), aggregate);
        call.transformTo(newScan);
    }

}
