/* This file is part of VoltDB.
 * Copyright (C) 2008-2014 VoltDB Inc.
 *
 * This file contains original code and/or modifications of original code.
 * Any modifications made by VoltDB Inc. are licensed under the following
 * terms and conditions:
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
/* Copyright (C) 2008 by H-Store Project
 * Brown University
 * Massachusetts Institute of Technology
 * Yale University
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
#ifndef HSTORETYPES_H
#define HSTORETYPES_H

#include <string>
#include <stdint.h>
namespace voltdb {

// forward declare
class NValue;

enum HashinatorType {
    HASHINATOR_LEGACY = 0
    , HASHINATOR_ELASTIC = 1
};

// ------------------------------------------------------------------
// Value Types
// This file defines all the types that we will support
// We do not allow for user-defined types, nor do we try to do anything dyanmic
//
// N.B. Oridnals MUST equal corresponding types in Java
// ------------------------------------------------------------------
enum ValueType {
    VALUE_TYPE_INVALID      = 0,    // a column should never be this type!
    VALUE_TYPE_NULL         = 1,    // and they should never be this either!
    VALUE_TYPE_FOR_DIAGNOSTICS_ONLY_NUMERIC = 2, // Java planner only. Not used in EE.

    //
    // Column Types
    //
    VALUE_TYPE_TINYINT      = 3,    // 1 byte int
    VALUE_TYPE_SMALLINT     = 4,    // 2 bytes int
    VALUE_TYPE_INTEGER      = 5,    // 4 bytes int
    VALUE_TYPE_BIGINT       = 6,    // 8 bytes int
    VALUE_TYPE_DOUBLE       = 8,    // 8 bytes floating, called FLOAT in java
    VALUE_TYPE_VARCHAR      = 9,    // variable length chars
    VALUE_TYPE_TIMESTAMP    = 11,   // 8 bytes int
    VALUE_TYPE_DECIMAL      = 22,   // decimal(p,s)
    VALUE_TYPE_BOOLEAN      = 23,   // only used in the EE
    VALUE_TYPE_ADDRESS      = 24,   // generated by certain expression code in the EE
    VALUE_TYPE_VARBINARY    = 25,   // variable length bytes
    VALUE_TYPE_ARRAY       = -99,   // an array of elements whose type is indicated elsewhere.
};

// ------------------------------------------------------------------
// Execution Result Codes
// ------------------------------------------------------------------
enum ResultType {
    RESULT_NOOP         = 1,
    RESULT_OK           = 2,
    RESULT_ABORTED      = 3,
    RESULT_ERROR        = 4,
    RESULT_UNKNOWN      = 5
};

// ------------------------------------------------------------------
// Payload Message Type
// ------------------------------------------------------------------
enum PayloadType {
    PAYLOAD_TYPE_INVALID            = 0,
    PAYLOAD_TYPE_CLIENT_REQUEST     = 1,
    PAYLOAD_TYPE_CLIENT_RESPONSE    = 2,
    PAYLOAD_TYPE_EXECUTE_REQUEST    = 3,
    PAYLOAD_TYPE_EXECUTE_RESPONSE   = 4,
};

// ------------------------------------------------------------------
// Query Type
// ------------------------------------------------------------------
enum QueryType {
    QUERY_TYPE_INVALID      = 0, // used for parsing
    QUERY_TYPE_NOOP         = 1,
    QUERY_TYPE_SELECT       = 2,
    QUERY_TYPE_INSERT       = 3,
    QUERY_TYPE_UPDATE       = 4,
    QUERY_TYPE_DELETE       = 5
};

// ------------------------------------------------------------------
// Join Type
// ------------------------------------------------------------------
enum JoinType {
    JOIN_TYPE_INVALID       = 0,
    JOIN_TYPE_INNER         = 1,
    JOIN_TYPE_LEFT          = 2,
    JOIN_TYPE_FULL          = 3,
    JOIN_TYPE_RIGHT         = 4,
};

// ------------------------------------------------------------------
// Constraint Type
// ------------------------------------------------------------------
enum ConstraintType {
    CONSTRAINT_TYPE_FOREIGN_KEY    = 0,
    CONSTRAINT_TYPE_MAIN           = 1,
    CONSTRAINT_TYPE_UNIQUE         = 2,
    CONSTRAINT_TYPE_CHECK          = 3,
    CONSTRAINT_TYPE_PRIMARY_KEY    = 4,
    CONSTRAINT_TYPE_NOT_NULL       = 5,
    CONSTRAINT_TYPE_PARTITIONING   = 6,
};

// ------------------------------------------------------------------
// Sort Direction Type
// ------------------------------------------------------------------
enum SortDirectionType {
    SORT_DIRECTION_TYPE_INVALID = 0,
    SORT_DIRECTION_TYPE_ASC     = 1,
    SORT_DIRECTION_TYPE_DESC    = 2,
};

// ------------------------------------------------------------------
// Union Type
// ------------------------------------------------------------------
enum UnionType {
    UNION_TYPE_NOUNION          = 0,
    UNION_TYPE_UNION            = 1,
    UNION_TYPE_UNION_ALL        = 2,
    UNION_TYPE_INTERSECT        = 3,
    UNION_TYPE_INTERSECT_ALL    = 4,
    UNION_TYPE_EXCEPT_ALL       = 5,
    UNION_TYPE_EXCEPT           = 6
};

// ------------------------------------------------------------------
// PlanNode Type
// ------------------------------------------------------------------
enum PlanNodeType {
    PLAN_NODE_TYPE_INVALID          = 0, // for parsing...

    //
    // Scan Nodes
    //
    PLAN_NODE_TYPE_SEQSCAN          = 10,
    PLAN_NODE_TYPE_INDEXSCAN        = 11,
    // special counting index
    PLAN_NODE_TYPE_INDEXCOUNT       = 12,
    PLAN_NODE_TYPE_TABLECOUNT       = 13,
    // for sql-in using indexes
    PLAN_NODE_TYPE_MATERIALIZEDSCAN = 14,
    //
    // Join Nodes
    //
    PLAN_NODE_TYPE_NESTLOOP         = 20,
    PLAN_NODE_TYPE_NESTLOOPINDEX    = 21,

    //
    // Operator Nodes
    //
    PLAN_NODE_TYPE_UPDATE           = 30,
    PLAN_NODE_TYPE_INSERT           = 31,
    PLAN_NODE_TYPE_DELETE           = 32,
    PLAN_NODE_TYPE_UPSERT           = 33,

    //
    // Communication Nodes
    //
    PLAN_NODE_TYPE_SEND             = 40,
    PLAN_NODE_TYPE_RECEIVE          = 41,

    //
    // Misc Nodes
    //
    PLAN_NODE_TYPE_AGGREGATE        = 50,
    PLAN_NODE_TYPE_HASHAGGREGATE    = 51,
    PLAN_NODE_TYPE_UNION            = 52,
    PLAN_NODE_TYPE_ORDERBY          = 53,
    PLAN_NODE_TYPE_PROJECTION       = 54,
    PLAN_NODE_TYPE_MATERIALIZE      = 55,
    PLAN_NODE_TYPE_LIMIT            = 56,
    PLAN_NODE_TYPE_DISTINCT         = 57,
    PLAN_NODE_TYPE_PARTIALAGGREGATE = 58
};

// ------------------------------------------------------------------
//  Resource Access Mode Type for ReadWrite set
// ------------------------------------------------------------------
enum ReadWriteType {
    READWRITE_TYPE_NONE     = 0,
    READWRITE_TYPE_S        = 1,
    READWRITE_TYPE_X        = 2,
    READWRITE_TYPE_IS       = 3,
    READWRITE_TYPE_IX       = 4,
    READWRITE_TYPE_SIX      = 5
    // mode U/IU cannot exist as the read/write set is checked after execution
    // TODO : range ReadWrite types? if we need them, like SQLServer or like DB2?
};

// ------------------------------------------------------------------
// Undo Log Entry Types
// ------------------------------------------------------------------
enum UndoLogType {
   UNDOLOG_NOOP                 = 0,
   UNDOLOG_INSERT               = 1,
   UNDOLOG_UPDATE               = 2,
   UNDOLOG_DELETE               = 3,
   UNDOLOG_CHECKCONSISTENCY     = 4 //check consistency
};

// ------------------------------------------------------------------
// Predicate Expression Operation Types
// ------------------------------------------------------------------
enum ExpressionType {
    EXPRESSION_TYPE_INVALID                     = 0,

    // -----------------------------
    // Arithmetic Operators
    // Implicit Numeric Casting: Trying to implement SQL-92.
    // Implicit Character Casting: Trying to implement SQL-92, but not easy...
    // Anyway, use explicit EXPRESSION_TYPE_OPERATOR_CAST if you could.
    // -----------------------------
    EXPRESSION_TYPE_OPERATOR_PLUS                   = 1, // left + right (both must be number. implicitly casted)
    EXPRESSION_TYPE_OPERATOR_MINUS                  = 2, // left - right (both must be number. implicitly casted)
    EXPRESSION_TYPE_OPERATOR_MULTIPLY               = 3, // left * right (both must be number. implicitly casted)
    EXPRESSION_TYPE_OPERATOR_DIVIDE                 = 4, // left / right (both must be number. implicitly casted)
    EXPRESSION_TYPE_OPERATOR_CONCAT                 = 5, // left || right (both must be char/varchar)
    EXPRESSION_TYPE_OPERATOR_MOD                    = 6, // left % right (both must be integer)
    EXPRESSION_TYPE_OPERATOR_CAST                   = 7, // explicitly cast left as right (right is integer in ValueType enum)
    EXPRESSION_TYPE_OPERATOR_NOT                    = 8, // logical not operator
    EXPRESSION_TYPE_OPERATOR_IS_NULL                = 9, // is null test.

    // -----------------------------
    // Comparison Operators
    // -----------------------------
    EXPRESSION_TYPE_COMPARE_EQUAL                   = 10, // equal operator between left and right
    EXPRESSION_TYPE_COMPARE_NOTEQUAL                = 11, // inequal operator between left and right
    EXPRESSION_TYPE_COMPARE_LESSTHAN                = 12, // less than operator between left and right
    EXPRESSION_TYPE_COMPARE_GREATERTHAN             = 13, // greater than operator between left and right
    EXPRESSION_TYPE_COMPARE_LESSTHANOREQUALTO       = 14, // less than equal operator between left and right
    EXPRESSION_TYPE_COMPARE_GREATERTHANOREQUALTO    = 15, // greater than equal operator between left and right
    EXPRESSION_TYPE_COMPARE_LIKE                    = 16, // LIKE operator (left LIKE right). both children must be string.
    EXPRESSION_TYPE_COMPARE_IN                      = 17, // IN operator [left IN (right1, right2, ...)]

    // -----------------------------
    // Conjunction Operators
    // -----------------------------
    EXPRESSION_TYPE_CONJUNCTION_AND                 = 20,
    EXPRESSION_TYPE_CONJUNCTION_OR                  = 21,

    // -----------------------------
    // Values
    // -----------------------------
    EXPRESSION_TYPE_VALUE_CONSTANT                  = 30,
    EXPRESSION_TYPE_VALUE_PARAMETER                 = 31,
    EXPRESSION_TYPE_VALUE_TUPLE                     = 32,
    EXPRESSION_TYPE_VALUE_TUPLE_ADDRESS             = 33,
    EXPRESSION_TYPE_VALUE_NULL                      = 34,
    EXPRESSION_TYPE_VALUE_VECTOR                    = 35,

    // -----------------------------
    // Aggregates
    // -----------------------------
    EXPRESSION_TYPE_AGGREGATE_COUNT                 = 40,
    EXPRESSION_TYPE_AGGREGATE_COUNT_STAR            = 41,
    EXPRESSION_TYPE_AGGREGATE_SUM                   = 42,
    EXPRESSION_TYPE_AGGREGATE_MIN                   = 43,
    EXPRESSION_TYPE_AGGREGATE_MAX                   = 44,
    EXPRESSION_TYPE_AGGREGATE_AVG                   = 45,

    // -----------------------------
    // Functions
    // -----------------------------
    EXPRESSION_TYPE_FUNCTION                        = 100,


    // -----------------------------
    // Internals added for Elastic
    // -----------------------------
    EXPRESSION_TYPE_HASH_RANGE                      = 200,

    // -----------------------------
    // Internals added for Case When
    // -----------------------------
    EXPRESSION_TYPE_OPERATOR_CASE_WHEN                       = 300,
    EXPRESSION_TYPE_OPERATOR_ALTERNATIVE                     = 301,

};

// ------------------------------------------------------------------
// Table Index Types
// ------------------------------------------------------------------
enum TableIndexType {
    BALANCED_TREE_INDEX     = 1,
    HASH_TABLE_INDEX        = 2,
};

// ------------------------------------------------------------------
// Index Lookup Types
// ------------------------------------------------------------------
enum IndexLookupType {
   INDEX_LOOKUP_TYPE_INVALID = 0,
   INDEX_LOOKUP_TYPE_EQ      = 1,
   INDEX_LOOKUP_TYPE_GT      = 2,
   INDEX_LOOKUP_TYPE_GTE     = 3,
   INDEX_LOOKUP_TYPE_LT      = 4,
   INDEX_LOOKUP_TYPE_LTE     = 5,
   INDEX_LOOKUP_TYPE_GT_LT   = 6,
   INDEX_LOOKUP_TYPE_GTE_LT  = 7,
   INDEX_LOOKUP_TYPE_GTL_TE  = 8,
   INDEX_LOOKUP_TYPE_GTE_LTE = 9
};

// ------------------------------------------------------------------
// Table Stream Types
//
// IMPORTANT: Keep this enum in sync with the Java equivalent
//            in TableStreamType.java!
//
// Use the functions in preference to switch/case constructs on enum
// values. This minimizes the spread of stream type assumptions and
// makes it less painful to change the enum.
// ------------------------------------------------------------------
enum TableStreamType {
    // Table stream types that use predicates.
    TABLE_STREAM_SNAPSHOT,
    TABLE_STREAM_ELASTIC_INDEX,

    // Materialize previously-captured index.
    TABLE_STREAM_ELASTIC_INDEX_READ,

    // Clear previously-captured index using the same range predicate that
    // was used for TABLE_STREAM_ELASTIC_INDEX_READ.
    TABLE_STREAM_ELASTIC_INDEX_CLEAR,

    // Table stream types that don't use predicates.
    // Add new non-predicate types below TABLE_STREAM_RECOVERY so
    // that tableStreamTypeHasPredicates() doesn't have to change.
    TABLE_STREAM_RECOVERY,

    // Table stream type provided when no stream is active.
    TABLE_STREAM_NONE = -1
};

// Serialization special values returned by serializeMore(), etc. instead
// of the normal count. There's only one possible value for now.
enum TableStreamSerializationError {
    TABLE_STREAM_SERIALIZATION_ERROR = -1
};

/**
 * Return true if the table stream type uses predicates.
 */
