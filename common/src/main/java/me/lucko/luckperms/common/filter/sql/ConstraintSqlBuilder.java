package me.lucko.luckperms.common.filter.sql;

import me.lucko.luckperms.common.filter.Comparison;
import me.lucko.luckperms.common.filter.Constraint;
import me.lucko.luckperms.common.filter.PageParameters;
import me.lucko.luckperms.common.storage.implementation.sql.builder.AbstractSqlBuilder;

public class ConstraintSqlBuilder extends AbstractSqlBuilder {

    public void visitConstraintValue(Object value) {
        if (value instanceof String) {
            this.builder.variable(((String) value));
        } else {
            throw new IllegalArgumentException("Don't know how to write value with type: " + value.getClass().getName());
        }
    }

    public void visit(Constraint<?> constraint) {
        //        '= value'
        //       '!= value'
        //     'LIKE value'
        // 'NOT LIKE value'

        visit(constraint.comparison());
        this.builder.append(' ');
        visitConstraintValue(constraint.value());
    }

    public void visit(Comparison comparison) {
        switch (comparison) {
            case EQUAL:
                this.builder.append("=");
                break;
            case NOT_EQUAL:
                this.builder.append("!=");
                break;
            case SIMILAR:
                this.builder.append("LIKE");
                break;
            case NOT_SIMILAR:
                this.builder.append("NOT LIKE");
                break;
            default:
                throw new AssertionError(comparison);
        }
    }

    public void visit(PageParameters pageParameters) {
        int pageSize = pageParameters.pageSize();
        int pageNumber = pageParameters.pageNumber();
        this.builder.append(" LIMIT " + pageSize + " OFFSET " + (pageNumber - 1) * pageSize);
    }

}
