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

package org.voltdb.plannerv2.rules.logical;

import java.util.ArrayList;

import com.google_voltpatches.common.base.Preconditions;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.*;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.rules.PushProjector;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexProgram;
import org.apache.calcite.tools.RelBuilderFactory;
import org.apache.calcite.util.Pair;

import com.google.common.collect.ImmutableList;
import org.voltdb.plannerv2.rel.logical.VoltLogicalCalc;
import org.voltdb.plannerv2.rel.logical.VoltLogicalJoin;
import org.voltdb.plannerv2.rel.logical.VoltLogicalRel;
import org.voltdb.types.JoinType;

/**
 * Planner rule that pushes a {@link org.voltdb.plannerv2.rel.logical.VoltLogicalCalc}
 * past a {@link org.voltdb.plannerv2.rel.logical.VoltLogicalJoin}
 * by splitting the projection into a projection on top of each child of the join.
 *
 * Adopted from {@link org.apache.calcite.rel.rules.ProjectJoinTransposeRule}
 */
public class VoltLCalcJoinTransposeRule extends RelOptRule {

    public static final RelOptRule INSTANCE =
            new VoltLCalcJoinTransposeRule(expr -> true, RelFactories.LOGICAL_BUILDER);

    //~ Instance fields --------------------------------------------------------

    /**
     * Condition for expressions that should be preserved in the projection.
     */
    private final PushProjector.ExprCondition m_preserveExprCondition;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a VoltDBLCalcJoinTransposeRule with an explicit condition.
     *
     * @param preserveExprCondition Condition for expressions that should be
     *                              preserved in the projection
     */
    private VoltLCalcJoinTransposeRule(
            PushProjector.ExprCondition preserveExprCondition,
            RelBuilderFactory relFactory) {
        super(operand(VoltLogicalCalc.class,
                operand(VoltLogicalJoin.class, any())),
                relFactory, null);
        m_preserveExprCondition = preserveExprCondition;
    }

    //~ Methods ----------------------------------------------------------------

    // implement RelOptRule
    public void onMatch(RelOptRuleCall call) {
        final Calc origCalc = call.rel(0);
        final Join join = call.rel(1);

        if (join.getJoinType() != JoinRelType.INNER || join instanceof SemiJoin) {
            return; // TODO: support SemiJoin; other types of join
        }
        // Split project (left) and filter (right) expressions
        final Pair<ImmutableList<RexNode>, ImmutableList<RexNode>> projectFilter =
                origCalc.getProgram().split();
        // Can push down only if this Calc does not have any filters
        if (!projectFilter.right.isEmpty()) {
            return;
        }
        // Re-create a LogicalProject to be able to pass it to the PushProjector
        final Project origProject = new LogicalProject(
                origCalc.getCluster(), origCalc.getTraitSet().replace(Convention.NONE), origCalc.getInput(),
                projectFilter.left, origCalc.getRowType());

        // locate all fields referenced in the projection and join condition;
        // determine which inputs are referenced in the projection and
        // join condition; if all fields are being referenced and there are no
        // special expressions, no point in proceeding any further
        final PushProjector pushProject = new PushProjector(
                origProject, join.getCondition(), join, m_preserveExprCondition, call.builder());
        if (pushProject.locateAllRefs()) {
            return;
        }

        // create left and right projections, projecting only those
        // fields referenced on each side
        final RelNode leftProjRel = pushProject.createProjectRefsAndExprs(
                join.getLeft(), true, false);
        // Convert LogicalProject to a VoltDBLCalc
        final Calc leftCalcRel = projectToVoltCalc(leftProjRel, false);

        final RelNode rightProjRel = pushProject.createProjectRefsAndExprs(
                join.getRight(), true, true);
        final Calc rightCalcRel = projectToVoltCalc(rightProjRel, false);

        // convert the join condition to reference the projected columns
        final RexNode newJoinFilter;
        final int[] adjustments = pushProject.getAdjustments();
        if (join.getCondition() != null) {
            newJoinFilter = pushProject.convertRefsAndExprs(join.getCondition(),
                    new ArrayList<RelDataTypeField>() {{
                        addAll(join.getSystemFieldList());
                        addAll(leftProjRel.getRowType().getFieldList());
                        addAll(rightProjRel.getRowType().getFieldList());
                    }},
                    adjustments);
        } else {
            newJoinFilter = null;
        }

        // create a new join with the projected children
        final Join newJoinRel = join.copy(
                join.getTraitSet(), newJoinFilter, leftCalcRel,
                rightCalcRel, join.getJoinType(), join.isSemiJoinDone());

        // put the original project on top of the join, converting it to
        // reference the modified projection list
        RelNode resultRel = pushProject.createNewProject(newJoinRel, adjustments);
        // There may be a case when all the projects were pushed down to children.
        // In this case the resultRel is a Join
        if (resultRel instanceof Project) {
            resultRel = projectToVoltCalc(resultRel, true);
        }
        call.transformTo(resultRel);
    }

    /**
     * Convert LogicalProject to a VoltDBLCalc
     *
     * @param relNode LogicalProject
     * @param isTopJoin TRUE is this Project is on top of a join. FALSE otherwise
     *
     * @return VoltDBLCalc
     */
    private Calc projectToVoltCalc(RelNode relNode, boolean isTopJoin) {
        Preconditions.checkState(relNode instanceof Project, "RelNode is not a Project");
        final Project projectRel = (Project) relNode;
        final RexProgram program = RexProgram.create(
                projectRel.getInput(0).getRowType(), projectRel.getProjects(), null,
                projectRel.getRowType(), projectRel.getCluster().getRexBuilder());
        return new VoltLogicalCalc(
                projectRel.getCluster(), projectRel.getTraitSet().replace(VoltLogicalRel.CONVENTION),
                projectRel.getInput(0), program, isTopJoin);
    }
}