inline bool tableStreamTypeHasPredicates(TableStreamType streamType) {
    return streamType == TABLE_STREAM_SNAPSHOT
        || streamType == TABLE_STREAM_ELASTIC_INDEX
        || streamType == TABLE_STREAM_ELASTIC_INDEX_READ;
}

/**
 * Return true if the table stream type is performing a snapshot.
 */
inline bool tableStreamTypeIsSnapshot(TableStreamType streamType) {
    return streamType == TABLE_STREAM_SNAPSHOT;
}

/**
 * Return true if the table stream type is for recovery.
 */
inline bool tableStreamTypeIsRecovery(TableStreamType streamType) {
    return streamType == TABLE_STREAM_RECOVERY;
}

/**
 * Return true if the table stream type valid.
 */
inline bool tableStreamTypeIsValid(TableStreamType streamType) {
    return streamType != TABLE_STREAM_NONE;
}

inline bool tableStreamTypeAppliesToPreTruncateTable(TableStreamType streamType) {
    return streamType == TABLE_STREAM_ELASTIC_INDEX;
}

// ------------------------------------------------------------------
// Statistics Selector Types
// ------------------------------------------------------------------
enum StatisticsSelectorType {
    STATISTICS_SELECTOR_TYPE_TABLE,
    STATISTICS_SELECTOR_TYPE_INDEX
};

