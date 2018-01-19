package edu.mit.bcs.clevros;

import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.composites.Pair;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Verify that the provided LogicalExpression is an instance of a filtering invocation,
 * and retrieve the relevant filtering attribute and value.
 */
public class GetFilterArguments implements ILogicalExpressionVisitor {

    private static final Type ET_TYPE;
    private static final Set<Type> PROPERTY_TYPES = new HashSet<>();
    private static final Set<Type> FILTER_TYPES = new HashSet<>();
    static {
        TypeRepository repo = LogicLanguageServices.getTypeRepository();

        ET_TYPE = repo.getType("<e,t>");

        PROPERTY_TYPES.add(repo.getType("psh"));
        PROPERTY_TYPES.add(repo.getType("psi"));
        PROPERTY_TYPES.add(repo.getType("pc"));
        PROPERTY_TYPES.add(repo.getType("pm"));

        for (String attribute : Arrays.asList("shape", "size", "color", "material")) {
            String abbr = (attribute.charAt(0) == 's') ? attribute.substring(0, 2) : attribute.substring(0, 1);
            FILTER_TYPES.add(repo.getType(String.format("<<e,t>,<p%s,<e,t>>>", abbr)));
        }
    }

    private int state = 0;
    private String filterAttribute = null;
    private String filterValue = null;

    public static Pair<String, String> of(LogicalExpression exp) {
        final GetFilterArguments visitor = new GetFilterArguments();
        visitor.visit(exp);
        return visitor.getResult();
    }

    private Pair<String, String> getResult() {
        return filterAttribute == null ? null : Pair.of(filterAttribute, filterValue);
    }

    private void abort() {
        filterAttribute = null;
        state = -1;
    }

    @Override
    public void visit(Lambda lambda) {
        TypeRepository repo = LogicLanguageServices.getTypeRepository();

        if (state == 0) {
            if (!lambda.getArgument().getType().equals(ET_TYPE)) {
                abort();
                return;
            }

            state = 1;
            lambda.getBody().accept(this);
        } else if (state == 1) {
            if (!lambda.getArgument().getType().equals(repo.getEntityType())) {
                abort();
                return;
            }

            state = 2;
            lambda.getBody().accept(this);
        } else {
            abort();
        }
    }

    @Override
    public void visit(Literal literal) {
        if (state == 2) {
            LogicalExpression pred = literal.getPredicate();
            if (!(pred instanceof LogicalConstant)) {
                abort();
                return;
            }

            LogicalConstant predConst = (LogicalConstant) pred;
            if (!predConst.getBaseName().startsWith("filter_") || !FILTER_TYPES.contains(predConst.getType())) {
                abort();
                return;
            }

            if (!(literal.getArg(0) instanceof Lambda) || !literal.getArg(0).getType().equals(ET_TYPE)) {
                abort();
                return;
            }

            if (!PROPERTY_TYPES.contains(literal.getArg(1).getType())) {
                abort();
                return;
            }

            state = 3;

            String filterName = predConst.getBaseName();
            filterAttribute = filterName.substring(filterName.indexOf("_") + 1);
            filterValue = ((LogicalConstant) literal.getArg(1)).getBaseName();
        } else {
            abort();
        }
    }

    @Override
    public void visit(LogicalConstant logicalConstant) {
        return;
    }

    @Override
    public void visit(Variable variable) {
        return;
    }

}