// ------------------------------------------------------------------
// Recovery protocol message types
// ------------------------------------------------------------------
enum RecoveryMsgType {
    /*
     * Message containing freshly scanned tuples to be inserted
     */
    RECOVERY_MSG_TYPE_SCAN_TUPLES = 0,
    /*
     * Message indicating that the table scan is complete, future polling
     * will produce delta data
     */
    RECOVERY_MSG_TYPE_SCAN_COMPLETE = 1,
    /*
     * Message containing whole tuples that are either updates or inserts
     */
    RECOVERY_MSG_TYPE_DELTA_MERGE_TUPLES = 2,
    /*
     * Message containing primary keys that must be deleted
     */
    RECOVERY_MSG_TYPE_DELTA_DELETE_PKEYS = 3,
    /*
     * Generated when all recovery data for a table has been generated
     */
    RECOVERY_MSG_TYPE_COMPLETE = 4
};

// ------------------------------------------------------------------
// Types of generic tasks that can be submitted to the EE
// ------------------------------------------------------------------
enum TaskType {
    TASK_TYPE_VALIDATE_PARTITIONING = 0,
    TASK_TYPE_APPLY_BINARY_LOG = 1
};


// ------------------------------------------------------------------
// Types of DR records
// ------------------------------------------------------------------
enum DRRecordType {
    DR_RECORD_INSERT = 0,
    DR_RECORD_DELETE = 1,
    DR_RECORD_UPDATE = 2,
    DR_RECORD_BEGIN_TXN = 3,
    DR_RECORD_END_TXN = 4,
    DR_RECORD_TRUNCATE_TABLE = 5
};

// ------------------------------------------------------------------
// Tuple serialization formats
// ------------------------------------------------------------------
enum TupleSerializationFormat { TUPLE_SERIALIZATION_NATIVE = 0, TUPLE_SERIALIZATION_DR = 1 };

// ------------------------------------------------------------------
// Endianess
// ------------------------------------------------------------------
enum Endianess { BYTE_ORDER_BIG_ENDIAN = 0, BYTE_ORDER_LITTLE_ENDIAN = 1 };


// ------------------------------------------------------------------
// Utility functions.
// -----------------------------------------------------------------
std::string getTypeName(ValueType type);
std::string tableStreamTypeToString(TableStreamType type);

bool isNumeric(ValueType type);
bool isIntegralType(ValueType type);

// for testing, obtain a random instance of the specified type
NValue getRandomValue(ValueType type);

std::string valueToString(ValueType type);
ValueType stringToValue(std::string str );

std::string joinToString(JoinType type);
JoinType stringToJoin(std::string str );

std::string sortDirectionToString(SortDirectionType type);
SortDirectionType stringToSortDirection(std::string str );

std::string planNodeToString(PlanNodeType type);
PlanNodeType stringToPlanNode(std::string str );

std::string expressionToString(ExpressionType type);
ExpressionType stringToExpression(std::string str );

std::string indexLookupToString(IndexLookupType type);
IndexLookupType stringToIndexLookup(std::string str );

int64_t getMaxTypeValue (ValueType type);

bool hexDecodeToBinary(unsigned char *bufferdst, const char *hexString);
}

#endif
